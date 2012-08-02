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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.helios.jzab.agent.net.active.ActiveClient;
import org.helios.jzab.agent.util.FileDeletor;
import org.helios.jzab.agent.util.ReadableWritableByteChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.DefaultFileRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: DirectFileActiveCollectionStream</p>
 * <p>Description: A collection stream that buffers check results to disk and implements direct memory usage</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.collection.DirectFileActiveCollectionStream</code></p>
 */
public class DirectFileActiveCollectionStream extends AbstractFileActiveCollectionStream {
	/** The temp file to stream results through */
	protected final File tmpFile;
	/** The random access file wrapper around the temp file*/ 
	protected final RandomAccessFile raf;
	/** The temp file's NIO file channel */
	protected final FileChannel fileChannel;
	/** The logger for ActiveClient so we know what it's level is */
	protected static final Logger activeClientLogger = LoggerFactory.getLogger(ActiveClient.class);

	/**
	 * Creates a new DirectFileActiveCollectionStream
	 * @param buffer
	 * @param type
	 */
	public DirectFileActiveCollectionStream(
			ReadableWritableByteChannelBuffer buffer,
			ActiveCollectionStreamType type) {
		super(buffer, type);
		try {
			tmpFile = File.createTempFile("jzab-coll", ".tmp");
			raf = new RandomAccessFile(tmpFile, "rw");
			fileChannel = raf.getChannel();
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to create DirectFileActiveCollectionStreamOLD instance", e);
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#close()
	 */
	@Override
	public boolean close() {
		try {			
			fileChannel.force(true);
			return super.close();
		} catch (Exception e) {			
			return false;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#flushToFile(int)
	 */
	@Override
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
				log.warn("\n\t!!!!!!!!!!!!!!!!!!!!!!!!!!\n\tOi! bytesTransferred!=bytesWritten !  [{}] != [{}]\n\t!!!!!!!!!!!!!!!!!!!!!!!!!!\n", bytesTransferred, bytesWritten);
			}
			return bytesWritten;
		} catch (Exception e) {
			throw new RuntimeException("Failed to flush buffer to file", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#getCleanUpListener()
	 */
	@Override
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

	/**
	 * If the logger for {@link ActiveClient} is debug enabled, will use a non-direct write of the file to the channel
	 * so that the logging handler will display the payload sent. If it is not debug enabled, will use a zero-copy direct transfer
	 * which bypasses the payload dump in the logging handler.
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#writeFile(org.jboss.netty.channel.Channel)
	 */
	@Override
	protected ChannelFuture writeFile(Channel channel) {
		if(activeClientLogger.isDebugEnabled()) {			
			return writeFileUserSpace(channel);
		} else {
			return writeFileDirect(channel);
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
			ByteBuffer buff = ByteBuffer.allocateDirect(fileSize);
			long bytes = fileChannel.read(buff, 0);
			log.debug("Read [{}] Bytes From File During writeFileUserSpace", bytes);
			buff.flip();
			return channel.write(ChannelBuffers.wrappedBuffer(ChannelBuffers.wrappedBuffer(buff)));
		} catch (Exception e) {
			throw new RuntimeException("Failed to user space write file to channel", e);
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#rewritePayloadLength()
	 */
	@Override
	public void rewritePayloadLength() {
		try {
			log.debug("Rewriting Payload Length in file [{}] to [{}]", tmpFile, byteCount);
			MappedByteBuffer mbb = fileChannel.map(MapMode.READ_WRITE, lengthPosition, 8).load();
			mbb.put(encodeLittleEndianLongBytes(byteCount));
			mbb.force();
		} catch (Exception e) {
			throw new RuntimeException("Failed to rewritePayloadLength", e);
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#trimLastCharacter()
	 */
	@Override
	public void trimLastCharacter() {
		if(completedChecks>0) {
			try {
				log.debug("Trimming File Size [{}]", tmpFile);
				long prevSize = fileChannel.size(), postSize = 0L;
				fileChannel.position(prevSize-1);
				byteCount--;
				fileChannel.force(true);
				postSize = fileChannel.size();
				log.debug("Trimmed file size from [{}] to [{}]", prevSize, postSize);
			} catch (Exception e) {
				throw new RuntimeException("Failed to trimLastCharacter", e);
			}
		}		
	}

}
