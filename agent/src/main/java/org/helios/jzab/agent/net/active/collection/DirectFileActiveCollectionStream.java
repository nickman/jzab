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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.util.FileDeletor;
import org.helios.jzab.agent.util.ReadableWritableByteChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.DefaultFileRegion;

/**
 * <p>Title: DirectFileActiveCollectionStream</p>
 * <p>Description: A collection stream that buffers check results to disk</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.collection.DirectFileActiveCollectionStream</code></p>
 */
public class DirectFileActiveCollectionStream extends ActiveCollectionStream {
	/** The temp file to stream results through */
	protected final File tmpFile;
	/** The random access file wrapper around the temp file*/ 
	protected final RandomAccessFile raf;
	/** The temp file's NIO file channel */
	protected final FileChannel fileChannel;
	/**
	 * Creates a new DirectFileActiveCollectionStream
	 * @param buffer the underlying buffer
	 * @param type The collection stream type
	 */
	public DirectFileActiveCollectionStream(ReadableWritableByteChannelBuffer buffer, ActiveCollectionStreamType type) {
		super(buffer, type);
		try {
			tmpFile = File.createTempFile("jzab-coll", ".tmp");
			raf = new RandomAccessFile(tmpFile, "rw");
			fileChannel = raf.getChannel();
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to create DirectFileActiveCollectionStream instance", e);
		}
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
	
	protected ChannelFutureListener getCleanUpListener() {		
		return new ChannelFutureListener() {
			/**
			 * {@inheritDoc}
			 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
			 */
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {				
				FileDeletor.closeOnExit(raf);
				FileDeletor.closeOnExit(fileChannel);				
				FileDeletor.deleteOnExit(tmpFile);
			}
		};
	}
	
	
	
	/**Writes the file to the channel
	 * @param channel The channel to write to 
	 * @return the channel future
	 */
	protected ChannelFuture writeFile(Channel channel) {
		if(this.type.isDirect()) {
			return writeFileDirect(channel);
		} else {
			return writeFileUserSpace(channel);
		}
	}
	
	/**
	 * Executes a direct write to the channel
	 * @param channel The channel to write to
	 * @return the write future
	 */
	protected ChannelFuture writeFileDirect(Channel channel) {
		return channel.write(new DefaultFileRegion(fileChannel, 0, tmpFile.length(), true));
	}
	
	/**
	 * Reads the contents of the file into a byte buffer, wraps it and writes it to the channel
	 * @param channel The channel to write to
	 * @return the write future
	 * @throws IOException
	 */
	protected ChannelFuture writeFileUserSpace(Channel channel)  {
		try {
			int fileSize = (int) fileChannel.size();
			log.debug("Allocated[{}] Byte Buffer During writeFileUserSpace", fileSize);
			ByteBuffer buff = ByteBuffer.allocate(fileSize);
			long bytes = fileChannel.read(buff, 0);
			log.debug("Read [{}] Bytes From File During writeFileUserSpace", bytes);
			buff.flip();
			
			//return channel.write(ChannelBuffers.wrappedBuffer(ChannelBuffers.wrappedBuffer(buff), ChannelBuffers.wrappedBuffer(new byte[]{1})));
			return channel.write(ChannelBuffers.wrappedBuffer(ChannelBuffers.wrappedBuffer(buff)));
		} catch (Exception e) {
			throw new RuntimeException("Failed to user space write file to channel", e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#close()
	 */
	@Override
	public boolean close() {
		try {			
			fileChannel.force(true);
			//fileChannel.close();
			return super.close();
		} catch (Exception e) {			
			return false;
		}		
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#rewritePayloadLength()
	 */
	public void rewritePayloadLength() {
		try {
			MappedByteBuffer mbb = fileChannel.map(MapMode.READ_WRITE, lengthPosition, 8).load();
			mbb.put(encodeLittleEndianLongBytes(byteCount));
			mbb.force();
		} catch (Exception e) {
			throw new RuntimeException("Failed to rewritePayloadLength", e);
		}		
	}
	
	
	@Override
	public void trimLastCharacter() {
		if(completedChecks>0) {
			try {
				fileChannel.position(fileChannel.size()-1);
				byteCount--;
			} catch (Exception e) {
				throw new RuntimeException("Failed to trimLastCharacter", e);
			}
		}		
	}	
	
	protected int flushToFile(int bytesWritten) {
		try {
			long bytesTransferred = 0L; 
			if(this.type.isDirect()) {
				bytesTransferred = fileChannel.transferFrom(buffer, fileChannel.position(), bytesWritten);
				fileChannel.position(fileChannel.position()+bytesTransferred);				
			} else {
				bytesTransferred = fileChannel.write(buffer.toByteBuffer());				
			}
			buffer.reset();
			if(bytesTransferred!=bytesWritten) {
				log.warn("Oi! bytesTransferred!=bytesWritten !  [{}] != [{}]", bytesTransferred, bytesWritten);
			}
			return bytesWritten;
		} catch (Exception e) {
			throw new RuntimeException("Failed to flush buffer to file", e);
		}
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
	

}
