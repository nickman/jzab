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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.XMLHelper;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
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

public class ActiveClient extends NotificationBroadcasterSupport implements ChannelPipelineFactory {
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
	
	
	/** The netty logging handler for debugging the netty stack */
	protected final LoggingHandler loggingHandler;
	
	/** The configuration node name */
	public static final String NODE = "active-client";

	/** The config type name for the boss pool type */
	public static final String BOSS_POOL_TYPE = "boss-pool";
	/** The config type name for the worker pool type */
	public static final String WORKER_POOL_TYPE = "worker-pool";
	
	
	/**
	 * Creates a new ActiveClient
	 * @param configNode The configuration node
	 */
	public ActiveClient(Node configNode) {
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
		log.info("Created ActiveAgent [{}]", agentName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		//pipeline.addLast("logger", loggingHandler);
		pipeline.addLast("frameDecoder", new DelimiterBasedFrameDecoder(256, true, true, Delimiters.lineDelimiter()));
		pipeline.addLast("stringDecoder", sharableHandlers.getHandler("stringDecoder"));						
		pipeline.addLast("stringEncoder", sharableHandlers.getHandler("stringEncoder"));
		pipeline.addLast("passiveResponseEncoder", sharableHandlers.getHandler("responseEncoder"));
		
		
		return pipeline;
	}
	

	

}
