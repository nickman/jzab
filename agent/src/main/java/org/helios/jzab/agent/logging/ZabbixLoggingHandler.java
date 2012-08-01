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
package org.helios.jzab.agent.logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLogger;

/**
 * <p>Title: ZabbixLoggingHandler</p>
 * <p>Description: Netty logging handler extension to reformat the <b>ZBXD</b> header so messages are hex dumped in a more readable way.</p>
 * <p>Prints a header like this before the full dump occurs:<pre>
 * +--------+-------------------------------------------------+----------------+
 *          |  ZABBIX HEADER DETECTED. Protocol:1  Size:89    |
 * +--------+-------------------------------------------------+----------------+
 * </pre></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.logging.ZabbixLoggingHandler</code></p>
 */

public class ZabbixLoggingHandler extends LoggingHandler  {
    
    /** True if the native byte order is littleendian */
    public static final boolean isLittleEndian = ByteOrder.nativeOrder().getClass().equals(ByteOrder.LITTLE_ENDIAN);
	/** The Zabbix Header */
	private static final byte[] ZABBIX_HEADER =  "ZBXD".getBytes();
	/** The Zabbix Header as a ChannelBuffer */
	private static final ChannelBuffer ZABBIX_HEADER_CB = ChannelBuffers.wrappedBuffer(ZABBIX_HEADER); 
	/** The zabbix response baseline size for creating the downstream channel buffer */
	public static final int BASELINE_SIZE = ZABBIX_HEADER.length + 9;  // one byte for protocol, 8 bytes for length
	/** Logging new line char */
	public static final String NEWLINE = String.format("%n");	
	/** Formatting */
	public static final int LINE_LENGTH = 60;
    
	
   /**
	* Logs the specified event to the {@link InternalLogger} returned by
	* {@link #getLogger()}. If hex dump has been enabled for this handler,
	* the hex dump of the {@link ChannelBuffer} in a {@link MessageEvent} will
	* be logged together.
	* @param e The channel event to log
	*/
	@Override
	public void log(ChannelEvent e) {
		
		
        if (getLogger().isEnabled(getLevel())) {
            try {	        	
	            if(e instanceof MessageEvent) {
	            	MessageEvent me = (MessageEvent)e;
	            	if(me.getMessage() instanceof ChannelBuffer) {
		            	ChannelBuffer buf = (ChannelBuffer)me.getMessage();
		            	int length = buf.readableBytes();
			            if(length >= BASELINE_SIZE) {
			            	ChannelBuffer header = findZabbixHeader(buf, length);
			            	if(header!=null) {
			            		byte protocol = header.getByte(0);
			            		long messageSize = -1L;
			            		if(isLittleEndian) {
			            			messageSize = header.getLong(1);
			            		} else {
				            		byte[] messageSizeBytes = new byte[8];
				            		header.getBytes(1, messageSizeBytes, 0, 8);
				            		messageSize = decodeLittleEndianLongBytes(messageSizeBytes);			            			
			            		}
			            		String message = padRight(String.format("         |  ZABBIX HEADER DETECTED. Protocol:%s  Size:%s", protocol, messageSize), LINE_LENGTH-1);
				            	StringBuilder zbxHeader = new StringBuilder(240)				            	
				            	.append(NEWLINE).append("+--------+-------------------------------------------------+----------------+")
				            	.append(NEWLINE).append(message).append("|")
				            	.append(NEWLINE).append("+--------+-------------------------------------------------+----------------+");
				            	getLogger().log(getLevel(), zbxHeader.toString());
			            	}
			            }
	            	}
	            }
            } finally {
            	super.log(e);
            }
        }		
	}
	
	/**
	 * Right pads the passed string
	 * @param s The string to pad
	 * @param n The number of characters to pad out to
	 * @return The padded string
	 */
	public static String padRight(String s, int n) {
	    return String.format("%1$-" + n + "s", s);
	}

	
	/**
	 * Left pads the passed string
	 * @param s The string to pad
	 * @param n The number of characters to pad out to
	 * @return The padded string
	 */
	public static String padLeft(String s, int n) {
	    return String.format("%1$#" + n + "s", s);
	}	
	
	
	/**
	 * Attempts to locate the Zabbix Header in the passed buffer. 
	 * Currently cannot handle a partial header, which might happen if <b>ZBXD</b> is at the end of the buffer, 
	 * and the next buffer contains the remainder.
	 * Assumes only one header per buffer. 
	 * @param buf The ChannelBuffer to search
	 * @param length The length of the ChannelBuffer
	 * @return The protocol and message size portion of the header in a sliced ChannelBuffer, or null if the header was not found. 
	 */
	public static ChannelBuffer findZabbixHeader(ChannelBuffer buf, int length) {
		int index = indexOf(buf, ZABBIX_HEADER_CB);
		if(index==-1) return null;
		if(length-index >= BASELINE_SIZE) {
			return buf.slice(index+4, BASELINE_SIZE);
		} else {
			// partial header .....
		}
		return null;
	}
	
    /**
     * Returns the number of bytes between the readerIndex of the haystack and
     * the first needle found in the haystack.  -1 is returned if no needle is
     * found in the haystack.
     */
    private static int indexOf(ChannelBuffer haystack, ChannelBuffer needle) {
        for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i ++) {
            int haystackIndex = i;
            int needleIndex;
            for (needleIndex = 0; needleIndex < needle.capacity(); needleIndex ++) {
                if (haystack.getByte(haystackIndex) != needle.getByte(needleIndex)) {
                    break;
                } else {
                    haystackIndex ++;
                    if (haystackIndex == haystack.writerIndex() &&
                        needleIndex != needle.capacity() - 1) {
                        return -1;
                    }
                }
            }

            if (needleIndex == needle.capacity()) {
                // Found the needle from the haystack!
                return i - haystack.readerIndex();
            }
        }
        return -1;
    }	
	
	/**
	 * Decodes the little endian encoded bytes to a long
	 * @param bytes The bytes to decode
	 * @return the decoded long value
	 */
	public static long decodeLittleEndianLongBytes(byte[] bytes) {
		return ((ByteBuffer) ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put(bytes).flip()).getLong();
	}

	
	
	

	/**
	 * Creates a new ZabbixLoggingHandler
	 */
	public ZabbixLoggingHandler() {
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param level The netty logging level for this handler
	 */
	public ZabbixLoggingHandler(InternalLogLevel level) {
		super(level);
		
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(boolean hexDump) {
		super(hexDump);
		
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param clazz The logger name
	 */
	public ZabbixLoggingHandler(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param name The logger name
	 */
	public ZabbixLoggingHandler(String name) {
		super(name);
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param level The netty logging level for this handler
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(InternalLogLevel level, boolean hexDump) {
		super(level, hexDump);
		
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param clazz The logger name
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(Class<?> clazz, boolean hexDump) {
		super(clazz, hexDump);
		
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param clazz The logger name
	 * @param level The netty logging level for this handler
	 */
	public ZabbixLoggingHandler(Class<?> clazz, InternalLogLevel level) {
		super(clazz, level);
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param name The logger name
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(String name, boolean hexDump) {
		super(name, hexDump);
		
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param clazz The logger name
	 * @param level The netty logging level for this handler
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(Class<?> clazz, InternalLogLevel level,
			boolean hexDump) {
		super(clazz, level, hexDump);
		
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param name The logger name
	 * @param level The netty logging level for this handler
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(String name, InternalLogLevel level,
			boolean hexDump) {
		super(name, level, hexDump);
		
	}


}
