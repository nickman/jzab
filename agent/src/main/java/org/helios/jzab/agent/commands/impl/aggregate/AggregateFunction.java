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
package org.helios.jzab.agent.commands.impl.aggregate;

/**
 * <p>Title: AggregateFunction</p>
 * <p>Description: Defines aggregate functions for aggregating the values of multiple attributes into one return value</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction</code></p>
 * TODO:<ol>
 * 	<li>GROUP (text):  Map of Key:Number of Occurences   e.g. ThreadState:Count</li>
 *  <li>MMAC (text):  Maximum / Minimum / Average / Count  all in one shot</li>
 * </ol>
 */

public enum AggregateFunction {
	/** Calculates the sum of the returned values */
	SUM,
	/** Returns the number of items in the result set */
	COUNT,
	/** Returns the average of the returned values */
	AVG,
	/** Returns the number of distinct items */
	DISTINCT,
	/** Returns the minimum value */
	MIN,
	/** Returns the maximum value */
	MAX;
	
	
	
	
}
