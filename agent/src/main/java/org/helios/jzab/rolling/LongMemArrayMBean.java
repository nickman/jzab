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

/**
 * <p>Title: LongMemArrayMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.rolling.LongMemArrayMBean</code></p>
 */
public interface LongMemArrayMBean {

	/**
	 * Returns an array of longs representing the window content so far
	 * @return an array of longs 
	 */
	public abstract long[] get();

	/**
	 * The number of currently occupied slots
	 * @return the number of currently occupied slots
	 */
	public abstract int getSize();

	/**
	 * Returns the average of all the values in the buffer
	 * @return the average of all the values in the buffer
	 */
	public abstract long avg();

	/**
	 * Returns the number of entry slots
	 * @return the number of entry slots
	 */
	public abstract int getEntryCount();

	/**
	 * Returns the size in bytes of one entry 
	 * @return the size in bytes of one entry
	 */
	public abstract int getEntrySize();

	/**
	 * Returns the range of the window in minutes
	 * @return the range of the window in minutes
	 */
	public abstract int getRange();

	/**
	 * The number of samples taken per minute
	 * @return the number of samples taken per minute
	 */
	public abstract int getSamples();
	
	/**
	 * The name of this array
	 * @return the name
	 */
	public String getName();

	/**
	 * Returns the ID key for this LMA
	 * @return the ID key
	 */
	public String getKey();
	
	/**
	 * Returns the last execution time in ns.
	 * @return the last execution time in ns.
	 */
	public long getLastExecution();
	
	/**
	 * Returns the last execution time in ms.
	 * @return the last execution time in ms.
	 */
	public long getLastExecutionMs();
	
	

}