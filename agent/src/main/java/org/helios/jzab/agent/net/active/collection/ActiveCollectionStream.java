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
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.net.active.ActiveAgent;
import org.helios.jzab.agent.net.active.ActiveHost;
import org.helios.jzab.agent.net.codecs.ResponseRoutingHandler;
import org.helios.jzab.agent.net.routing.JSONResponseHandler;
import org.helios.jzab.agent.util.ReadableWritableByteChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ActiveCollectionStream</p>
 * <p>Description: Base class implementation of the active collection stream</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.AbstractActiveCollectionStream</code></p>
 */
public class ActiveCollectionStream implements IActiveCollectionStream {
	/** The results accumulation buffer */
	protected final ReadableWritableByteChannelBuffer buffer;
	
	/** The written byte count, not including the ZABX header, protocol and length */
	protected long byteCount = 0L;	
	/** The number of check results written */
	protected int resultCount = 0;
	/** The starting position of the ZABX payload length */
	protected int lengthPosition = 0;
	
	/** The UTF-8 charset */
	protected final Charset charSet = Charset.forName("UTF-8");
	
	/** static logger */
	protected static final Logger log = LoggerFactory.getLogger(ActiveCollectionStream.class);

	/**
	 * Executes a full ActiveHost check submission using the default byte order and buffer size
	 * @param type The collection stream type
	 * @param host The active host to execute and submit checks for
	 * @param channel The netty channel to send the results on
	 */	
	public static void execute(ActiveCollectionStreamType type, ActiveHost host, Channel channel) {
		execute(ByteOrder.nativeOrder(), DEFAULT_COLLECTION_BUFFER_SIZE, type, host, channel);
	}
	
	/**
	 * Executes a full ActiveHost check submission.
	 * @param order The byte order of the buffer
	 * @param size The size of the buffer
	 * @param type The collection stream type
	 * @param host The active host to execute and submit checks for
	 * @param channel The netty channel to send the results on
	 */
	public static void execute(ByteOrder order, int size, ActiveCollectionStreamType type, final ActiveHost host, final Channel channel) {
		Map<String, String> route = new HashMap<String, String>(1);
		route.put(JSONResponseHandler.KEY_REQUEST, JSONResponseHandler.VALUE_ACTIVE_CHECK_SUBMISSION);
		ResponseRoutingHandler.ROUTING_OVERRIDE.set(channel, route);
		log.debug("Starting Collection Stream for Active Host [{}] for send to [{}]", host, channel);
		final long startTime = System.currentTimeMillis();
		IActiveCollectionStream collector = type.newCollectionStream(order, size);
		log.debug("Collector for Active Host [{}] is [{}]", host, collector);
		try {
			collector.writeHeader();
			collector.collect(host);
			collector.trimLastCharacter();
			collector.writeJSONCloser();
			collector.rewritePayloadLength();
			collector.close();
			final long totalSize = collector.getByteCount() + BASELINE_SIZE;
			collector.writeToChannel(channel).addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()) {
						long elapsed = System.currentTimeMillis()-startTime;
						log.info("Collection Stream for Active Host [{}] Completed in [{}] ms.", host, elapsed);
						log.info("Size [{}] bytes.:", totalSize);
					}
				}
			});
		} catch (Exception e) {
			log.error("Submission Failed", e);
		}
		
	}
	
	
	/**
	 * Creates a new ActiveCollectionStream
	 * @param buffer The accumulation buffer
	 */
	public ActiveCollectionStream(ReadableWritableByteChannelBuffer buffer) {
		this.buffer = buffer;		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#rewritePayloadLength()
	 */
	public void rewritePayloadLength() {
		buffer.setBytes(lengthPosition, encodeLittleEndianLongBytes(byteCount-1));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#getCollectionCloser()
	 */
	public byte[] getCollectionCloser() {
		return String.format("],\"clock\":%s}", SystemClock.currentTimeSecs()).getBytes();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#collect(long)
	 */
	public void collect(long delay) {
		ActiveAgent.getInstance().executeChecks(delay, this);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#collect(org.helios.jzab.agent.net.active.ActiveHost)
	 */
	public void collect(ActiveHost activeHost) {
		activeHost.executeChecks(this);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#addResult(java.lang.CharSequence)
	 */
	@Override
	public void addResult(CharSequence result)  {
		try {
			byteCount += buffer.write(charSet.encode(CharBuffer.wrap(result)));
			resultCount++;
		} catch (Exception e) {
			throw new RuntimeException("Failed to add result to collection stream [" + result + "]", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#writeToChannel(org.jboss.netty.channel.Channel)
	 */
	@Override
	public ChannelFuture writeToChannel(Channel channel) {
		return channel.write(buffer);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#trimLastCharacter()
	 */
	@Override
	public void trimLastCharacter() {
		if(resultCount>0) {
			buffer.writerIndex(buffer.writerIndex()-1);
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#writeHeader()
	 */
	@Override
	public int writeHeader() {
		buffer.writeBytes(ZABBIX_HEADER);
		lengthPosition = buffer.writerIndex();
		buffer.writeLong(0L);
		buffer.writeBytes(AGENT_DATA_HEADER);
		byteCount += AGENT_DATA_HEADER.length;
		return buffer.writerIndex();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#writeJSONCloser()
	 */
	@Override
	public int writeJSONCloser() {
		byte[] closer = getCollectionCloser();
		buffer.writeBytes(closer);
		byteCount += closer.length;		
		return closer.length;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#close()
	 */
	@Override
	public boolean close() {
		return true;
	}

	/**
	 * Returns the passed long in the form of a little endian formatted byte array 
	 * @param payloadLength The long value to encode
	 * @return an byte array
	 */
	public static byte[] encodeLittleEndianLongBytes(long payloadLength) {
		return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(payloadLength).array();
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
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ActiveCollectionStream [");
		if (buffer != null)
			builder.append("buffer=").append(buffer).append(", ");
		builder.append("byteCount=").append(byteCount).append(", resultCount=")
				.append(resultCount).append(", lengthPosition=")
				.append(lengthPosition).append("]");
		return builder.toString();
	}

	/**
	 * Returns the number of bytes in the payload
	 * @return the number of bytes in the payload
	 */
	public long getByteCount() {
		return byteCount;
	}

	/**
	 * Returns the number of results collected
	 * @return the number of results collected
	 */
	public int getResultCount() {
		return resultCount;
	}

	/**
	 * Returns the current buffer write position
	 * @return the current buffer write position
	 */
	public int getLengthPosition() {
		return lengthPosition;
	}
}
