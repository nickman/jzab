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
import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLogger;

/**
 * <p>Title: ZabbixLoggingHandler</p>
 * <p>Description: Netty logging handler extension to reformat the <b>ZBXD</b> header so messages are hex dumped in a more readable way.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.logging.ZabbixLoggingHandler</code></p>
 */

public class ZabbixLoggingHandler extends LoggingHandler {
    private static final String NEWLINE = String.format("%n");
    /** True if the native byte order is littleendian */
    public static final boolean isLittleEndian = ByteOrder.nativeOrder().getClass().equals(ByteOrder.LITTLE_ENDIAN);
    private static final String[] BYTE2HEX = new String[256];
    private static final String[] HEXPADDING = new String[16];
    private static final String[] BYTEPADDING = new String[16];
    private static final char[] BYTE2CHAR = new char[256];
    private final boolean hexDump;
    static {
        int i;

        // Generate the lookup table for byte-to-hex-dump conversion
        for (i = 0; i < 10; i ++) {
            StringBuilder buf = new StringBuilder(3);
            buf.append(" 0");
            buf.append(i);
            BYTE2HEX[i] = buf.toString();
        }
        for (; i < 16; i ++) {
            StringBuilder buf = new StringBuilder(3);
            buf.append(" 0");
            buf.append((char) ('a' + i - 10));
            BYTE2HEX[i] = buf.toString();
        }
        for (; i < BYTE2HEX.length; i ++) {
            StringBuilder buf = new StringBuilder(3);
            buf.append(' ');
            buf.append(Integer.toHexString(i));
            BYTE2HEX[i] = buf.toString();
        }

        // Generate the lookup table for hex dump paddings
        for (i = 0; i < HEXPADDING.length; i ++) {
            int padding = HEXPADDING.length - i;
            StringBuilder buf = new StringBuilder(padding * 3);
            for (int j = 0; j < padding; j ++) {
                buf.append("   ");
            }
            HEXPADDING[i] = buf.toString();
        }

        // Generate the lookup table for byte dump paddings
        for (i = 0; i < BYTEPADDING.length; i ++) {
            int padding = BYTEPADDING.length - i;
            StringBuilder buf = new StringBuilder(padding);
            for (int j = 0; j < padding; j ++) {
                buf.append(' ');
            }
            BYTEPADDING[i] = buf.toString();
        }

        // Generate the lookup table for byte-to-char conversion
        for (i = 0; i < BYTE2CHAR.length; i ++) {
            if (i <= 0x1f || i >= 0x7f) {
                BYTE2CHAR[i] = '.';
            } else {
                BYTE2CHAR[i] = (char) i;
            }
        }
    }
	
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
            String msg = e.toString();

            // Append hex dump if necessary.
            if (hexDump && e instanceof MessageEvent) {
                MessageEvent me = (MessageEvent) e;
                if (me.getMessage() instanceof ChannelBuffer) {
                    msg += formatBuffer((ChannelBuffer) me.getMessage());
                }
            }

            // Log the message (and exception if available.)
            if (e instanceof ExceptionEvent) {
                getLogger().log(getLevel(), msg, ((ExceptionEvent) e).getCause());
            } else {
                getLogger().log(getLevel(), msg);
            }
        }
		
	}
	
	/** The Zabbix Header */
	private static final byte[] ZABBIX_HEADER =  "ZBXD".getBytes();
	/** The zabbix response baseline size for creating the downstream channel buffer */
	public static final int BASELINE_SIZE = ZABBIX_HEADER.length + 9;  // one byte for protocol, 8 bytes for length
	
	/**
	 * Decodes the little endian encoded bytes to a long
	 * @param bytes The bytes to decode
	 * @return the decoded long value
	 */
	public static long decodeLittleEndianLongBytes(byte[] bytes) {
		return ((ByteBuffer) ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put(bytes).flip()).getLong();
	}

	
	
    private static String formatBuffer(ChannelBuffer buf) {
        int length = buf.readableBytes();
        int rows = length / 16 + (length % 15 == 0? 0 : 1) + 4;
        StringBuilder zbxHeader = new StringBuilder(60);
        int extraChars = 0;
        if(!isLittleEndian && length >= BASELINE_SIZE) {
        	byte[] hdrbuff = new byte[ZABBIX_HEADER.length];
        	buf.getBytes(0, hdrbuff, 0, ZABBIX_HEADER.length);
        	if(Arrays.equals(hdrbuff, ZABBIX_HEADER)) {
        		byte[] messageSizeBytes = new byte[8];
        		buf.getBytes(5, messageSizeBytes, 0, 8);
        		long messageSize = decodeLittleEndianLongBytes(messageSizeBytes);
        		zbxHeader.append(
                        NEWLINE + "         +-------------------------------------------------+" + 
                        NEWLINE + "         |"
        	} 
        }

        StringBuilder dump = new StringBuilder((rows * 80)+extraChars);
        
        dump.append(
                NEWLINE + "         +-------------------------------------------------+" +
                NEWLINE + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" +
                NEWLINE + "+--------+-------------------------------------------------+----------------+");

        final int startIndex = buf.readerIndex();
        final int endIndex = buf.writerIndex();

        int i;
        for (i = startIndex; i < endIndex; i ++) {
            int relIdx = i - startIndex;
            int relIdxMod16 = relIdx & 15;
            if (relIdxMod16 == 0) {
                dump.append(NEWLINE);
                dump.append(Long.toHexString(relIdx & 0xFFFFFFFFL | 0x100000000L));
                dump.setCharAt(dump.length() - 9, '|');
                dump.append('|');
            }
            dump.append(BYTE2HEX[buf.getUnsignedByte(i)]);
            if (relIdxMod16 == 15) {
                dump.append(" |");
                for (int j = i - 15; j <= i; j ++) {
                    dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
                }
                dump.append('|');
            }
        }

        if ((i - startIndex & 15) != 0) {
            int remainder = length & 15;
            dump.append(HEXPADDING[remainder]);
            dump.append(" |");
            for (int j = i - remainder; j < i; j ++) {
                dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
            }
            dump.append(BYTEPADDING[remainder]);
            dump.append('|');
        }

        dump.append(
                NEWLINE + "+--------+-------------------------------------------------+----------------+");

        return dump.toString();
    }
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.logging.LoggingHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		super.handleUpstream(ctx, e);
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 */
	public ZabbixLoggingHandler() {
		hexDump = true;
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param level The netty logging level for this handler
	 */
	public ZabbixLoggingHandler(InternalLogLevel level) {
		super(level);
		hexDump = true;
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(boolean hexDump) {
		super(hexDump);
		this.hexDump = hexDump;
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param clazz The logger name
	 */
	public ZabbixLoggingHandler(Class<?> clazz) {
		super(clazz);
		this.hexDump = true;
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param name The logger name
	 */
	public ZabbixLoggingHandler(String name) {
		super(name);
		this.hexDump = true;
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param level The netty logging level for this handler
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(InternalLogLevel level, boolean hexDump) {
		super(level, hexDump);
		this.hexDump = hexDump;
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param clazz The logger name
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(Class<?> clazz, boolean hexDump) {
		super(clazz, hexDump);
		this.hexDump = hexDump;
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param clazz The logger name
	 * @param level The netty logging level for this handler
	 */
	public ZabbixLoggingHandler(Class<?> clazz, InternalLogLevel level) {
		super(clazz, level);
		this.hexDump = true;
	}

	/**
	 * Creates a new ZabbixLoggingHandler
	 * @param name The logger name
	 * @param hexDump true to enable hex display of transferring buffers
	 */
	public ZabbixLoggingHandler(String name, boolean hexDump) {
		super(name, hexDump);
		this.hexDump = hexDump;
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
		this.hexDump = hexDump;
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
		this.hexDump = hexDump;
	}

}
