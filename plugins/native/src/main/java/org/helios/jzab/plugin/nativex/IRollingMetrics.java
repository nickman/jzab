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
package org.helios.jzab.plugin.nativex;

import javax.management.ObjectName;

import org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction;

/**
 * <p>Title: IRollingMetrics</p>
 * <p>Description: Convenience interface for the core's rolling metric service so an MBean invoker can be proxied out.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.IRollingMetrics</code></p>
 */
public interface IRollingMetrics {
	/**
	 * Registers a new rolling metric. The created metric array will allocate slots to accomodate
	 * the width of the window multiplied by the number of samples to collect within each minute.
	 * @param name The name of the metric collection
	 * @param range The window width in minutes
	 * @param samplesPerRange The number of samples to collect
	 * @param longCollector The ObjectName of the MBean providing the collection target
	 * @param opName The JMX operation name to invoke
	 * @param commandName The collection command name
	 * @param args The collection command arguments
	 * @return true if the collector was created, false if it already existed
	 */
	public boolean registerLongRollingMetric(String name, int range, int samplesPerRange, final ObjectName longCollector, final String opName, final String commandName, final String... args);

	/**
	 * Registers a new rolling metric. The created metric array will allocate slots to accomodate
	 * the width of the window multiplied by the number of samples to collect within each minute.
	 * @param name The name of the metric collection
	 * @param range The window width in minutes
	 * @param samplesPerRange The number of samples to collect
	 * @param doubleCollector The ObjectName of the MBean providing the collection target
	 * @param opName The JMX operation name to invoke
	 * @param commandName The collection command name
	 * @param args The collection command arguments
	 * @return true if the collector was created, false if it already existed
	 */
	public boolean registerDoubleRollingMetric(String name, int range, int samplesPerRange, final ObjectName doubleCollector, final String opName, final String commandName, final String... args);
	
	
	/**
	 * Returns the raw long array for the passed metric name
	 * @param name The metric name
	 * @param windowSize The number of minutes to retrieve for. <code>-1</code> means the whole raw array.
	 * @return the raw long array
	 */
	public long[] getLongArray(String name, int windowSize);
	
	/**
	 * Returns the raw long array for the passed metric name
	 * @param name The metric name
	 * @return the raw long array
	 */
	public long[] getLongArray(String name);
	
	/**
	 * Returns the raw double array for the passed metric name
	 * @param name The metric name
	 * @param windowSize The number of minutes to retrieve for. <code>-1</code> means the whole raw array.
	 * @return the raw double array
	 */
	public double[] getDoubleArray(String name, int windowSize);
	
	/**
	 * Returns the double long array for the passed metric name
	 * @param name The metric name
	 * @return the raw double array
	 */
	public double[] getDoubleArray(String name);
	
	/**
	 * Requests a rolling window evaluation
	 * @param name The metric name
	 * @param type The type of evaluation as defined in {@link AggregateFunction}
	 * @param windowSize The length of the window to evaluate (e.g. 1, 5 or 15 minutes)
	 * @return The calculated value
	 */
	public long getLongEvaluation(String name, String type, int windowSize);
	
	/**
	 * Requests a rolling window evaluation
	 * @param name The metric name
	 * @param type The type of evaluation as defined in {@link AggregateFunction}
	 * @param windowSize The length of the window to evaluate (e.g. 1, 5 or 15 minutes)
	 * @return The calculated value
	 */
	public double getDoubleEvaluation(String name, String type, int windowSize);	
	
	
}
