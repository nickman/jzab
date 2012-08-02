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
package org.helios.jzab.agent.commands.instrumentation;

import java.util.Date;

/**
 * <p>Title: ExecutionMetricMBean</p>
 * <p>Description: Defines an execution metric for command processors </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean</code></p>
 */
public interface ExecutionMetricMBean {
	/**
	 * Returns the name of the metric
	 * @return the name of the metric
	 */
	public String getName();
	
	/**
	 * Returns the last date/time this metric was executed
	 * @return the last date/time this metric was executed
	 */
	public Date getLastExecutionDate();
	
	/**
	 * Returns the elapsed time of the last execution in ms.
	 * @return the elapsed time of the last execution in ms.
	 */
	public long getLastExecutionElapsed();
	
	/**
	 * Returns the total cummulative number of executions
	 * @return the total cummulative number of executions
	 */
	public long getExecutionCount();
	
	/**
	 * Returns the elapsed time of the last <i>n</i> execution in ms.
	 * @return the elapsed time of the last <i>n</i> execution in ms.
	 */
	public long getAvgExecutionElapsed();
	
	/**
	 * Returns the last <i>n</i> execution times
	 * @return the last <i>n</i> execution times
	 */
	public long[] getLastNElapsed();
}
