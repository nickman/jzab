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
 * <p>Title: FileActiveCollectionStream</p>
 * <p>Description: A collection stream that buffers check results to disk</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.collection.FileActiveCollectionStream</code></p>
 */
public class FileActiveCollectionStream extends ActiveCollectionStream {
	/** The temp file to stream results through */
	protected final File tmpFile;
	/** The random access file wrapper around the temp file*/ 
	protected final RandomAccessFile raf;
	/** The temp file's NIO file channel */
	protected final FileChannel fileChannel;
	/**
	 * Creates a new FileActiveCollectionStream
	 * @param buffer the underlying buffer
	 */
	public FileActiveCollectionStream(ReadableWritableByteChannelBuffer buffer) {
		super(buffer);
		try {
			tmpFile = File.createTempFile("jzab-coll", ".tmp");
			FileDeletor.deleteOnExit(tmpFile);
			raf = new RandomAccessFile(tmpFile, "rw");
			fileChannel = raf.getChannel();
			FileDeletor.closeOnExit(fileChannel);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create FileActiveCollectionStream instance", e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.ActiveCollectionStream#writeToChannel(org.jboss.netty.channel.Channel)
	 */
	@Override
	public ChannelFuture writeToChannel(Channel channel) {
		try {
			//ChannelFuture cf = channel.write(ChannelBuffers.wrappedBuffer(fileChannel.map(MapMode.READ_ONLY, 0, tmpFile.length())));
			ChannelFuture cf = channel.write(new DefaultFileRegion(fileChannel, 0, tmpFile.length(), true));
			final FileChannel fChannel = fileChannel;
			cf.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					try { fChannel.close(); } catch (Exception e) {}
					tmpFile.delete();
					log.debug("Closed FileChannel for [{}]", tmpFile);					
					if(future.isSuccess()) {					
						long elapsed = SystemClock.currentTimeMillis()-startTime;
						completeElapsedTime.set(elapsed);
						log.debug("Collection Stream of size [{}] bytes Completed in [{}] ms.",  getTotalSize(), elapsed);
					}
				}
			});
			return cf;
		} catch (Exception e) {
			log.error("Failed to write buffered file to channel [{}]", channel, e);
			throw new RuntimeException("Failed to write buffered file to channel", e);
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
			return true;
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
	
	@Override
	public void trimLastCharacter() {
		if(resultCount>0) {
			try {
				fileChannel.position(fileChannel.size()-1);
			} catch (Exception e) {
				throw new RuntimeException("Failed to trimLastCharacter", e);
			}
		}		
	}	
	
	protected int flushToFile(int bytesWritten) {
		try {
			long bytesTransferred = fileChannel.transferFrom(buffer, fileChannel.position(), bytesWritten);
			fileChannel.position(fileChannel.position()+bytesTransferred);
			log.debug("Result Written. Bytes:[{}]. FC Position:[{}]", bytesTransferred, fileChannel.position());
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
	public void addResult(CharSequence result)  {
		try {
			int bytesWritten = buffer.write(charSet.encode(CharBuffer.wrap(result)));
			flushToFile(bytesWritten);
			byteCount += bytesWritten;
			resultCount++;
		} catch (Exception e) {
			throw new RuntimeException("Failed to add result to collection stream [" + result + "]", e);
		}
	}
	

}
