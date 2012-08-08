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
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: LongMemArray</p>
 * <p>Description: A sized long buffer for computing rolling aggregates</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.rolling.LongMemArray</code></p>
 */
public class LongMemArray implements LongMemArrayMBean {
	/** The name */
	protected final String name;
	/** The number of entries in the buffer */
	protected final AtomicIntCounter size;
	/** The number of entry slots in the buffer */
	protected final int entryCount;
	/** The slot buffer */
	protected final LongBuffer buffer;
	/** The size in bytes of one entry */
	protected final int entrySize = 8;
	/** The range of the window in minutes */
	protected final int range;
	/** The number of samples per minute */
	protected final int samples;
	
	
	/**
	 * Creates a new LongMemArray
	 * @param name The name of this array
	 * @param entryCount The number of entries
	 * @param direct true for a direct buffer, false for a heap buffer
	 */
	public LongMemArray(String name, int range, int samples, boolean direct) {
		this.name = name;
		this.range = range;
		this.samples = samples;
		this.entryCount = range*samples;
		this.size = new AtomicIntCounter(entryCount);
		buffer = direct ? ByteBuffer.allocateDirect(entryCount * getEntrySize()).asLongBuffer() : ByteBuffer.allocate(entryCount * getEntrySize()).asLongBuffer();
	}
	
	/**
	 * Inserts a new value to the rolling window
	 * @param value The value to add
	 */	
	public synchronized void add(long value) {
		int sz = size.tick();
		if(sz==entryCount) {
			buffer.position(1);
			buffer.compact();
		} 
		buffer.put(value);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#get()
	 */
	@Override
	public long[] get() {
		int sz = size.get();
		long[] arr = new long[sz];
		for(int i = 0; i < sz; i++) {
			arr[i] = buffer.get(i);
		}
		return arr;
	}
	
	/**
	 * Returns the item entries within the specified minute based window.
	 * If the window size is bigger than the existing array, the whole array is retrieved.
	 * @param windowSize The minute window to retrieve
	 * @return the item entries within the specified minute based window
	 */
	public long[] get(int windowSize) {
		windowSize = windowSize*samples;
		int sz = size.get();
		if(windowSize>sz) windowSize = sz;
		long[] arr = new long[windowSize];
		for(int i = 0; i < windowSize; i++) {
			arr[i] = buffer.get(i);
		}
		return arr;
		
	}
	
	/**
	 * The name of this array
	 * @return the name
	 */
	public String getName() {
		return name;
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#getSize()
	 */
	@Override
	public int getSize() {
		return size.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#avg()
	 */	
	@Override
	public long avg() {
		long[] arr = get();		
		long size = arr.length;
		if(size==0) return 0;
		long total = 0;
		for(int i = 0; i < size; i++) {
			total += arr[i];
		}
		return cavg(total, size);
	}
	
	public long cavg(double total, double count) {
		if(total==0 || count==0) {
			return 0;
		}
		double d = total/count;
		return (long)d;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#getEntryCount()
	 */
	@Override
	public int getEntryCount() {
		return entryCount;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#getEntrySize()
	 */
	@Override
	public int getEntrySize() {
		return entrySize;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#getRange()
	 */
	@Override
	public int getRange() {
		return range;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#getSamples()
	 */
	@Override
	public int getSamples() {
		return samples;
	}	
		
	
	public static void main(String[] args) {
		log("LongMemArrayTest");
		LongMemArray lma = new LongMemArray("Foo", 15, 4, true);
		Random r = new Random(System.nanoTime());
		int loops = 10000;
		long spoof = Long.MIN_VALUE;
		
		for(int i = 0; i < loops; i++) {
			for(int x = 0; x < 5000; x++) {
				lma.add(Math.abs(r.nextInt(100)));
			}
			spoof += lma.avg();
			//log("Average:" + lma.avg());
		}
		lma = new LongMemArray("Foo", 15, 4, true);
		spoof = Long.MIN_VALUE;
		long start = System.currentTimeMillis();
		for(int i = 0; i < loops; i++) {
			for(int x = 0; x < 5000; x++) {
				lma.add(Math.abs(r.nextInt(100)));
			}
			spoof += lma.avg();
			//log("Average:" + lma.avg());
		}
		
		long elapsed = System.currentTimeMillis()-start;
		log("Elaped: " + elapsed + " ms.");
		log("NS per:" + (TimeUnit.NANOSECONDS.convert(elapsed, TimeUnit.MILLISECONDS)/loops) + " ns.");
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
