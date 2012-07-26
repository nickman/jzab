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

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>Title: ZabbixResponseDecoder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.codecs.ZabbixResponseDecoder</code></p>
 */

public class ZabbixResponseDecoder extends ReplayingDecoder<ZabbixResponse> {
	/** Instance logger */
	protected Logger log;

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer, java.lang.Enum)
	 */
	@Override
	protected JSONObject decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, ZabbixResponse state) throws Exception {		
		log = LoggerFactory.getLogger(getClass() + "[" + channel.getRemoteAddress() + "]" );
		if(state==null) {
			state = ZabbixResponse.ZHEADER;
			log.trace("Started Response Decode [{}]", state);
		}		
		switch(state) {
			case ZHEADER:
				byte[] header = new byte[4];
				 buffer.readBytes(header);
				 log.trace("Read Response Header [{}]", new String(header));
				 if(!Arrays.equals(ZabbixConstants.ZABBIX_HEADER,header)) {
					 throw new Exception("Invalid Header " + Arrays.toString(header), new Throwable());
				 }
				 checkpoint(ZabbixResponse.ZPROTOCOL);
			case ZPROTOCOL:
				byte protocol = buffer.readByte();
				log.trace("Read Response Protocol [{}]", protocol);
				if(protocol!=ZabbixConstants.ZABBIX_PROTOCOL) {
					 throw new Exception("Invalid Protocol [" + protocol + "]", new Throwable());
				}
				checkpoint(ZabbixResponse.ZLENGTH);
			case ZLENGTH:
				byte[] rLength = new byte[8];
				buffer.readBytes(rLength);
				long length = ZabbixConstants.decodeLittleEndianLongBytes(rLength);
				ctx.setAttachment(length);
				log.trace("Read Response Length: [{}]", length);
				checkpoint(ZabbixResponse.JSON);
			case JSON:
				length = (Long)ctx.getAttachment();
				byte[] jsonBytes = new byte[(int)length];
				buffer.readBytes(jsonBytes);
				JSONObject obj = new JSONObject(new String(jsonBytes));
				log.trace("Decoded JSONObject [{}]",  obj.toString());
				return obj;
		}
		return null;
	}

}
