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
package org.helios.jzab.agent.net.passive;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * <p>Title: PassiveResponseEncoder</p>
 * <p>Description: Encodes the response to a passive check request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.passive.PassiveResponseEncoder</code></p>
 */
@Sharable
public class PassiveResponseEncoder extends OneToOneEncoder {
	/** The protocol version of the zabbix passive response processor */
	protected final byte protocolVersion;

	/** The zabbix response header in bytes */
	private static final byte[] ZABBIX_HEADER =  "ZBXD".getBytes();
	/** The zabbix response baseline size for creating the downstream channel buffer */
	public static final int BASELINE_SIZE = ZABBIX_HEADER.length + 9;  // one byte for protocol, 8 bytes for length
	
	
	/**
	 * Creates a new PassiveResponseEncoder
	 * @param protocolVersion The zabbix protocol version for passive check responses.
	 */
	public PassiveResponseEncoder(byte protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.oneone.OneToOneEncoder#encode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
	 */
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if(msg==null) return null;
		byte[] payload = msg.toString().getBytes();
		ChannelBuffer buffer = ChannelBuffers.buffer(BASELINE_SIZE + payload.length);
		buffer.writeBytes(ZABBIX_HEADER);
		buffer.writeByte(protocolVersion);
		buffer.writeBytes(encodeLittleEndianLongBytes(payload.length));
		buffer.writeBytes(payload);		
		return buffer;
	}
	
	/**
	 * Returns the passed long in the form of a little endian formatted byte array 
	 * @param payloadLength The long value to encode
	 * @return an byte array
	 */
	protected byte[] encodeLittleEndianLongBytes(long payloadLength) {
		return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(payloadLength).array();
	}
	
	
}
