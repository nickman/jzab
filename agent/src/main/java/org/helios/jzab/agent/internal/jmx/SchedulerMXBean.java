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
package org.helios.jzab.agent.internal.jmx;

import javax.management.MXBean;

/**
 * <p>Title: SchedulerMXBean</p>
 * <p>Description: JMX interface for ScheduledThreadPoolFactories</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.internal.jmx.SchedulerMXBean</code></p>
 */
@MXBean
public interface SchedulerMXBean {
	/**
	 * Returns the approximate number of threads that are actively executing tasks.
	 * @return the approximate number of threads that are actively executing tasks.
	 */
	public int getActiveCount();
	
	/**
	 * Returns the core number of threads.
	 * @return the core number of threads.
	 */
	public int getCorePoolSize();
	
	/**
	 * Returns the maximum number of threads.
	 * @return the maximum number of threads.
	 */
	public int getMaximumPoolSize();
	
	
	/**
	 * Returns the largest number of threads that have ever simultaneously been in the pool.
	 * @return the largest number of threads that have ever simultaneously been in the pool.
	 */
	public int getLargestPoolSize(); 
	
	/**
	 * Returns the current number of threads in the pool.
	 * @return the current number of threads in the pool.
	 */
	public int getPoolSize();
	
	/**
	 * Returns the approximate total number of tasks that have completed execution.
	 * @return the approximate total number of tasks that have completed execution.
	 */
	public long getCompletedTaskCount();
	
	
	/**
	 * Returns true if this executor has been shut down.
	 * @return true if this executor has been shut down.
	 */
	public boolean isShutdown();
	
	/**
	 * Returns true if this executor has been terminated.
	 * @return true if this executor has been terminated.
	 */
	public boolean isTerminated();
	
	/**
	 * Returns true if this executor is terminating.
	 * @return true if this executor is terminating.
	 */
	public boolean isTerminating();
	
	

}
