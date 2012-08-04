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
package org.helios.jzab.agent.api;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.util.CharsetUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ZClient</p>
 * <p>Description: The main API controller class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.api.ZClient</code></p>
 */

public class ZClient implements ChannelPipelineFactory, ChannelUpstreamHandler {
	// ===================================================================
	//    Session fields
	private String authKey = null;
	/** The Zabbix server host */
	private String host;
	/** The Zabbix server port */
	private int port;
	/** The Zabbix user name */
	private String user;
	/** The Zabbix user password */
	private String password;
	// ==================================================================
	//     Netty fields
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The netty server boss pool */
	protected final Executor bossPool = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "API Boss Thread");
			t.setDaemon(true);
			return t;
		}
	});
	/** The netty server worker pool */
	protected final Executor workerPool = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "API Worker Thread");
			t.setDaemon(true);
			return t;
		}
	});
	/** The netty bootstrap */
	protected final ClientBootstrap bstrap;
	/** The netty channel factory */
	protected final ChannelFactory channelFactory;
	/** The socket options for the listener and child sockets */
	protected final Map<String, Object> socketOptions = new HashMap<String, Object>();
	/** A channel group containing all open channels created by the agent */
	protected final ChannelGroup channelGroup;
	
	
	
	
	/** The netty logging handler for debugging the netty stack */
	//protected final LoggingHandler loggingHandler = new ZabbixLoggingHandler("ZAPI", InternalLogLevel.DEBUG, false);
	protected final LoggingHandler loggingHandler = new LoggingHandler(InternalLogLevel.INFO, true);
	
	
	/** The channel connection timeout in ms. that is used on connection requests if no timeout socket option has been specified */
	public static final int DEFAULT_CONNECT_TIMEOUT = 1000;
	
	/** The API uri */
	protected final URI apiUri;
	
	/** The sock addres to connect to */
	protected final InetSocketAddress sockAddress;
	/** The connected channel */
	protected final Channel channel;
	/** The request id serial */
	protected final AtomicLong reqSerial = new AtomicLong(0L);
	/** The format template for an authentication request */
	public static final String AUTH_REQ = "{\"jsonrpc\":\"2.0\",\"method\":\"user.authenticate\",\"params\":{\"user\":\"%s\",\"password\":\"%s\"},\"id\":%s}";

	/**
	 * Creates a new ZClient
	 * @param host The Zabbix server host
	 * @param port The Zabbix server port
	 * @param user The Zabbix user name
	 * @param password The Zabbix user password
	 */
	public ZClient(String host, int port, String user, String password) {		
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		sockAddress = new InetSocketAddress(host, port);
		try {
			apiUri = new URI(String.format("http://%s:%s/zabbix/api_jsonrpc.php", this.host, this.port));
		} catch (Exception e) {
			throw new RuntimeException("Failed to create JSON-RPC URI for [" + String.format("", this.host, this.port) + "]", e);
		}
		channelGroup = new DefaultChannelGroup("ZAPI-ChannelGroup");
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);		
		bstrap = new ClientBootstrap(channelFactory);
		bstrap.setPipelineFactory(this);
		bstrap.setOptions(socketOptions);
		ChannelFuture cf = bstrap.connect(sockAddress);
		cf.awaitUninterruptibly().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					channelGroup.add(future.getChannel());
				}
			}
		});
		channel = cf.getChannel();
		log.info("Created ZClient for [{}]:[{}]", host, port);
	}
	
	public void authenticate() {
		String resp = request(String.format(AUTH_REQ, user, password, reqSerial.incrementAndGet()));
		//log.info("Auth Response [{}]", resp);
	}
	
	private String request(String req) {
		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, apiUri.getRawPath());
		final ChannelBuffer content = ChannelBuffers.copiedBuffer(req, CharsetUtil.UTF_8);
//		request.setHeader(HttpHeaders.Names.HOST, host);
//		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
		//request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
		request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json");
	    request.setHeader(HttpHeaders.Names.ACCEPT, "application/json");
	    request.setHeader(HttpHeaders.Names.USER_AGENT, "jZab Agent");
		request.setContent(content);
	    request.setHeader(HttpHeaders.Names.HOST, host + ":" + port);
	    request.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
	    request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(content.readableBytes()));
	    
		log.info("Request [{}]",req);
	    channel.write(request).addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					log.info("Wrote Request");
				} else {
					log.info("Request Failed", future.getCause());
				}
				
			}
		});
	    
	    return null;
	    
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof ExceptionEvent) {
			ExceptionEvent ee = (ExceptionEvent)e;
			log.error("Request Fail Event", ee.getCause());
		}
		if(e instanceof MessageEvent) {
			MessageEvent me = (MessageEvent)e;
			Object obj = me.getMessage();
			if(obj instanceof HttpResponse) {
				HttpResponse response = (HttpResponse)obj;
				log.info("RESPONSE:\n[{}]", response.getContent().toString(CharsetUtil.UTF_8));
				JSONObject jsonObj = new JSONObject(response.getContent().toString(CharsetUtil.UTF_8));
				log.info("Response:\n[{}]", jsonObj.toString(2));
			}
		}
		ctx.sendUpstream(e);
		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline()  {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("logger", loggingHandler);
		if(log.isDebugEnabled()) {
			//pipeline.addLast("logger", loggingHandler);
		}
		pipeline.addLast("codec", new HttpClientCodec());
//		pipeline.addLast("stringd", new StringDecoder());
//		pipeline.addLast("stringe", new StringEncoder());
		pipeline.addLast("zclient", this);
		return pipeline;
	}
	


	/**
	 * Connects to the Zabbix server and issues a request
	 * @param args None specified
	 */
	public static void main(String[] args) {
		ZClient client = new ZClient("zabbix", 80, "admin", "zabbix");
		client.authenticate();
		try { Thread.sleep(5000); } catch (Exception e) {}
	}
	



	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ZClient [");
		if (authKey != null) {
			builder.append("authKey=");
			builder.append(authKey.replaceAll(".", "*"));
			builder.append(", ");
		}
		if (host != null) {
			builder.append("host=");
			builder.append(host);
			builder.append(", ");
		}
		builder.append("port=");
		builder.append(port);
		builder.append(", ");
		if (user != null) {
			builder.append("user=");
			builder.append(user);
			builder.append(", ");
		}
		if (password != null) {
			builder.append("password=");
			builder.append(password.replaceAll(".", "*"));
			builder.append(", ");
		}
		if (socketOptions != null) {
			builder.append("socketOptions=");
			builder.append(socketOptions);
		}
		builder.append("]");
		return builder.toString();
	}


}
