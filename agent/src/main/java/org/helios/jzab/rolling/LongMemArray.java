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
	protected int size = 0;
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
	/** The ID key for this LongMemArray */
	protected final String key;
	
	
	
	/**
	 * Creates a new LongMemArray
	 * @param name The name of this array
	 * @param range The number of minutes in the full range
	 * @param samples The number of samples taken per minute
	 * @param direct true for a direct buffer, false for a heap buffer
	 */
	public LongMemArray(String name, int range, int samples, boolean direct) {
		this.name = name;
		this.range = range;
		this.samples = samples;
		this.entryCount = range*samples;	
		this.key = name + range;
		buffer = direct ? ByteBuffer.allocateDirect(entryCount * getEntrySize()).asLongBuffer() : ByteBuffer.allocate(entryCount * getEntrySize()).asLongBuffer();
	}
	
	/**
	 * Creates a new LongMemArray from an existing LongMemArray but for a new range.
	 * The contents of the old LongMemArray are copied into the new one 
	 * @param range The new range
	 * @param lma The old LongMemArray to copy from
	 */
	public LongMemArray(int range, LongMemArray lma) {
		this(lma.name, range, lma.samples, lma.buffer.isDirect());
		buffer.put(lma.buffer);
		size = lma.size;
	}
		
	
	/**
	 * Inserts a new value to the rolling window
	 * @param value The value to add
	 */	
	public void add(long value) {
		synchronized(buffer) {
			if(size==entryCount) {
				buffer.position(1);
				buffer.compact();
			} else {
				size++;
			}
		}
		buffer.put(value);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#get()
	 */
	@Override
	public long[] get() {		
		long[] arr = new long[size];
		for(int i = 0; i < size; i++) {
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
		if(windowSize>size) windowSize = size;
		long[] arr = new long[windowSize];
		for(int i = 0; i < windowSize; i++) {
			arr[i] = buffer.get(i);
		}
		return arr;
		
	}
	
	/**
	 * Returns the ID key for this LMA
	 * @return the ID key
	 */
	@Override
	public String getKey() {
		return key;
	}		
	
	/**
	 * The name of this array
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.LongMemArrayMBean#getSize()
	 */
	@Override
	public int getSize() {
		return size;
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
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LongMemArray [");
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		builder.append("entryCount=");
		builder.append(entryCount);
		builder.append(", range=");
		builder.append(range);
		builder.append(", samples=");
		builder.append(samples);
		builder.append("]");
		return builder.toString();
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
