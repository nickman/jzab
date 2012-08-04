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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ZabbixProxyHandler</p>
 * <p>Description: Decodes zabbix proxy requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.proxy.ZabbixProxyHandler</code></p>
 */

public class ZabbixProxyHandler extends SimpleChannelHandler {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object obj = e.getMessage();
		if(obj instanceof JSONObject) {
			processZabbixRequest(ctx, (JSONObject)obj);
		}
		super.messageReceived(ctx, e);
	}
	
	
	void processZabbixRequest(ChannelHandlerContext ctx, JSONObject json) throws JSONException {
		log.info("Received Request [{}]", json.toString(2));
		if(json.has("response")) {
			String response = json.getString("response");
			if(!"success".equals(response)) {
				log.error("Processing response for failed request [{}]", json);
			} else {
				log.debug("Processing response for successful request [{}]", json);
			}
		} else if(json.has("request")) {
			String requestType = json.getString("request");
			Channel channel = ctx.getChannel();
//			if("host availability".equals(requestType)) {			
				ChannelBuffer response = ChannelBuffers.buffer(4);
				response.writeInt(1);
				ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), response, channel.getRemoteAddress()));
//			}			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		// TODO Auto-generated method stub
		log.error("Caught error [{}]", e.getCause().toString());
	}
}
