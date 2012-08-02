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

import java.nio.CharBuffer;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.util.ReadableWritableByteChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * <p>Title: AbstractFileActiveCollectionStream</p>
 * <p>Description: A collection stream that buffers collection results to disk</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream</code></p>
 */
public abstract class AbstractFileActiveCollectionStream extends ActiveCollectionStream {
	/**
	 * Creates a new AbstractFileActiveCollectionStream
	 * @param buffer The collection buffer
	 * @param type The collection type
	 */
	public AbstractFileActiveCollectionStream(ReadableWritableByteChannelBuffer buffer, ActiveCollectionStreamType type) {
		super(buffer, type);
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#writeToChannel(org.jboss.netty.channel.Channel)
	 */
	@Override
	public ChannelFuture writeToChannel(Channel channel) {
		channel.getCloseFuture().addListener(getCleanUpListener());
		ChannelFuture cf = writeFile(channel);		
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
				} else {
					log.error("Submission Failed", future.getCause());
				}
			}
		});
		return cf;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#addResult(java.lang.CharSequence)
	 */
	@Override
	public synchronized void addResult(CharSequence result)  {
		if(!open.get()) return;
		try {
			int bytesWritten = buffer.write(charSet.encode(CharBuffer.wrap(result)));
			flushToFile(bytesWritten);
			byteCount += bytesWritten;
			completedChecks++;
		} catch (Exception e) {
			throw new RuntimeException("Failed to add result to collection stream [" + result + "]", e);
		}
	}

		
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#writeHeader()
	 */
	@Override
	public int writeHeader() {
		return flushToFile(super.writeHeader());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#writeJSONCloser()
	 */
	@Override
	public int writeJSONCloser() {
		return flushToFile(super.writeJSONCloser());
	
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#close()
	 */
	@Override
	public boolean close() {
		try {			
			return super.close();
		} catch (Exception e) {			
			return false;
		}		
	}

	
	/**
	 * @param bytesWritten
	 * @return
	 */
	protected abstract int flushToFile(int bytesWritten);	
	
	
	/**
	 * Creates a cleanup listener to deallocate any resources after the collection is complete
	 * @return a cleanup listener to deallocate any resources after the collection is complete
	 */
	protected abstract ChannelFutureListener getCleanUpListener();
	
	/**
	 * Writes the file to the channel
	 * @param channel The channel to write to 
	 * @return the channel future
	 */
	protected abstract ChannelFuture writeFile(Channel channel);
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#rewritePayloadLength()
	 */
	public abstract void rewritePayloadLength();	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#trimLastCharacter()
	 */
	@Override
	public abstract void trimLastCharacter();
	
}
