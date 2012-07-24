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
package org.helios.jzab.agent.net.codecs;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.json.JSONObject;

/**
 * <p>Title: ZabbixRequestEncoder</p>
 * <p>Description: Encodes the response to a passive check request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.passive.PassiveResponseEncoder</code></p>
 */
@Sharable
public class ZabbixRequestEncoder extends OneToOneEncoder {
	/** The protocol version of the zabbix passive response processor */
	protected final byte protocolVersion;

	
	
	/**
	 * Creates a new ZabbixRequestEncoder
	 * @param protocolVersion The zabbix protocol version for passive check responses.
	 */
	public ZabbixRequestEncoder(byte protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.oneone.OneToOneEncoder#encode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
	 */
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if(msg==null) return null;
		if(msg instanceof ChannelBuffer) {
			return msg;
		}
		if(msg instanceof JSONObject) {
			msg = msg.toString();
		}
		byte[] payload = null;
		if(msg instanceof CharSequence) {
			payload = msg.toString().getBytes();
		} else if(msg instanceof byte[]) {
			payload = (byte[])msg;
		} else if(msg instanceof FileRegion) {
			FileRegion fr = (FileRegion)msg;
			int size = (ZabbixConstants.BASELINE_SIZE);
			ChannelBuffer buffer = ChannelBuffers.buffer(size);
			buffer.writeBytes(ZabbixConstants.ZABBIX_HEADER);
			buffer.writeByte(protocolVersion);
			buffer.writeBytes(ZabbixConstants.encodeLittleEndianLongBytes(fr.getCount()));
			//ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), buffer, channel.getRemoteAddress()));
			
			//ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), fr, channel.getRemoteAddress()));
			return fr;
			
		} else  {
			throw new RuntimeException("Unrecognized payload type [" + msg.getClass().getName() + "]", new Throwable());
		}
		
		ChannelBuffer buffer = ChannelBuffers.buffer(ZabbixConstants.BASELINE_SIZE + payload.length);
		buffer.writeBytes(ZabbixConstants.ZABBIX_HEADER);
		buffer.writeByte(protocolVersion);
		buffer.writeBytes(ZabbixConstants.encodeLittleEndianLongBytes(payload.length));
		buffer.writeBytes(payload);		
		return buffer;
	}
	
	
	
}
