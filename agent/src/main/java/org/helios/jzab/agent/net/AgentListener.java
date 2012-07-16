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
package org.helios.jzab.agent.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.XMLHelper;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: AgentListener</p>
 * <p>Description: The socket listener that listens for server requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.AgentListener</code></p>
 */

public class AgentListener extends NotificationBroadcasterSupport implements ChannelPipelineFactory, AgentListenerMXBean {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The name of this agent listener */
	protected final String listenerName;
	/** The agent listener biding interface */
	protected final String bindingInterface;
	/** The agent listener port */
	protected final int bindingPort;
	/** This listener's object name */
	protected final ObjectName objectName;
	
	/** The netty server boss pool */
	protected final Executor bossPool;
	/** The netty server worker pool */
	protected final Executor workerPool;
	/** The netty bootstrap */
	protected final ServerBootstrap bstrap;
	/** The netty channel factory */
	protected final ChannelFactory channelFactory;
	/** The Inet socket that the server will listen on */
	protected final InetSocketAddress isock;
	/** The socket options for the listener and child sockets */
	protected final Map<String, Object> socketOptions = new HashMap<String, Object>();
	/** The listener channel */
	protected Channel listenerChannel;
	/** Up/Down flag */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** A channel group containing all open channels created by the listener */
	protected final ChannelGroup channelGroup;
	
	
	
	/** The configuration node name */
	public static final String NODE = "agent-listener";

	/** The default binding interface */
	public static final String DEFAULT_INTERFACE = "0.0.0.0";
	/** The default binding port */
	public static final int DEFAULT_PORT = 10050;
	/** The config type name for the boss pool type */
	public static final String BOSS_POOL_TYPE = "boss-pool";
	/** The config type name for the worker pool type */
	public static final String WORKER_POOL_TYPE = "worker-pool";
	
	
	/**
	 * Creates a new AgentListener
	 * @param configNode The configuration node
	 */
	public AgentListener(Node configNode) {
		super(ThreadPoolFactory.getInstance("NotificationProcessor"));
		if(configNode==null) throw new IllegalArgumentException("The passed configuration node was null", new Throwable());
		String nodeName = configNode.getNodeName(); 
		if(!NODE.equals(nodeName)) {
			throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
		}
		bindingPort = XMLHelper.getAttributeByName(configNode, "port", DEFAULT_PORT);
		bindingInterface = XMLHelper.getAttributeByName(configNode, "interface", DEFAULT_INTERFACE);		
		listenerName = XMLHelper.getAttributeByName(configNode, "name", "AgentListener@" + System.identityHashCode(this));
		objectName = JMXHelper.objectName("org.helios.jzab.agent.net", "service", "AgentListener", "name", listenerName);
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
		channelGroup = new DefaultChannelGroup(listenerName + "-ChannelGroup");
		isock = new InetSocketAddress(bindingInterface, bindingPort);
		channelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		
		bstrap = new ServerBootstrap(channelFactory);
		bstrap.setPipelineFactory(this);
		bstrap.setOptions(socketOptions);
		log.info("Created Agent Listener [{}] Ready to Listen on [{}]", listenerName, bindingInterface + ":" + bindingPort);
	}
	
	/**
	 * Starts this agent listener
	 * @throws Exception thrown if any error occurs starting the listener
	 */
	public void start() throws Exception {
		try {
			listenerChannel = bstrap.bind(isock);
			started.set(true);
			JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), objectName, this);
			log.info("Started Listener [{}] Ready to Listen on [{}]", listenerName, bindingInterface + ":" + bindingPort);
		} catch (Exception e) {
			throw new Exception("Failed to start Agent Listener [" + listenerName + "] on [" + bindingInterface + ":" + bindingPort + "]", e);
		}
	}
	
	/**
	 * Disconnects all the connected channels and stops the listening channel
	 */
	public void stop() {
		if(started.get()) {
			try {
				log.info("Closing [{}] Listener on [{}]", listenerName, isock);
				listenerChannel.close().addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						log.info("Stopped [{}] Listener on [{}]", listenerName, isock);
						log.info("Closing [{}] Connected Channels on [{}]", listenerName, isock);
						channelGroup.close().addListener(new ChannelGroupFutureListener() {
							@Override
							public void operationComplete(ChannelGroupFuture future) throws Exception {
								log.info("Closed [{}] Connected Channels on [{}]", listenerName, isock);
								started.set(false);
							}
						});
					}
				});
			} catch (Exception e) {
				log.warn("Exception stopping Agent Listener [{}]", listenerName, e);
			}
		}
	}
	
	/**
	 * Stops and releases all resources associated with this listener
	 */
	public void shutdown() {
		stop();
		channelFactory.releaseExternalResources();
		log.info("Release All Resources for [{}]", listenerName);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline(
			new SimpleChannelUpstreamHandler() {
				/**
				 * Invoked when a child Channel was open.
				 * @param ctx The channel handler context
				 * @param e The child channel state event
				 */
				@Override
				public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
					Channel channel = e.getChannel();
					channelGroup.add(channel);
					sendNotification(new Notification("child.channel.open", objectName, notificationSequence.incrementAndGet(), new StringBuilder("Channel Opened [")
						.append(" ChannelID:").append(channel.getId())
						.append(" Remote:").append(channel.getRemoteAddress())
						.append("]")
						.toString()
					));
					log.debug("Child channel created on [{}] from [{}]", channel.getLocalAddress(), channel.getRemoteAddress());
				}
				
				/**
				 * Invoked when a child Channel was closed.
				 * @param ctx The channel handler context
				 * @param e The child channel state event
				 */
				@Override
				public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
					Channel channel = e.getChannel();
					channelGroup.add(channel);
					sendNotification(new Notification("child.channel.closed", objectName, notificationSequence.incrementAndGet(), new StringBuilder("Channel Closed [")
						.append(" ChannelID:").append(channel.getId())
						.append(" Remote:").append(channel.getRemoteAddress())
						.append("]")
						.toString()
					));
					log.info("Child channel closed on [{}] from [{}]", channel.getLocalAddress(), channel.getRemoteAddress());
				}
				
				
			},
			new LoggingHandler(listenerName, InternalLogLevel.INFO, true),
			new SimpleChannelUpstreamHandler() {				
				@Override
				public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
					Object message = e.getMessage();
					if(message instanceof ChannelBuffer) {
						log.info("REQUEST: [{}]", new String(((ChannelBuffer)message).array()));
						ChannelBuffer cb = ChannelBuffers.dynamicBuffer(20);
						cb.writeBytes("ZBXD".getBytes());
						cb.writeByte((byte) 0x01);
						cb.writeBytes(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong("2".getBytes().length).array());
						cb.writeBytes("2".getBytes());
						e.getChannel().write(cb);
					} else {
						e.getChannel().write(e.getMessage()).addListener(ChannelFutureListener.CLOSE);
					}					
				};
			}
		);
		log.info("Connected Channel [{}]", pipeline.getChannel());
		return pipeline;
	}
	
	/** JMX notification serial number factory */
	protected final AtomicLong notificationSequence = new AtomicLong(0);
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.AgentListenerMXBean#getConnectionCount()
	 */
	@Override
	public int getConnectionCount() {
		return channelGroup.size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.AgentListenerMXBean#getListenerName()
	 */
	@Override
	public String getListenerName() {
		return listenerName;
	}

	/**
	 * Returns the registered socket options 
	 * @return the socketOptions
	 */
	public Map<String, Object> getSocketOptions() {
		return socketOptions;
	}
	
	/**
	 * Returns a map of the socket options with string values
	 * @return a map of the socket options with string values
	 */
	public Map<String, String> getSockOptions() {
		Map<String, String> map = new HashMap<String, String>(socketOptions.size());
		for(Map.Entry<String, Object> e: socketOptions.entrySet()) {
			map.put(e.getKey(), e.getValue().toString());
		}
		return map;
	}
	

	/**
	 * Indicates if the listener is started
	 * @return the started
	 */
	public AtomicBoolean getStarted() {
		return started;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.AgentListenerMXBean#getListenerPort()
	 */
	@Override
	public int getListenerPort() {
		if(isock!=null) {
			return isock.getPort();
		}
		return -1;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.AgentListenerMXBean#getListenerInterface()
	 */
	@Override
	public String getListenerInterface() {
		if(isock!=null) {
			return isock.getAddress().getHostAddress();
		}
		return null;
	}

}	