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
package org.helios.jzab.rolling;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

/**
 * <p>Title: LongMemArray</p>
 * <p>Description: A sized long buffer for computing rolling aggregates</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.rolling.LongMemArray</code></p>
 */
public class LongMemArray {
	/** The number of entries in the buffer */
	protected final AtomicIntCounter size;
	/** The number of entry slots in the buffer */
	protected final int entryCount;
	/** The slot buffer */
	protected final ByteBuffer buffer;
	/** The size in bytes of one entry */
	protected final int entrySize = 8;
	
	/**
	 * Creates a new LongMemArray
	 * @param entryCount The number of entries
	 * @param direct true for a direct buffer, false for a heap buffer
	 */
	public LongMemArray(int entryCount, boolean direct) {
		this.entryCount = entryCount;
		this.size = new AtomicIntCounter(this.entryCount);
		buffer = direct ? ByteBuffer.allocateDirect(entryCount * getEntrySize()) : ByteBuffer.allocate(entryCount * getEntrySize());
	}
	
	public void add(long value) {
		int sz = size.tick();
		if(sz==entryCount) {
			buffer.position(entrySize);
			buffer.compact();			
		} 
		buffer.putLong((sz-1)*entrySize, value);		
	}
	
	
	public long[] get() {
		int sz = size.get();
		long[] arr = new long[sz];
		for(int i = 0; i < sz; i++) {
			arr[i] = buffer.asLongBuffer().get(i);
		}
		return arr;
	}

	/**
	 * The number of currently occupied slots
	 * @return the number of currently occupied slots
	 */
	public int getSize() {
		return size.get();
	}

	/**
	 * Returns the number of entyr slots
	 * @return the number of entyr slots
	 */
	public int getEntryCount() {
		return entryCount;
	}

	/**
	 * Returns the size in bytes of one entry 
	 * @return the size in bytes of one entry
	 */
	public int getEntrySize() {
		return entrySize;
	}
	
	public static void main(String[] args) {
		log("LongMemArrayTest");
		LongMemArray lma = new LongMemArray(15, true);
		for(long a = 1L; a < 100; a++) {
			lma.add(a);
			if(a%10==0) {
				log("Arr:" + Arrays.toString(lma.get()));
			}
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
