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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <p>Title: AbstractScheduleBucket</p>
 * <p>Description: The base abstract class for schedule buckets</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.schedule.AbstractScheduleBucket</code></p>
 * @param <T> The type of items managed by this bucket
 * @param <E> The scoped instance type which is passed to the parent
 */
public abstract class AbstractScheduleBucket<T,E> implements IScheduleBucket<T> {
	/** Sets of managed items keyed by the delay of the schedule they have items in */
	protected final Map<Long, Set<T>> scheduleBucket = new ConcurrentHashMap<Long, Set<T>>();	

	
	/**
	 * Adds a new managed item
	 * @param delay The schedule delay for the item
	 * @param item The item to manage
	 */
	@Override
	public void addItem(long delay, T item) {
		Set<T> set = scheduleBucket.get(delay);
		if(set==null) {
			synchronized(scheduleBucket) {
				set = scheduleBucket.get(delay);
				if(set==null) {
					set = new CopyOnWriteArraySet<T>();
					set.add(item);
					scheduleBucket.put(delay, set);
					fireStartScheduledEvent(delay);
				}
			}
		} else {
			if(!set.contains(item)) {
				synchronized(set) {
					if(!set.contains(item)) {
						set.add(item);
					}
				}
			}			
		}
	}
	
	/**
	 * Removes an item from scheduling
	 * @param delay The delay schedule bucket to remove the item from
	 * @param item the delay schedule to remove the item from
	 * @return true if the item was removed, false if it was not found
	 */
	@Override
	public boolean removeItem(long delay, T item) {
		Set<T> set = scheduleBucket.get(delay);
		if(set==null) return false;
		if(set.remove(item)) {
			if(set.isEmpty()) {
				scheduleBucket.remove(delay);
				fireCancelScheduledEvent(delay);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Returns the managed set of items with the passed delay
	 * @param delay The delay key
	 * @return a set of managed items which may be null if no items have that delay
	 */
	@Override
	public Set<T> get(long delay) {
		return scheduleBucket.get(delay);
	}
	
	/**
	 * Returns an entry set of all the schedule entries
	 * @return an entry set of all the schedule entries
	 */
	@Override
	public Set<Map.Entry<Long,Set<T>>> entrySet() {
		return scheduleBucket.entrySet();
	}
	
	/**
	 * Returns a set of all the schedule delay keys
	 * @return a set of all the schedule delay keys
	 */
	@Override
	public Set<Long> keySet() {
		return scheduleBucket.keySet();
	}
	
	/**
	 * Returns the number of entries
	 * @return the number of entries
	 */
	@Override
	public int size() {
		return scheduleBucket.size();
	}
	
}
