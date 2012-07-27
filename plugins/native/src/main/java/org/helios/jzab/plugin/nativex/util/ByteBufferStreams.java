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
package org.helios.jzab.plugin.nativex.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * <p>Title: ByteBufferStreams</p>
 * <p>Description: Static byte buffer and stream utilities</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.util.ByteBufferStreams</code></p>
 */
public class ByteBufferStreams implements ReadableByteChannel, WritableByteChannel {
	/** The internal byte buffer */
	protected final ByteBuffer buffer;
	
	/**
	 * Creates a new direct buffer
	 * @param size The buffer size
	 * @return the new buffer
	 */
	public static ByteBufferStreams newDirectBuffer(int size) {
		return new ByteBufferStreams(ByteBuffer.allocateDirect(size));
	}

	
	/**
	 * Creates a new heap buffer
	 * @param size The buffer size
	 * @return the new buffer
	 */
	public static ByteBufferStreams newBuffer(int size) {
		return new ByteBufferStreams(ByteBuffer.allocate(size));
	}
	
	/**
	 * Creates a new heap buffer with a size of the available bytes in the passed input stream, then reads in that many bytes.
	 * @param is The input stream to read
	 * @return The created buffer
	 */
	public static ByteBufferStreams readInputStream(InputStream is) {
		try {
			int size = is.available();
			ByteBufferStreams b = newBuffer(size);
			byte[] buf = new byte[size];
			is.read(buf);
			b.buffer.put(buf);
			return b;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create buffer and read input stream", e);
		}
	}

	/**
	 * Creates a new direct buffer with a size of the available bytes in the passed input stream, then reads in that many bytes.
	 * @param is The input stream to read
	 * @return The created buffer
	 */
	public static ByteBufferStreams readInputStreamDirect(InputStream is) {
		try {
			int size = is.available();
			ByteBufferStreams b = newDirectBuffer(size);
			byte[] buf = new byte[size];
			is.read(buf);
			b.buffer.put(buf);
			return b;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create buffer and read input stream", e);
		}
	}
	
	
	private ByteBufferStreams(ByteBuffer buffer) {
		this.buffer = buffer;
	}
	
	/**
	 * Returns a <code>/dev/null</code> output stream
	 * @return an output stream that accepts bytes and drops them.
	 */
	public static OutputStream nullOutputStream() {
		return new OutputStream() {
			/**
			 * No Op
			 * {@inheritDoc}
			 * @see java.io.OutputStream#write(int)
			 */
			@Override
			public void write(int b) throws IOException {
			}
		};
	}
	

	/**
	 * Returns a <code>/dev/null</code> print stream
	 * @return a print stream that accepts bytes and drops them.
	 */
	public static PrintStream nullPrintStream() {
		return new PrintStream(nullOutputStream());
	}
	


	/**
	 * Always returns true
	 * {@inheritDoc}
	 * @see java.nio.channels.Channel#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return true;
	}


	/**
	 * No Op
	 * {@inheritDoc}
	 * @see java.nio.channels.Channel#close()
	 */
	@Override
	public void close() throws IOException {		
	}


	/**
	 * Writes a sequence of bytes to this buffer from the given buffer.
	 * Used by transferTo.
	 * {@inheritDoc}
	 * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
	 */
	@Override
	public int write(ByteBuffer src) throws IOException {
		buffer.flip();
		int pos = buffer.position();
		buffer.put(src);
		return buffer.position()-pos;
	}


	/**
	 * Reads a sequence of bytes from this buffer into the given buffer.
	 * Used by transferFrom.
	 * {@inheritDoc}
	 * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
	 */
	@Override
	public int read(ByteBuffer dst) throws IOException {		
		int pos = dst.position();
		dst.put(buffer);
		return dst.position()-pos;
	}

	
	public ByteBuffer getBuffer() {
		return buffer;
	}

	/**
	 * @return
	 * @see java.nio.Buffer#capacity()
	 */
	public final int capacity() {
		return buffer.capacity();
	}


	/**
	 * @return
	 * @see java.nio.Buffer#position()
	 */
	public final int position() {
		return buffer.position();
	}


	/**
	 * @param newPosition
	 * @return
	 * @see java.nio.Buffer#position(int)
	 */
	public final Buffer position(int newPosition) {
		return buffer.position(newPosition);
	}


	/**
	 * @return
	 * @see java.nio.Buffer#limit()
	 */
	public final int limit() {
		return buffer.limit();
	}


	/**
	 * @param newLimit
	 * @return
	 * @see java.nio.Buffer#limit(int)
	 */
	public final Buffer limit(int newLimit) {
		return buffer.limit(newLimit);
	}


	/**
	 * @return
	 * @see java.nio.Buffer#mark()
	 */
	public final Buffer mark() {
		return buffer.mark();
	}


	/**
	 * @return
	 * @see java.nio.Buffer#reset()
	 */
	public final Buffer reset() {
		return buffer.reset();
	}


	/**
	 * @return
	 * @see java.nio.Buffer#clear()
	 */
	public final Buffer clear() {
		return buffer.clear();
	}


	/**
	 * @return
	 * @see java.nio.Buffer#flip()
	 */
	public final Buffer flip() {
		return buffer.flip();
	}


	/**
	 * @return
	 * @see java.nio.Buffer#rewind()
	 */
	public final Buffer rewind() {
		return buffer.rewind();
	}


	/**
	 * @return
	 * @see java.nio.Buffer#remaining()
	 */
	public final int remaining() {
		return buffer.remaining();
	}


	/**
	 * @return
	 * @see java.nio.Buffer#hasRemaining()
	 */
	public final boolean hasRemaining() {
		return buffer.hasRemaining();
	}


	/**
	 * @return
	 * @see java.nio.Buffer#isReadOnly()
	 */
	public boolean isReadOnly() {
		return buffer.isReadOnly();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#slice()
	 */
	public ByteBuffer slice() {
		return buffer.slice();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#duplicate()
	 */
	public ByteBuffer duplicate() {
		return buffer.duplicate();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#asReadOnlyBuffer()
	 */
	public ByteBuffer asReadOnlyBuffer() {
		return buffer.asReadOnlyBuffer();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#get()
	 */
	public byte get() {
		return buffer.get();
	}


	/**
	 * @param b
	 * @return
	 * @see java.nio.ByteBuffer#put(byte)
	 */
	public ByteBuffer put(byte b) {
		return buffer.put(b);
	}


	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#get(int)
	 */
	public byte get(int index) {
		return buffer.get(index);
	}


	/**
	 * @param index
	 * @param b
	 * @return
	 * @see java.nio.ByteBuffer#put(int, byte)
	 */
	public ByteBuffer put(int index, byte b) {
		return buffer.put(index, b);
	}


	/**
	 * @param dst
	 * @param offset
	 * @param length
	 * @return
	 * @see java.nio.ByteBuffer#get(byte[], int, int)
	 */
	public ByteBuffer get(byte[] dst, int offset, int length) {
		return buffer.get(dst, offset, length);
	}


	/**
	 * @param dst
	 * @return
	 * @see java.nio.ByteBuffer#get(byte[])
	 */
	public ByteBuffer get(byte[] dst) {
		return buffer.get(dst);
	}


	/**
	 * @param src
	 * @return
	 * @see java.nio.ByteBuffer#put(java.nio.ByteBuffer)
	 */
	public ByteBuffer put(ByteBuffer src) {
		return buffer.put(src);
	}


	/**
	 * @param src
	 * @param offset
	 * @param length
	 * @return
	 * @see java.nio.ByteBuffer#put(byte[], int, int)
	 */
	public ByteBuffer put(byte[] src, int offset, int length) {
		return buffer.put(src, offset, length);
	}


	/**
	 * @param src
	 * @return
	 * @see java.nio.ByteBuffer#put(byte[])
	 */
	public final ByteBuffer put(byte[] src) {
		return buffer.put(src);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#hasArray()
	 */
	public final boolean hasArray() {
		return buffer.hasArray();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#array()
	 */
	public final byte[] array() {
		return buffer.array();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#arrayOffset()
	 */
	public final int arrayOffset() {
		return buffer.arrayOffset();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#compact()
	 */
	public ByteBuffer compact() {
		return buffer.compact();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#isDirect()
	 */
	public boolean isDirect() {
		return buffer.isDirect();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#toString()
	 */
	public String toString() {
		return buffer.toString();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#hashCode()
	 */
	public int hashCode() {
		return buffer.hashCode();
	}


	/**
	 * @param ob
	 * @return
	 * @see java.nio.ByteBuffer#equals(java.lang.Object)
	 */
	public boolean equals(Object ob) {
		return buffer.equals(ob);
	}


	/**
	 * @param that
	 * @return
	 * @see java.nio.ByteBuffer#compareTo(java.nio.ByteBuffer)
	 */
	public int compareTo(ByteBuffer that) {
		return buffer.compareTo(that);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#order()
	 */
	public final ByteOrder order() {
		return buffer.order();
	}


	/**
	 * @param bo
	 * @return
	 * @see java.nio.ByteBuffer#order(java.nio.ByteOrder)
	 */
	public final ByteBuffer order(ByteOrder bo) {
		return buffer.order(bo);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#getChar()
	 */
	public char getChar() {
		return buffer.getChar();
	}


	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putChar(char)
	 */
	public ByteBuffer putChar(char value) {
		return buffer.putChar(value);
	}


	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getChar(int)
	 */
	public char getChar(int index) {
		return buffer.getChar(index);
	}


	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putChar(int, char)
	 */
	public ByteBuffer putChar(int index, char value) {
		return buffer.putChar(index, value);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#asCharBuffer()
	 */
	public CharBuffer asCharBuffer() {
		return buffer.asCharBuffer();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#getShort()
	 */
	public short getShort() {
		return buffer.getShort();
	}


	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putShort(short)
	 */
	public ByteBuffer putShort(short value) {
		return buffer.putShort(value);
	}


	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getShort(int)
	 */
	public short getShort(int index) {
		return buffer.getShort(index);
	}


	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putShort(int, short)
	 */
	public ByteBuffer putShort(int index, short value) {
		return buffer.putShort(index, value);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#asShortBuffer()
	 */
	public ShortBuffer asShortBuffer() {
		return buffer.asShortBuffer();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#getInt()
	 */
	public int getInt() {
		return buffer.getInt();
	}


	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putInt(int)
	 */
	public ByteBuffer putInt(int value) {
		return buffer.putInt(value);
	}


	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getInt(int)
	 */
	public int getInt(int index) {
		return buffer.getInt(index);
	}


	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putInt(int, int)
	 */
	public ByteBuffer putInt(int index, int value) {
		return buffer.putInt(index, value);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#asIntBuffer()
	 */
	public IntBuffer asIntBuffer() {
		return buffer.asIntBuffer();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#getLong()
	 */
	public long getLong() {
		return buffer.getLong();
	}


	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putLong(long)
	 */
	public ByteBuffer putLong(long value) {
		return buffer.putLong(value);
	}


	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getLong(int)
	 */
	public long getLong(int index) {
		return buffer.getLong(index);
	}


	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putLong(int, long)
	 */
	public ByteBuffer putLong(int index, long value) {
		return buffer.putLong(index, value);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#asLongBuffer()
	 */
	public LongBuffer asLongBuffer() {
		return buffer.asLongBuffer();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#getFloat()
	 */
	public float getFloat() {
		return buffer.getFloat();
	}


	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putFloat(float)
	 */
	public ByteBuffer putFloat(float value) {
		return buffer.putFloat(value);
	}


	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getFloat(int)
	 */
	public float getFloat(int index) {
		return buffer.getFloat(index);
	}


	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putFloat(int, float)
	 */
	public ByteBuffer putFloat(int index, float value) {
		return buffer.putFloat(index, value);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#asFloatBuffer()
	 */
	public FloatBuffer asFloatBuffer() {
		return buffer.asFloatBuffer();
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#getDouble()
	 */
	public double getDouble() {
		return buffer.getDouble();
	}


	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putDouble(double)
	 */
	public ByteBuffer putDouble(double value) {
		return buffer.putDouble(value);
	}


	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getDouble(int)
	 */
	public double getDouble(int index) {
		return buffer.getDouble(index);
	}


	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putDouble(int, double)
	 */
	public ByteBuffer putDouble(int index, double value) {
		return buffer.putDouble(index, value);
	}


	/**
	 * @return
	 * @see java.nio.ByteBuffer#asDoubleBuffer()
	 */
	public DoubleBuffer asDoubleBuffer() {
		return buffer.asDoubleBuffer();
	}
}
