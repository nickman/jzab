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
import java.nio.DoubleBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: DoubleMemArray</p>
 * <p>Description:  A sized double buffer for computing rolling aggregates</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.rolling.DoubleMemArray</code></p>
 */
public class DoubleMemArray implements DoubleMemArrayMBean {
	/** The name */
	protected final String name;
	/** The number of entries in the buffer */
	protected int size = 0;
	/** The number of entry slots in the buffer */
	protected final int entryCount;
	/** The slot buffer */
	protected final DoubleBuffer buffer;
	/** The size in bytes of one entry */
	protected final int entrySize = 8;
	/** The range of the window in minutes */
	protected final int range;
	/** The number of samples per minute */
	protected final int samples;
	/** The ID key for this DoubleMemArray */
	protected final String key;
	
	/**
	 * Creates a new DoubleMemArray
	 * @param name The name of this array
	 * @param range The number of minutes in the full range
	 * @param samples The number of samples taken per minute
	 * @param direct true for a direct buffer, false for a heap buffer
	 */
	public DoubleMemArray(String name, int range, int samples, boolean direct) {
		this.name = name;
		this.range = range;
		this.samples = samples;
		this.entryCount = range*samples;
		this.key = name + range;
		buffer = direct ? ByteBuffer.allocateDirect(entryCount * getEntrySize()).asDoubleBuffer() : ByteBuffer.allocate(entryCount * getEntrySize()).asDoubleBuffer();
	}
	
	/**
	 * Creates a new DoubleMemArray from an existing DoubleMemArray but for a new range.
	 * The contents of the old DoubleMemArray are copied into the new one 
	 * @param range The new range
	 * @param dma The old DoubleMemArray to copy from
	 */
	public DoubleMemArray(int range, DoubleMemArray dma) {
		this(dma.name, range, dma.samples, dma.buffer.isDirect());
		buffer.put(dma.buffer);
		size = dma.size;
	}
	
	
	
	/**
	 * Inserts a new value to the rolling window
	 * @param value The value to add
	 */
	public void add(double value) {
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
	 * @see org.helios.jzab.rolling.DoubleMemArrayMBean#get()
	 */
	@Override
	public double[] get() {
		
		double[] arr = new double[size];
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
	public double[] get(int windowSize) {
		windowSize = windowSize*samples;
		
		if(windowSize>size) windowSize = size;
		double[] arr = new double[windowSize];
		for(int i = 0; i < windowSize; i++) {
			arr[i] = buffer.get(i);
		}
		return arr;
		
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
	 * @see org.helios.jzab.rolling.DoubleMemArrayMBean#getSize()
	 */
	@Override
	public int getSize() {
		return size;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.DoubleMemArrayMBean#getRange()
	 */
	@Override
	public int getRange() {
		return range;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.DoubleMemArrayMBean#getSamples()
	 */
	@Override
	public int getSamples() {
		return samples;
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.DoubleMemArrayMBean#avg()
	 */
	@Override
	public double avg() {
		double[] arr = get();		
		double size = arr.length;
		if(size==0) return 0;
		double total = 0;
		for(int i = 0; i < size; i++) {
			total += arr[i];
		}
		return cavg(total, size);
	}
	
	/**
	 * Calculates the average
	 * @param total The total value
	 * @param count The number of values
	 * @return The average value
	 */
	public double cavg(double total, double count) {
		if(total==0 || count==0) {
			return 0;
		}
		double d = total/count;
		return d;
	}
	

	/**
	 * Returns the ID key for this DMA
	 * @return the ID key
	 */
	@Override
	public String getKey() {
		return key;
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.DoubleMemArrayMBean#getEntryCount()
	 */
	@Override
	public int getEntryCount() {
		return entryCount;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.DoubleMemArrayMBean#getEntrySize()
	 */
	@Override
	public int getEntrySize() {
		return entrySize;
	}
	
	public static void main(String[] args) {
		log("DoubleMemArrayTest");
		DoubleMemArray lma = new DoubleMemArray("Foo", 15, 4, true);
		Random r = new Random(System.nanoTime());
		int loops = 100000;
		int innerLoops = 5000;
		double spoof = Double.MIN_VALUE;
		
		for(int i = 0; i < loops; i++) {
			for(int x = 0; x < innerLoops; x++) {
				lma.add(Math.abs(r.nextInt(100)));
			}
			spoof += lma.avg();
			//log("Average:" + lma.avg());
		}
		lma = new DoubleMemArray(15, lma);
		spoof = Double.MIN_VALUE;
		long start = System.currentTimeMillis();
		for(int i = 0; i < loops; i++) {
			for(int x = 0; x < innerLoops; x++) {
				lma.add(Math.abs(r.nextInt(100)));
			}
			spoof += lma.avg();
			//log("Average:" + lma.avg());
		}
		
		long elapsed = System.currentTimeMillis()-start;
		log("With Avg Elaped: " + elapsed + " ms.");
		log("With Avg NS per:" + (TimeUnit.NANOSECONDS.convert(elapsed, TimeUnit.MILLISECONDS)/(loops*innerLoops)) + " ns.");
		lma = new DoubleMemArray(15, lma);
		spoof = Double.MIN_VALUE;
		start = System.currentTimeMillis();
		for(int i = 0; i < loops; i++) {
			for(int x = 0; x < innerLoops; x++) {
				lma.add(Math.abs(r.nextInt(100)));
			}
			//spoof += lma.avg();
			//log("Average:" + lma.avg());
		}
		
		elapsed = System.currentTimeMillis()-start;
		log("No Avg Elaped: " + elapsed + " ms.");
		log("No Avg NS per:" + (TimeUnit.NANOSECONDS.convert(elapsed, TimeUnit.MILLISECONDS)/(loops*innerLoops)) + " ns.");
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DoubleMemArray [");
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




}
