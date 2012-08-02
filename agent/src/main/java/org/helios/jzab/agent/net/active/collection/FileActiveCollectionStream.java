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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;

import org.helios.jzab.agent.util.FileDeletor;
import org.helios.jzab.agent.util.ReadableWritableByteChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

/**
 * <p>Title: FileActiveCollectionStream</p>
 * <p>Description: Traditional file IO based file collection stream</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.collection.FileActiveCollectionStream</code></p>
 */
public class FileActiveCollectionStream extends AbstractFileActiveCollectionStream {
	/** The temp file to stream results through */
	protected final File tmpFile;
	/** The temp file output stream */
	protected OutputStream os;

	/**
	 * Creates a new FileActiveCollectionStream
	 * @param buffer The collection buffer
	 * @param type The collection type
	 */
	public FileActiveCollectionStream(ReadableWritableByteChannelBuffer buffer, ActiveCollectionStreamType type) {
		super(buffer, type);
		try {
			tmpFile = File.createTempFile("jzab-coll", ".tmp");
			os = new BufferedOutputStream(new FileOutputStream(tmpFile));
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
			os.flush();
			return true;
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
			int bytes = (int) buffer.writeOutputStream(os, false);
			os.flush();
			buffer.reset();
			return bytes;
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
				FileDeletor.closeOnExit(os);			
				FileDeletor.deleteOnExit(tmpFile);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#writeFile(org.jboss.netty.channel.Channel)
	 */
	@Override
	protected ChannelFuture writeFile(Channel channel) {			
		try {
			channel.getPipeline().replace("responseEncoder", "chunkedFileEncoder", new ChunkedWriteHandler());
			return channel.write(new ChunkedFile(tmpFile));
		} catch (Exception e) {
			throw new RuntimeException("Failed to user space write file to channel", e);
		} finally {
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#rewritePayloadLength()
	 */
	@Override
	public void rewritePayloadLength() {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(tmpFile, "rw");
			raf.seek(lengthPosition);
			raf.write(encodeLittleEndianLongBytes(byteCount));			
		} catch (Exception e) {
			throw new RuntimeException("Failed to rewritePayloadLength on file [" + tmpFile + "]", e);
		} finally {
			if(raf!=null) try { raf.close(); } catch (Exception e) {}
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.AbstractFileActiveCollectionStream#trimLastCharacter()
	 */
	@Override
	public void trimLastCharacter() {
		RandomAccessFile raf = null;
		try {
			os.flush();
			os.close();
			raf = new RandomAccessFile(tmpFile, "rw");
			raf.setLength(raf.length()-1);
			byteCount--;
			os = new BufferedOutputStream(new FileOutputStream(tmpFile, true));
		} catch (Exception e) {
			throw new RuntimeException("Failed to trimLastCharacter on file [" + tmpFile + "]", e);
		} finally {
			if(raf!=null) try { raf.close(); } catch (Exception e) {}
		}
	}

}
