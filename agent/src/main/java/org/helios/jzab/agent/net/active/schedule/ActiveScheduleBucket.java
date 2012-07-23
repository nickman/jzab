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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ActiveScheduleBucket</p>
 * <p>Description: Manages a schedule bucket map where sets of instances of the target type are bucketed by the delay of the scheduled event.
 * A ActiveScheduleBucket can be either <b>Passive</b> or <b>Active</b>:<ul>
 * 	<li><b>Passive<b>: Add and Cancel events are delegated up to the parent ActiveScheduleBucket</li>
 *  <li><b>Active<b>: Add and Cancel events result in the creation or deletion of scheduled events</li>
 * </ul></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ScheduleBucket</code></p>
 * @param <T> The expected type of the scheduled items
 */

public class ActiveScheduleBucket<T,E> extends AbstractScheduleBucket<T,E> {
	/** Instance logger */
	protected final Logger log; 
	/** The scheduler */
	protected final ScheduledThreadPoolExecutor scheduler;
	/** The threading policy for this collection */
	protected final CommandThreadPolicy threadPolicy;
	/** Indicates if in-memory or disk-based result collation is being used */
	protected final boolean inMemoryCollation;
	/** The task execution thread pool */
	protected final ThreadPoolExecutor taskThreadPool;
	
	
	/** A map of scheduled task handles keyed by the scheduled delay */
	protected final Map<Long, ScheduledFuture<?>> scheduleHandles = new ConcurrentHashMap<Long, ScheduledFuture<?>>();

	
	
	/**
	 * Creates a new ActiveScheduleBucket
	 * @param scheduler The scheduler that manages execution schedules
	 * @param threadPolicy The threading policy for this collection
	 * @param inMemoryCollation Indicates if in-memory or disk-based result collation is being used
	 * @param taskThreadPool The task execution thread pool 
	 * @param managedType The type of the items managed by this instance
	 */
	public ActiveScheduleBucket(ScheduledThreadPoolExecutor scheduler,
			CommandThreadPolicy threadPolicy, 
			boolean inMemoryCollation, ThreadPoolExecutor taskThreadPool, Class<T> managedType) {
		this.scheduler = scheduler;
		this.threadPolicy = threadPolicy;
		this.inMemoryCollation = inMemoryCollation;
		this.taskThreadPool = taskThreadPool;
		log = LoggerFactory.getLogger(getClass().getName() + "-Active-" + managedType.getSimpleName());
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
					fireCancelScheduledEvent(entry.getKey());
				}
			}
		}
		return modifiedDelays;
	}
	
	/**
	 * Initates a scheduled task to execute the items in the passed schedule delay
	 * @param delay The schedule delay
	 */
	public void fireStartScheduledEvent(long delay) {
		
	}
	
	/**
	 * Cancels a scheduled task for the passed schedule delay
	 * @param delay The schedule delay
	 */
	public void fireCancelScheduledEvent(long delay) {
		ScheduledFuture<?> handle = scheduleHandles.remove(delay);
		if(handle!=null) {
			handle.cancel(true);
		}
	}
	
	

}
