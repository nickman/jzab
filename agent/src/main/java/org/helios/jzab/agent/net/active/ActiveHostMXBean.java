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

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.helios.jzab.agent.internal.jmx.TrackedScheduledFuture;
import org.helios.jzab.agent.net.active.ActiveHost.LastRefreshChange;

/**
 * <p>Title: ActiveHostMXBean</p>
 * <p>Description: JMX interface for ActiveHost</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ActiveHostMXBean</code></p>
 */
public interface ActiveHostMXBean {
	/**
	 * Returns the logging level for this active host
	 * @return the logging level for this active host
	 */
	public String getLevel();
	
	/**
	 * Sets the logger level for this active host
	 * @param level The level to set this logger to
	 */
	public void setLevel(String level);
	
	/**
	 * Returns a map of the number of checks registered for each delay
	 * @return a map of the number of checks registered for each delay
	 */
	public Map<Long, Integer> getScheduleCounts();
	
	/**
	 * Returns the state of this host
	 * @return the state of this host
	 */
	public String getState();	
	
	/**
	 * Returns an array of the unique schedule windows for this active host's checks
	 * @return an array of longs representing the unique delays for this active host's checks
	 */
	public long[] getDistinctSchedules();
	
	/**
	 * Returns the number of active checks for this host
	 * @return the number of active checks for this host
	 */
	public int getActiveCheckCount();
	
	/**
	 * Returns the effective time of the last state change in seconds
	 * @return the effective time of the last state change in seconds
	 */
	public long getStateTimestamp();
	
	/**
	 * Returns the effective time of the last state change as a java date
	 * @return the effective time of the last state change as a java date
	 */
	public Date getStateDate();
	
	/**
	 * Returns the refresh period in seconds.
	 * @return the refresh period in seconds.
	 */
	public long getRefreshPeriod();



	/**
	 * Sets the refresh period in seconds
	 * @param refreshPeriod the refresh period in seconds
	 */
	public void setRefreshPeriod(long refreshPeriod);



	/**
	 * Return the host name
	 * @return the hostName
	 */
	public String getHostName();

	/**
	 * Returns the ID of this active host
	 * @return the ID of this active host
	 */
	public String getId();
	
	/**
	 * Executes all the active checks for this host and submits them
	 */
	public void executeChecks();
	
	/**
	 * Requests an updates on active checks assigned to this host (forced)
	 */
	public void requestMarchingOrders();
	
	/**
	 * Returns the current pending refresh task
	 * @return the current pending refresh task
	 */
	public TrackedScheduledFuture getRefreshTask();
	
	/**
	 * Returns the changes made in the last refresh 
	 * @return the changes made in the last refresh
	 */
	public LastRefreshChange getLastRefreshChange();	
	
	/**
	 * Returns a set of the names of removed checks
	 * @return the removed Check Names
	 */
	public Set<String> getRemovedCheckNames();
	
	/**
	 * Clears the removed check names
	 */
	public void clearRemovedCheckNames();	
	

}
