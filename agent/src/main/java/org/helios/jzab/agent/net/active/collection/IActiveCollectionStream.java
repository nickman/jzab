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
package org.helios.jzab.agent.net.active.collection;

import java.nio.ByteBuffer;

import org.helios.jzab.agent.net.active.ActiveHost;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 * <p>Title: IActiveCollectionStream</p>
 * <p>Description: Defines a container that is used to accumulate the results of an active check sweep</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.IActiveCollectionStream</code></p>
 */

public interface IActiveCollectionStream extends IResultCollector {
	/** The protocol version of the zabbix passive response processor */
	public static final byte ZABBIX_PROTOCOL = 1;

	/** The zabbix response header in bytes */
	public static final byte[] ZABBIX_HEADER = ByteBuffer.allocate(5).put("ZBXD".getBytes()).put((byte)1).array();  
			//("ZBXD" + ZABBIX_PROTOCOL).getBytes();
	/** The zabbix response baseline size for creating the downstream channel buffer */
	public static final int BASELINE_SIZE = ZABBIX_HEADER.length + 9;  // one byte for protocol, 8 bytes for length
	
	/** The JSON Opener */
	public static final byte[] AGENT_DATA_HEADER = "{ \"request\":\"agent data\", \"data\":[".getBytes();
		

	/** The default buffer size for collection buffers */
	public static final int DEFAULT_COLLECTION_BUFFER_SIZE = 4096 * 10;
	

	
	/**
	 * Executes check results for all active checks with the specified delay
	 * @param delay the delay of the active checks to execute 
	 */
	public void collect(long delay);
	
	/**
	 * Executes check results on all active checks for the passed active host
	 * @param activeHost The active host to execute checks for
	 */
	public void collect(ActiveHost activeHost);
	
	/**
	 * Flushes the accumulated results to the passed channel
	 * @param channel The channel to write the results to
	 * @return The write completion future
	 */
	public ChannelFuture writeToChannel(Channel channel);
	
	/**
	 * Removes the trailing comma in the accumulated bytes (that represent a JSON array)
	 */
	public void trimLastCharacter();
	
	/**
	 * Writes the initial header with a zero payload size (which will be updated later)
	 * @return the number of bytes written
	 */
	public int writeHeader();
	
	/**
	 * Returns the JSON closer bytes
	 * @return the JSON closer bytes
	 */
	public byte[] getCollectionCloser();

	
	/**
	 * Writes the JSON closer
	 * @return the number of bytes written
	 */
	public int writeJSONCloser();
	
	/**
	 * Rewrites the actual payload length into the ZABX header
	 */
	public void rewritePayloadLength();
	
	/**
	 * Closes any resources on collection completion
	 * @return true if resources were successfully closed, false otherwise
	 */
	public boolean close();
	
	/**
	 * Returns the number of bytes in the payload
	 * @return the number of bytes in the payload
	 */
	public long getByteCount();

	/**
	 * Returns the number of results collected
	 * @return the number of results collected
	 */
	public int getResultCount();

	/**
	 * Returns the current buffer write position
	 * @return the current buffer write position
	 */
	public int getLengthPosition();
	
	
	
}
