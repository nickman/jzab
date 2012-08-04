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
package org.helios.jzab.proxy;

import org.helios.jzab.agent.net.codecs.ZabbixRequestEncoder;
import org.helios.jzab.agent.net.passive.AgentListener;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.w3c.dom.Node;

/**
 * <p>Title: ProxyListener</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.proxy.ProxyListener</code></p>
 */

public class ProxyListener extends AgentListener {

	/** The Zabbix protocol token */
	protected static final ChannelBuffer[] ZBX_DELIM = new ChannelBuffer[]{ChannelBuffers.wrappedBuffer(new byte[]{'Z', 'B', 'X', 'D', '1'})}; 
	
	/**
	 * Creates a new ProxyListener
	 * @param configNode The configuration node
	 */
	public ProxyListener(Node configNode) {
		super(configNode);
	}
	
	/** The configuration node name */
	public static final String NODE = "proxy-listener";
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.passive.AgentListener#getNodeName()
	 */
	protected String getNodeName() {
		return "proxy-listener";
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.passive.AgentListener#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		//pipeline.addLast("logger1", loggingHandler);
		pipeline.addLast("zabbixSplitter", new ZabbixSplitter());
		
//		pipeline.addLast("zabbixDecoder", new ZabbixProxyRequestDecoder());
//		pipeline.addLast("zabbixEncoder", new ZabbixRequestEncoder((byte)1));
//		pipeline.addLast("zabbixProxyHandler", new ZabbixProxyHandler());						
		return pipeline;
	}
	

}
