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
package org.helios.jzab.agent.net.active;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ScheduleBucket</p>
 * <p>Description: Manages a schedule bucket map where sets of instances of the target type are bucketed by the delay of the scheduled event</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ScheduleBucket</code></p>
 * @param <T> The expected type of the scheduled items
 */

public class ScheduleBucket<T> {
	/** Instance logger */
	protected final Logger log; // = LoggerFactory.getLogger(getClass());
	
	/** Sets of managed items keyed by the delay of the schedule they have items in */
	protected final Map<Long, Set<T>> scheduleBucket = new ConcurrentHashMap<Long, Set<T>>();

	/**
	 * Creates a new ScheduleBucket
	 * @param managedType The type of the items managed by this instance
	 */
	public ScheduleBucket(Class<T> managedType) {
		log = LoggerFactory.getLogger(getClass().getName() + "-" + managedType.getSimpleName());
	}
	
	/**
	 * Adds a new managed item
	 * @param delay The schedule delay for the item
	 * @param item The item to manage
	 */
	public void addItem(long delay, T item) {
		Set<T> set = scheduleBucket.get(delay);
		if(set==null) {
			synchronized(scheduleBucket) {
				set = scheduleBucket.get(delay);
				if(set==null) {
					set = new CopyOnWriteArraySet<T>();
					set.add(item);
					scheduleBucket.put(delay, set);
					// fire start schedule event
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
	public boolean removeItem(long delay, T item) {
		Set<T> set = scheduleBucket.get(delay);
		if(set==null) return false;
		if(set.remove(item)) {
			if(set.isEmpty()) {
				scheduleBucket.remove(delay);
				// fire cancel schedule event
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Removes a managed item from all schedule delays that it is registered in
	 * @param item The item to remove
	 * @return a set of the delays that were modified as a result of this operation.
	 */
	public Set<Long> removeItem(T item) {
		Set<Long> modifiedDelays = new HashSet<Long>();
		Iterator<Map.Entry<Long, Set<T>>> iter = scheduleBucket.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<Long, Set<T>> entry = iter.next();
			if(entry.getValue().remove(item)) {
				modifiedDelays.add(entry.getKey());
				if(entry.getValue().isEmpty()) {
					iter.remove();
					// fire cancel schedule event
				}
			}
		}
		return modifiedDelays;
	}
	

}
