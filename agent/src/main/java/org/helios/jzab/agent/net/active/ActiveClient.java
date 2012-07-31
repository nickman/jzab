/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.jzab.agent.net.active;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.agent.net.SharableHandlers;
import org.helios.jzab.agent.net.codecs.ZabbixResponseDecoder;
import org.helios.jzab.agent.net.routing.JSONResponseHandler;
import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.XMLHelper;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: ActiveClient</p>
 * <p>Description: Client to implement a zabbix Active Agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.ActiveClient</code></p>
 */

public class ActiveClient extends NotificationBroadcasterSupport implements ChannelPipelineFactory, ActiveClientMXBean  {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The name of this active agent  */
	protected final String agentName;
	/** This agent's object name */
	protected final ObjectName objectName;
	
	/** The netty server boss pool */
	protected final Executor bossPool;
	/** The netty server worker pool */
	protected final Executor workerPool;
	/** The netty bootstrap */
	protected final ClientBootstrap bstrap;
	/** The netty channel factory */
	protected final ChannelFactory channelFactory;
	/** The socket options for the listener and child sockets */
	protected final Map<String, Object> socketOptions = new HashMap<String, Object>();
	/** Up/Down flag */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** A channel group containing all open channels created by the agent */
	protected final ChannelGroup channelGroup;
	/** JMX notification serial number factory */
	protected final AtomicLong notificationSequence = new AtomicLong(0);
	/** The sharable handlers repository */
	protected final SharableHandlers sharableHandlers = SharableHandlers.getInstance();
	
	/** The singleton ActiveClient instance */
	private static volatile ActiveClient instance = null;
	/** The singleton ActiveClient instance ctor lock */
	private static final Object lock = new Object();
	
	/** The ActiveAgent  JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.agent.client:service=ActiveClient");
	
	/** The netty logging handler for debugging the netty stack */
	protected final LoggingHandler loggingHandler;
	
	/** The configuration node name */
	public static final String NODE = "active-client";

	/** The config type name for the boss pool type */
	public static final String BOSS_POOL_TYPE = "boss-pool";
	/** The config type name for the worker pool type */
	public static final String WORKER_POOL_TYPE = "worker-pool";
	
	/** The channel connection timeout in ms. that is used on connection requests if no timeout socket option has been specified */
	public static final int DEFAULT_CONNECT_TIMEOUT = 1000;
	
	/**
	 * Returns the ActiveClient singleton instance
	 * @return the ActiveClient singleton instance
	 */
	public static ActiveClient getInstance() {
		if(instance==null) {
				throw new IllegalStateException("The active client has not been initialized", new Throwable());
		}
		return instance;
	}
	
	/**
	 * Configures and returns the ActiveClient singleton instance
	 * @param configNode The configuration node
	 * @return the ActiveClient singleton instance
	 */
	public static ActiveClient getInstance(Node configNode) {
		if(instance==null) {
				synchronized(lock) {
					if(instance==null) {
						instance = new ActiveClient(configNode);
					}
				}
		}
		return instance;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.ActiveClientMXBean#getChannelCount()
	 */
	@Override
	public int getChannelCount() {
		return channelGroup.size();
	}
	
	/**
	 * Creates a new ActiveClient
	 * @param configNode The configuration node
	 */
	private ActiveClient(Node configNode) {
		super(ThreadPoolFactory.getInstance("NotificationProcessor"));
		if(configNode==null) throw new IllegalArgumentException("The passed configuration node was null", new Throwable());
		String nodeName = configNode.getNodeName(); 
		if(!NODE.equals(nodeName)) {
			throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
		}
		agentName = XMLHelper.getAttributeByName(configNode, "name", "ActiveClient@" + System.identityHashCode(this));
		loggingHandler = new LoggingHandler(agentName, InternalLogLevel.DEBUG, true);
		objectName = JMXHelper.objectName("org.helios.jzab.agent.net", "service", "ActiveClient", "name", agentName);
		bossPool = ThreadPoolFactory.getInstance(XMLHelper.getAttributeByName(XMLHelper.getChildNodeByName(configNode, BOSS_POOL_TYPE, false), "name", null));
		workerPool = ThreadPoolFactory.getInstance(XMLHelper.getAttributeByName(XMLHelper.getChildNodeByName(configNode, WORKER_POOL_TYPE, false), "name", null));
		Node socketOpts = XMLHelper.getChildNodeByName(configNode, "socket-options", false);
		if(socketOpts!=null) {
			for(Node socketOption: XMLHelper.getChildNodesByName(socketOpts, "opt", false)) {
				String valueStr = XMLHelper.getAttributeByName(socketOption, "value", "-1").trim().toLowerCase();				
				Object value = null;
				if(valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("false")) {
					value = valueStr.equalsIgnoreCase("true");
				} else {
					try { value = Integer.parseInt(valueStr); } catch (Exception e) {
						value = valueStr;
					}
				}
				socketOptions.put(XMLHelper.getAttributeByName(socketOption, "name", null), value);
			}
		}
		channelGroup = new DefaultChannelGroup(agentName + "-ChannelGroup");
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		
		bstrap = new ClientBootstrap(channelFactory);
		bstrap.setPipelineFactory(this);
		bstrap.setOptions(socketOptions);
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), OBJECT_NAME, this);
		log.info("Created ActiveAgent [{}]", agentName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("logger", loggingHandler);
		if(log.isTraceEnabled()) {
			pipeline.addLast("logger", loggingHandler);
		}
		pipeline.addLast("routingHandler1", sharableHandlers.getHandler("responseRoutingHandler"));
		pipeline.addLast("responseEncoder", sharableHandlers.getHandler("responseEncoder"));		
		pipeline.addLast("responseDecoder", new ZabbixResponseDecoder());
		pipeline.addLast("routingHandler2", sharableHandlers.getHandler("responseRoutingHandler"));
		pipeline.addLast("channelCloser", sharableHandlers.getHandler("channelCloser"));
		return pipeline;
	}
	
	
	/**
	 * Returns the logging level for this active client
	 * @return the logging level for this active client
	 */
	@Override
	public String getLevel() {
		return LoggerManager.getInstance().getLoggerLevelManager().getLoggerLevel(getClass().getName());
	}
	
	/**
	 * Sets the logger level for this active client
	 * @param level The level to set this logger to
	 */
	@Override
	public void setLevel(String level) {
		LoggerManager.getInstance().getLoggerLevelManager().setLoggerLevel(getClass().getName(), level);
	}
	
	
	/**
	 * Acquires a new channel to the passed socket asyncrhonously.
	 * The passed future listener should implement the action to be executed when the channel is acquired
	 * and an error handler in the event that the connection fails.
	 * @param host The host name or ip address to connect to
	 * @param port The listening port
	 * @param futureListener The callback executed when the connection is acquired or failed
	 * @return The ChannelFuture for this connection request
	 */
	public ChannelFuture newChannel(String host, int port, ChannelFutureListener futureListener) {
		if(host==null) throw new IllegalArgumentException("The passed host was null", new Throwable());
		if(futureListener==null) throw new IllegalArgumentException("The passed callback listener was null", new Throwable());
		SocketAddress sa = new InetSocketAddress(host, port);
		ChannelFuture cf = bstrap.connect(sa);
		cf.addListener(futureListener);
		return cf;
	}
	
	/**
	 * Issues a request/response operation against the specified server
	 * @param host The host of the target server
	 * @param port The port of the target server
	 * @param request The request object
	 * @param responseHandler The JSON response handlr
	 * @param timeout The operation timeout
	 * @param unit The timeout unit
	 */
	public void newReqRespChannel(String host, int port, Object request, JSONResponseHandler responseHandler, long timeout, TimeUnit unit) {
		if(host==null) throw new IllegalArgumentException("The passed host was null", new Throwable());
		if(responseHandler==null) throw new IllegalArgumentException("The passed response handler was null", new Throwable());
		SocketAddress sa = new InetSocketAddress(host, port);
		bstrap.connect(sa).addListener(wrapHandler(request, responseHandler, timeout, unit));
	}
	
	
	/**
	 * Executes a synchronous request response operation against the passed server
	 * @param request The request object
	 * @param responseType The expected response type
	 * @param server The active server to issue the request to
	 * @param timeout The operation timeout
	 * @param unit The operation timeout unit
	 * @return the returned result
	 */
	public <T> T requestResponse(final Object request, Class<T> responseType, ActiveServer server, long timeout, TimeUnit unit) {
		final long startTime = SystemClock.currentTimeMillis();
		ChannelFuture cf = bstrap.connect(server.getSocketAddress());
		if(!cf.awaitUninterruptibly(timeout, unit)) {
			log.error("Connection to [{}] timed out", server);
			throw new RuntimeException("Connection to [" + server + "] timed out", new Throwable());
		}
		if(!cf.isSuccess()) {
			log.error("Failure Connecting to [{}] timed out: [{}]", server, cf.getCause());
			throw new RuntimeException("Failure Connecting to [" + server + "]", new Throwable());			
		}
		final Channel channel = cf.getChannel();
		final AtomicReference<T> result = new AtomicReference<T>(null);
		final AtomicReference<Throwable> exception = new AtomicReference<Throwable>(null);
		long remaining = computeNextTimeout(TimeUnit.MILLISECONDS.convert(timeout, unit), startTime);
		log.debug("Connected to [{}]. Time remaining to complete [{}] ms.", server, remaining);
		if(!modfyRequestResponseChannel(channel, responseType, result, exception).write(request).awaitUninterruptibly(remaining, TimeUnit.MILLISECONDS)) {
			log.error("Timed out waiting for response from [{}]", server);
			throw new RuntimeException("Timed out waiting for response from [" + server + "]", new Throwable());			
		}
		Throwable throwable = exception.get();
		if(throwable != null) {
			log.error("Failed to get response from [{}] : [{}]", server, throwable);
			throw new RuntimeException("Failed to get response from [" + server + "]", throwable);						
		}
		return result.get();		
	}
	
	/**
	 * Computes the remaining time to completion
	 * @param originalTimeout The original timeout specified
	 * @param startTime The start time of the root operation in ms.
	 * @return The remaining time before timeout
	 */
	protected long computeNextTimeout(long originalTimeout, long startTime) {
		return originalTimeout - (SystemClock.currentTimeMillis() - startTime);
	}
	
	/**
	 * Modifies the passed channel's pipeline to handle a request response
	 * @param channel The channel to modify the pipeline for
	 * @param result A reference container for the result
	 * @param exception A reference container for any thrown exception
	 * @return the modified channel
	 */
	protected <T> Channel modfyRequestResponseChannel(Channel channel, final Class<T> responseType, final AtomicReference<T> result, final AtomicReference<Throwable> exception) {
		channel.getPipeline().remove("routingHandler1");
		channel.getPipeline().remove("routingHandler2");
		channel.getPipeline().addAfter("responseDecoder", "requestResponseHandler", new SimpleChannelUpstreamHandler() {
			@Override
			public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
				final Object response = e.getMessage();
				try {
					result.set(responseType.cast(response));
				} catch (Exception ex) {
					exception.set(new Exception("Incompatible Result Type [" + response==null ? "<null>" :  response.getClass().getName() + "] but was expecting [" + responseType.getClass().getName() + "]", ex));
				}
				super.messageReceived(ctx, e);
			}
			
			@Override
			public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
				exception.set(e.getCause());
			}
		});		
		return channel;
	}
	
	
	/**
	 * Creates a request response channel future listener
	 * @param request The request to send
	 * @param responseHandler The JSON response handler
	 * @return the request response channel future listener
	 */
	protected ChannelFutureListener wrapHandler(final Object request, final JSONResponseHandler responseHandler, long timeout, TimeUnit unit) {
			final long startTime = SystemClock.currentTimeMillis();
			return new ChannelFutureListener() {
				// Handles the connect completion
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.getChannel().getPipeline().remove("routingHandler1");
					future.getChannel().getPipeline().remove("routingHandler2");
					future.getChannel().getPipeline().addAfter("responseDecoder", "requestResponseHandler", new SimpleChannelUpstreamHandler() {
						@Override
						public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
							responseHandler.jsonResponse(null, (JSONObject)e.getMessage());
							super.messageReceived(ctx, e);
						}					
					});
					if(future.isSuccess()) {
						future.getChannel().write(request).addListener(new ChannelFutureListener(){
							// Handles the request write
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								if(future.isSuccess()) {
									log.debug("Sent ReqResp Request [{}]", request);
								} else {
									log.error("Failed to write request [{}]:[{}]", request, future.getCause());
								}
							}
						});
					} else {
						log.error("Failed to connect [{}]", future.getCause());
					}
					
				}
			};
	}
	
	
	
	
	/**
	 * Acquires a new channel to the passed socket syncrhonously
	 * @param host The host name or ip address to connect to
	 * @param port The listening port
	 * @return A connected channel
	 */
	public Channel newChannel(String host, int port) {
		if(host==null) throw new IllegalArgumentException("The passed host was null", new Throwable());
		SocketAddress sa = new InetSocketAddress(host, port);
		Channel channel = null;
		ChannelFuture cf = null;
		if(socketOptions.containsKey("connectTimeoutMillis")) {
			cf = bstrap.connect(sa).awaitUninterruptibly();
		} else {
			cf = bstrap.connect(sa);
			if(!cf.awaitUninterruptibly(DEFAULT_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)) {				
				if(!cf.cancel()) {
					try { cf.getChannel().close(); } catch (Exception e) {}
				}
				// TIMEOUT THROW
			}
		}
		if(!cf.isDone()) {
			if(!cf.cancel()) {
				try { cf.getChannel().close(); } catch (Exception e) {}
			}	
			// TIMEOUT THROW
		} 
		// operation completed
		if(!cf.isSuccess()) {
			// FAILED THROW
		}
		// operation succeeded
		channel = cf.getChannel();		
		channelGroup.add(channel);
		return channel;
	}
	
	/**
	 * Acquires a new channel to the passed ActiveServer syncrhonously
	 * @param server The ActiveServer to connect to
	 * @return A connected channel
	 */
	public Channel newChannel(ActiveServer server) {
		if(server==null) throw new IllegalArgumentException("The passed server was null", new Throwable());
		return newChannel(server.getAddress(), server.getPort());
	}
	
	/**
	 * Acquires a new channel to the passed ActiveServer asyncrhonously.
	 * The passed future listener should implement the action to be executed when the channel is acquired
	 * and an error handler in the event that the connection fails.
	 * @param server The ActiveServer to connect to
	 * @param futureListener The callback executed when the connection is acquired or failed
	 * @return The ChannelFuture for this connection request
	 */
	public ChannelFuture newChannel(ActiveServer server, ChannelFutureListener futureListener) {	
		if(server==null) throw new IllegalArgumentException("The passed server was null", new Throwable());
		return newChannel(server.getAddress(), server.getPort(), futureListener);		
	}
	
	
	/**
	 * Returns a map representation of the installed socket options for this client
	 * @return a map representation of the installed socket options for this client
	 */
	@Override
	public Map<String, String> getSocketOptions() {
		Map<String, String> map = new HashMap<String, String>(socketOptions.size());
		for(Map.Entry<String, Object> opt: socketOptions.entrySet()) {
			map.put(opt.getKey(), opt.getValue().toString());
		}
		return map;
	}

	

}
