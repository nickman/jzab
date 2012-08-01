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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.agent.net.active.ActiveAgent;
import org.helios.jzab.agent.net.active.ActiveClient;
import org.helios.jzab.agent.net.active.ActiveHost;
import org.helios.jzab.agent.net.active.ActiveServer;
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
	/** The collection stream type */
	protected final ActiveCollectionStreamType type;
	/** The written byte count, not including the ZABX header, protocol and length */
	protected long byteCount = 0L;	
	/** The starting position of the ZABX payload length */
	protected int lengthPosition = 0;
	
	/** Indicates if the stream is open for collection. */
	protected final AtomicBoolean open = new AtomicBoolean(true);
	
	/** The elapsed time to execute the checks */
	protected long checksElapsed = -1L;

	/** The number of checks scheduled */
	protected long totalChecks = 0;	
	/** Number of timed out checks */
	protected long timedOutChecks = 0;
	/** Number of completed checks */
	protected long completedChecks = 0;
	
	/** The elapsed time to complete the whole collection */
	protected final AtomicLong completeElapsedTime = new AtomicLong(-1L);
	/** The start time of this collection stream */
	protected final long startTime;
	
	/** The UTF-8 charset */
	protected final Charset charSet = Charset.forName("UTF-8");
	
	/** static logger */
	protected static final Logger log = LoggerFactory.getLogger(ActiveCollectionStream.class);

	/**
	 * Executes a full ActiveHost check submission using the default byte order and buffer size
	 * @param type The collection stream type
	 * @param host The active host to execute and submit checks for
	 * @param channel The netty channel to send the results on
	 * @return The collector stream created for the submission
	 */	
	public static IActiveCollectionStream execute(ActiveCollectionStreamType type, ActiveHost host, Channel channel) {
		return execute(ByteOrder.nativeOrder(), DEFAULT_COLLECTION_BUFFER_SIZE, type, host, channel);
	}
	
	/**
	 * Executes a full delay window check submission using the default byte order and buffer size
	 * @param type The collection stream type
	 * @param commandThreadPolicy The threading polcy for this collection
	 * @param delay The delay to execute and submit checks for
	 * @param agentCollectionTimeout The collection timeout in seconds
	 * @return The collector stream created for the submission
	 */	
	public static IActiveCollectionStream execute(ActiveCollectionStreamType type, CommandThreadPolicy commandThreadPolicy, long delay, long agentCollectionTimeout) {
		return execute(ByteOrder.nativeOrder(), DEFAULT_COLLECTION_BUFFER_SIZE, type, commandThreadPolicy, delay, agentCollectionTimeout);
	}
	
	
	/**
	 * Executes a full ActiveHost check submission.
	 * @param order The byte order of the buffer
	 * @param size The size of the buffer
	 * @param type The collection stream type
	 * @param host The active host to execute and submit checks for
	 * @param channel The netty channel to send the results on
	 * @return The collector stream created for the submission
	 */
	public static IActiveCollectionStream execute(ByteOrder order, int size, ActiveCollectionStreamType type, final ActiveHost host, final Channel channel) {
		Map<String, String> route = new HashMap<String, String>(1);
		route.put(JSONResponseHandler.KEY_REQUEST, JSONResponseHandler.VALUE_ACTIVE_CHECK_SUBMISSION);
		ResponseRoutingHandler.ROUTING_OVERRIDE.set(channel, route);
		log.debug("Starting Collection Stream for Active Host [{}] for send to [{}]", host, channel);		
		final IActiveCollectionStream collector = type.newCollectionStream(order, size);	
		collector.setScheduledChecks(host.getActiveCheckCount());
		try {
			collector.writeHeader();
			collector.collect(host);
			collector.trimLastCharacter();
			collector.writeJSONCloser();
			collector.rewritePayloadLength();
			collector.close();			
			collector.writeToChannel(channel);
		} catch (Exception e) {
			log.error("Submission Failed", e);
		}
		return collector;
		
	}
	
	/**
	 * Executes a full delay window check submission.
	 * @param order The byte order of the buffer
	 * @param size The size of the buffer
	 * @param type The collection stream type
	 * @param commandThreadPolicy The threading policy for this collection
	 * @param delay The delay window to execute and submit checks for
	 * @param agentCollectionTimeout The agent collection timeout in seconds
	 * @return The collector stream created for the submission
	 */
	public static IActiveCollectionStream execute(ByteOrder order, int size, final ActiveCollectionStreamType type, final CommandThreadPolicy commandThreadPolicy, final long delay, final long agentCollectionTimeout) {
		final ActiveAgent agent = ActiveAgent.getInstance();
		final ActiveClient client = ActiveClient.getInstance();
		Set<ActiveServer> targetCollectionServers = agent.getServersForDelay(delay);				
		final ExecutorService executorService = ThreadPoolFactory.getInstance("TaskExecutor");
		for(final ActiveServer activeServer: targetCollectionServers) {
			final IActiveCollectionStream collector = type.newCollectionStream(order, size);
			collector.setScheduledChecks(activeServer.getChecksForDelay(delay).size());
			executorService.execute(new Runnable(){
				public void run() {					
					try {
						collector.writeHeader();
						Collection<? extends Callable<Void>> tasks = commandThreadPolicy.createPlan(delay, activeServer, collector);
						try {
							long start = SystemClock.currentTimeMillis();
							executorService.invokeAll(tasks, agentCollectionTimeout, TimeUnit.SECONDS);
							collector.updateCheckCollectionTime(SystemClock.currentTimeMillis()-start);
							collector.close();							
							collector.setTimedOutChecks(collector.getScheduledChecks()-collector.getCompletedChecks());
						} catch (InterruptedException e) {
							log.error("Collection for ActiveServer [{}] was interrupted", activeServer, e);
							return;
						}
						collector.trimLastCharacter();
						collector.writeJSONCloser();
						collector.rewritePayloadLength();
						collector.close();
						Channel channel = client.newChannel(activeServer);
						collector.writeToChannel(channel);		
					} catch (Exception e) {
						log.error("Submission Failed", e);
					}
				}
			});
		}
		
//		Map<String, String> route = new HashMap<String, String>(1);
//		route.put(JSONResponseHandler.KEY_REQUEST, JSONResponseHandler.VALUE_ACTIVE_CHECK_SUBMISSION);
//		ResponseRoutingHandler.ROUTING_OVERRIDE.set(channel, route);
//		log.debug("Starting Collection Stream for Delay Window [{}] for send to [{}]", delay, channel);		
//		final IActiveCollectionStream collector = type.newCollectionStream(order, size);		
//		try {
//			collector.writeHeader();
//			collector.collect(delay);
//			collector.trimLastCharacter();
//			collector.writeJSONCloser();
//			collector.rewritePayloadLength();
//			collector.close();			
//			collector.writeToChannel(channel).addListener(new ChannelFutureListener() {
//				@Override
//				public void operationComplete(ChannelFuture future) throws Exception {
//					if(future.isSuccess()) {
//						log.debug("Collection Stream Completion {}",  collector);
//					} else {
//						log.debug("Collection Stream Failed", future.getCause());
//					}
//					future.getChannel().close();
//				}
//			});			
//		} catch (Exception e) {
//			log.error("Submission Failed", e);
//		}
//		return collector;
		
		return null;
		
	}
	
	
	/**
	 * Returns the total size of the request to be sent to the zabbix server
	 * @return the total size (in bytes) of the request to be sent to the zabbix server
	 */
	@Override
	public long getTotalSize() {
		return byteCount + BASELINE_SIZE;
	}
	
	/**
	 * Returns the elapsed time to execute the checks in ms.
	 * @return the elapsed time to execute the checks in ms.
	 */
	@Override
	public long getCheckExecutionElapsedTime() {
		return checksElapsed;
	}
	
	/**
	 * Returns the total elapsed time to execute this collection stream in ms.
	 * @return the total elapsed time to execute this collection stream in ms.
	 */
	@Override
	public long getTotalElapsedTime() {
		return completeElapsedTime.get();
	}
	
	
	/**
	 * Creates a new ActiveCollectionStream
	 * @param buffer The accumulation buffer
	 * @param type The collection stream type
	 */
	public ActiveCollectionStream(ReadableWritableByteChannelBuffer buffer, ActiveCollectionStreamType type) {
		this.buffer = buffer;
		this.type = type;
		startTime = System.currentTimeMillis();
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#rewritePayloadLength()
	 */
	@Override
	public void rewritePayloadLength() {
		buffer.setBytes(lengthPosition, encodeLittleEndianLongBytes(byteCount));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#getCollectionCloser()
	 */
	@Override
	public byte[] getCollectionCloser() {
		return String.format("],\"clock\":%s}", SystemClock.currentTimeSecs()).getBytes();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#collect(long)
	 */
	@Override
	public void collect(long delay) {		
		long start = System.currentTimeMillis();
		ActiveAgent.getInstance().executeChecks(delay, this);
		checksElapsed = System.currentTimeMillis()-start;

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#collect(org.helios.jzab.agent.net.active.ActiveHost)
	 */
	@Override
	public void collect(ActiveHost activeHost) {
		long start = System.currentTimeMillis();
		activeHost.executeChecks(this);
		checksElapsed = System.currentTimeMillis()-start;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IResultCollector#getCollectTime()
	 */
	public long getCollectTime() {
		return startTime;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#addResult(java.lang.CharSequence)
	 */
	@Override
	public synchronized void addResult(CharSequence result)  {
		if(!open.get()) return;
		try {
			byteCount += buffer.write(charSet.encode(CharBuffer.wrap(result)));
			completedChecks++;
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
			ChannelFuture cf = channel.write(buffer);
			final ActiveCollectionStream collector = this;
			cf.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()) {
						future.getChannel().getCloseFuture().addListener(new ChannelFutureListener() {							
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								completeElapsedTime.set(SystemClock.currentTimeMillis()-startTime);
								log.debug("Collection Stream Write Completed {}",  collector);
							}
						});
					}
				}
		});
		return cf;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#trimLastCharacter()
	 */
	@Override
	public void trimLastCharacter() {
		if(completedChecks>0) {
			buffer.writerIndex(buffer.writerIndex()-1);
			byteCount--;
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
		open.set(false);
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
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		builder.append("\n\tTotalSize (bytes):");
		builder.append(getTotalSize());
		builder.append("\n\tCheck Time (ms):");
		builder.append(getCheckExecutionElapsedTime());
		builder.append("\n\tTotal Elapsed Time (ms):");
		builder.append(getTotalElapsedTime());
		builder.append("\n\tMessage Size (bytes):");
		builder.append(getByteCount());
		builder.append("\n\tScheduled Check Count:");
		builder.append(getScheduledChecks());
		builder.append("\n\tCompleted Check Count:");
		builder.append(getCompletedChecks());
		builder.append("\n\tTimedout Check Count:");
		builder.append(getTimedOutChecks());
		
		builder.append("\n]");
		return builder.toString();
	}

	/**
	 * Returns the number of bytes in the payload
	 * @return the number of bytes in the payload
	 */
	@Override
	public long getByteCount() {
		return byteCount;
	}


	/**
	 * Returns the current buffer write position
	 * @return the current buffer write position
	 */
	@Override
	public int getLengthPosition() {
		return lengthPosition;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IActiveCollectionStream#updateCheckCollectionTime(long)
	 */
	@Override
	public void updateCheckCollectionTime(long elapsed) {
		checksElapsed = elapsed;
		
	}

	/**
	 * Returns the number of checks that timed out
	 * @return the timedOutChecks
	 */
	public long getTimedOutChecks() {
		return timedOutChecks;
	}

	/**
	 * Sets the number of checks that timed out
	 * @param timedOutChecks the timedOutChecks to set
	 */
	public void setTimedOutChecks(long timedOutChecks) {
		this.timedOutChecks = timedOutChecks;
	}

	/**
	 * Returns the number of checks that completed
	 * @return the completedChecks
	 */
	public long getCompletedChecks() {
		return completedChecks;
	}

	/**
	 * Sets the number of checks that completed
	 * @param completedChecks the completedChecks to set
	 */
	public void setCompletedChecks(long completedChecks) {
		this.completedChecks = completedChecks;
	}
	
	/**
	 * Returns the number of checks that were scheduled
	 * @return the number of checks that were scheduled
	 */
	public long getScheduledChecks() {
		return totalChecks;
	}

	/**
	 * Sets the number of checks that were scheduled
	 * @param scheuduledChecks the number of checks that were scheduled
	 */
	public void setScheduledChecks(long scheduledChecks) {
		totalChecks = scheduledChecks;
	}
	
}
