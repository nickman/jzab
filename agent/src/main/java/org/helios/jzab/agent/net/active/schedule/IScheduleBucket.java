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
package org.helios.jzab.agent.net.active.schedule;

import java.util.Map;
import java.util.Set;

/**
 * <p>Title: IScheduleBucket</p>
 * <p>Description: Defines a base scheduling bucket</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.schedule.IScheduleBucket</code></p>
 * @param <T> The type of items managed by this bucket
 */
public interface IScheduleBucket<T> {
	/**
	 * Adds a new managed item
	 * @param delay The schedule delay for the item
	 * @param item The item to manage
	 */
	public void addItem(long delay, T item);
	
	/**
	 * Removes an item from scheduling
	 * @param delay The delay schedule bucket to remove the item from
	 * @param item the delay schedule to remove the item from
	 * @return true if the item was removed, false if it was not found
	 */
	public boolean removeItem(long delay, T item);
	
	/**
	 * Initates a scheduled task to execute the items in the passed schedule delay
	 * @param delay The schedule delay
	 */
	public abstract void fireStartScheduledEvent(long delay);
		
	
	
	/**
	 * Cancels a scheduled task for the passed schedule delay
	 * @param delay The schedule delay
	 */
	public abstract void fireCancelScheduledEvent(long delay);
	
	/**
	 * Returns the managed set of items with the passed delay
	 * @param delay The delay key
	 * @return a set of managed items which may be null if no items have that delay
	 */
	public Set<T> get(long delay);
	
	
	/**
	 * Returns an entry set of all the schedule entries
	 * @return an entry set of all the schedule entries
	 */
	public Set<Map.Entry<Long,Set<T>>> entrySet();
	
	/**
	 * Returns a set of all the schedule delay keys
	 * @return a set of all the schedule delay keys
	 */
	public Set<Long> keySet();
	
	/**
	 * Returns the number of entries
	 * @return the number of entries
	 */
	public int size();
		
	
}
