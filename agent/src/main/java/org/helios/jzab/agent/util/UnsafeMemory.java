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
package org.helios.jzab.agent.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;

import sun.misc.Unsafe;

/**
 * <p>Title: UnsafeMemory</p>
 * <p>Description: Optimized byte array implementation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.util.UnsafeMemory</code></p>
 */
public class UnsafeMemory {
	/** The JVM's unsafe instance */
	private static final Unsafe unsafe;
	
	/** The direct byte buffer class */
	private static final Class<?> directByteBuffClass;
	/** The direct byte buffer's cleaner access method */
	private static final Method cleanerMethod;
	/** The direct byte buffer's cleaner clean method */
	private static final Method clean;
	
	
	static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        
        
        try {
        	directByteBuffClass = Class.forName("java.nio.DirectByteBuffer");
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        try {
        	cleanerMethod = directByteBuffClass.getDeclaredMethod("cleaner");
        	cleanerMethod.setAccessible(true);
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        try {
        	clean = cleanerMethod.getReturnType().getDeclaredMethod("clean");
        	clean.setAccessible(true);
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
	
	/** The array base offset for a byte array */
	public static final long byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
	/** The array base offset for a long array */
	public static final long longArrayOffset = unsafe.arrayBaseOffset(long[].class);
    /** The array base offset for a double array */
	public static final long doubleArrayOffset = unsafe.arrayBaseOffset(double[].class);
 
    /** The byte size of a boolean */
	public static final int SIZE_OF_BOOLEAN = 1;
    /** The byte size of an int */
	public static final int SIZE_OF_INT = 4;
    /** The byte size of a long */
	public static final int SIZE_OF_LONG = 8;
 
    /** The instance relative position */
    private int pos = 0;
    /** The unsafe allocated byte array */
    private final byte[] buffer;
    
    
    public static void unsafeDelete(Buffer buff) {
    	try {
	    	if(directByteBuffClass.isAssignableFrom(buff.getClass())) {
	    		Object cleaner = cleanerMethod.invoke(buff);
	    		if(cleaner!=null) {
	    			clean.invoke(cleaner);
	    		}
	    	}
    	} catch (Exception e) {
    		e.printStackTrace(System.err);
    		throw new RuntimeException(e);
    	}
    }
 	
    /**
     * Creates a new UnsafeMemory
     * @param buffer the buffer to allocate
     */
    public UnsafeMemory(final byte[] buffer) {
        if (null == buffer) {
            throw new NullPointerException("buffer cannot be null");
        } 
        this.buffer = buffer;
    }
    
    /**
     * Creates a new UnsafeMemory
     * @param size The size of the byte array to create
     */
    public UnsafeMemory(int size) {
    	this(new byte[size]);
    }
    
    /**
     * Resets the position
     */
    public void reset() {
        this.pos = 0;
    }
 
    /**
     * Writes a boolean and increments the position
     * @param value The value to write
     */
    public void putBoolean(final boolean value) {
        unsafe.putBoolean(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_BOOLEAN;
    }
 
    /**
     * Reads a boolean and increments the position
     * @return The read value
     */
    public boolean getBoolean() {
        boolean value = unsafe.getBoolean(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_BOOLEAN; 
        return value;
    }

    
    /**
     * Writes an int and increments the position
     * @param value The value to write
     */
    public void putInt(final int value) {
        unsafe.putInt(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_INT;
    }
 
    /**
     * Reads an int and increments the position
     * @return The read value
     */
    public int getInt() {
        int value = unsafe.getInt(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_INT; 
        return value;
    }
 
    /**
     * Writes a long and increments the position
     * @param value The value to write
     */
    public void putLong(final long value) {
        unsafe.putLong(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_LONG;
    }
 
    /**
     * Reads a long and increments the position
     * @return The read value
     */
    public long getLong() {
        long value = unsafe.getLong(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_LONG; 
        return value;
    }
 
    /**
     * Writes a long array and increments the position
     * @param value The value to write
     */
    public void putLongArray(final long[] values){
        putInt(values.length);
        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(values, longArrayOffset,
                          buffer, byteArrayOffset + pos,
                          bytesToCopy);
        pos += bytesToCopy;
    }
 
    /**
     * Reads a long array and increments the position
     * @return The read value
     */
    public long[] getLongArray() {
        int arraySize = getInt();
        long[] values = new long[arraySize];
 
        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(buffer, byteArrayOffset + pos,
                          values, longArrayOffset,
                          bytesToCopy);
        pos += bytesToCopy;
 
        return values;
    }
 
    /**
     * Writes a double and increments the position
     * @param value The value to write
     */
    public void putDoubleArray(final double[] values) {
        putInt(values.length);
 
        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(values, doubleArrayOffset,
                          buffer, byteArrayOffset + pos,
                          bytesToCopy);
        pos += bytesToCopy;
    }
 
    /**
     * Reads a double array and increments the position
     * @return The read value
     */
    public double[] getDoubleArray() {
        int arraySize = getInt();
        double[] values = new double[arraySize];
 
        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(buffer, byteArrayOffset + pos,
                          values, doubleArrayOffset,
                          bytesToCopy);
        pos += bytesToCopy;
 
        return values;
    }
    
    
    public static void main(String[] args) {
    	log("Unsafe test");
    	log("Page size:" + unsafe.pageSize());
    	log("Address size:" + unsafe.addressSize());
    	
    }
    
    public static void log(Object msg) {
    	System.out.println(msg);
    }
}
