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


/**
 * <p>Title: PassiveScheduleBucket</p>
 * <p>Description: Passive schedule bucket that delegates events to the parent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.schedule.PassiveScheduleBucket</code></p>
 * @param <T> The type of items managed by this bucket
 * @param <E> The scoped instance type which is passed to the parent
 */
public class PassiveScheduleBucket<T, E> extends AbstractScheduleBucket<T,E> {
	/** The parent schedule bucket events are relayed up to */
	protected final IScheduleBucket<E> parentBucket;
	/** The instance of T that this instance will pass a reference when relaying events */
	protected final E scopedInstance;
	
	
	
	/**
	 * Creates a new PassiveScheduleBucket
	 * @param parentBucket The parent bucket that this bucket aggregates up to
	 * @param scopedInstance The instance that will be aggregated up to the parent
	 */
	public PassiveScheduleBucket(IScheduleBucket<E> parentBucket, E scopedInstance) {
		this.parentBucket = parentBucket;
		this.scopedInstance = scopedInstance;
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.schedule.IScheduleBucket#fireStartScheduledEvent(long)
	 */
	@Override
	public void fireStartScheduledEvent(long delay) {
		parentBucket.addItem(delay, scopedInstance);
		
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.schedule.IScheduleBucket#fireCancelScheduledEvent(long)
	 */
	@Override
	public void fireCancelScheduledEvent(long delay) {
		parentBucket.removeItem(delay, scopedInstance);
		
	}


}
