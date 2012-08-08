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
package org.helios.jzab.rolling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction;
import org.helios.jzab.agent.internal.jmx.ScheduledThreadPoolFactory;
import org.helios.jzab.agent.internal.jmx.TrackedScheduledFuture;
import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: RollingMetricService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.rolling.RollingMetricService</code></p>
 */

public class RollingMetricService implements RollingMetricServiceMXBean {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** The singleton instance */
	private static volatile RollingMetricService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** A map of double mem arrays keyed by metric name */
	protected final Map<String, DoubleMemArray> doubleArrays = new ConcurrentHashMap<String, DoubleMemArray>();
	/** A map of long mem arrays keyed by metric name */
	protected final Map<String, LongMemArray> longArrays = new ConcurrentHashMap<String, LongMemArray>();
	/** A map of double collector tasks keyed by metric name */
	protected final Map<String, TrackedScheduledFuture> doubleCollectors = new ConcurrentHashMap<String, TrackedScheduledFuture>();
	/** A map of long collector tasks keyed by metric name */
	protected final Map<String, TrackedScheduledFuture> longCollectors = new ConcurrentHashMap<String, TrackedScheduledFuture>();
	
	/** The service's JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.rolling:service=WeAreRolling");
	/** THe JMX invocation signature for JMX based collection callbacks */
	protected static final String[] COLLECT_SIGNATURE = new String[]{String.class.getName(), new String[0].getClass().getName()}; 
	
	/**
	 * Returns the RollingMetricService singleton instance
	 * @return the RollingMetricService singleton instance
	 */
	public static RollingMetricService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RollingMetricService();
				}
			}
		}
		return instance;
		
	}
	
	/**
	 * Creates a new RollingMetricService and registers the JMX interface
	 */
	private RollingMetricService() {
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), OBJECT_NAME, this);
		log.info("Started RollingMetricService");
	}
	
	/**
	 * Registers a new rolling metric. The created metric array will allocate slots to accomodate
	 * the width of the window multiplied by the number of samples to collect within each minute.
	 * @param name The name of the metric collection
	 * @param range The window width in minutes
	 * @param samplesPerRange The number of samples to collect
	 * @param longCollector The schedulable task that will collect the samples
	 * @return true if the collector was created, false if it already existed
	 */
	public boolean registerLongRollingMetric(final String name, int range, int samplesPerRange, final Callable<Long> longCollector) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		if(longCollector==null) throw new IllegalArgumentException("The passed collector was null", new Throwable());
		log.debug("Creating LongRollingMetric for [{}]", name);
		LongMemArray lma = longArrays.get(name);
		boolean exists = true;
		if(lma==null) {
			synchronized(longArrays) {
				lma = longArrays.get(name);
				if(lma==null) {
					lma = new LongMemArray(name, range, samplesPerRange, true);
					longArrays.put(name, lma);
					final LongMemArray finalMemArr = lma;
					Callable<Long> task = new Callable<Long>() {
						@Override
						public Long call() throws Exception {
							long val = longCollector.call();
							finalMemArr.add(val);
							log.debug("Added [{}] to LongRollingMetric [{}]", val, name);
							return val;
						}
					};
					longCollectors.put(name, ScheduledThreadPoolFactory.getInstance("Scheduler").scheduleAtFixedRate("Rollng Collection [" + name + "]", new FutureTask<Long>(task), 0, 60/samplesPerRange, TimeUnit.SECONDS));
					exists = false;
				}
			}
		}
		return exists;
	}
	
	/**
	 * Registers a new rolling metric. The created metric array will allocate slots to accomodate
	 * the width of the window multiplied by the number of samples to collect within each minute.
	 * @param name The name of the metric collection
	 * @param range The window width in minutes
	 * @param samplesPerRange The number of samples to collect
	 * @param doubleCollector The schedulable task that will collect the samples
	 * @return true if the collector was created, false if it already existed
	 */
	public boolean registerDoubleRollingMetric(final String name, int range, int samplesPerRange, final Callable<Double> doubleCollector) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		if(doubleCollector==null) throw new IllegalArgumentException("The passed collector was null", new Throwable());
		log.debug("Creating DoubleRollingMetric for [{}]", name);
		DoubleMemArray lma = doubleArrays.get(name);
		boolean exists = true;
		if(lma==null) {
			synchronized(doubleArrays) {
				lma = doubleArrays.get(name);
				if(lma==null) {
					lma = new DoubleMemArray(name, range, samplesPerRange, true);
					doubleArrays.put(name, lma);
					final DoubleMemArray finalMemArr = lma;
					Callable<Double> task = new Callable<Double>() {
						@Override
						public Double call() throws Exception {
							double val = doubleCollector.call();
							finalMemArr.add(val);
							log.debug("Added [{}] to DoubleRollingMetric [{}]", val, name);
							return val;
						}
					};
					doubleCollectors.put(name, ScheduledThreadPoolFactory.getInstance("Scheduler").scheduleAtFixedRate("Rollng Collection [" + name + "]", new FutureTask<Double>(task), 0, 60/samplesPerRange, TimeUnit.SECONDS));
					exists = false;
				}
			}
		}
		return exists;
	}
	
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
	public boolean registerLongRollingMetric(String name, int range, int samplesPerRange, final ObjectName longCollector, final String opName, final String commandName, final String... args) {
		Callable<Long> callable = new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				return (Long)JMXHelper.invoke(longCollector, JMXHelper.getHeliosMBeanServer(), opName, new Object[]{commandName, args}, COLLECT_SIGNATURE);
			}
		};
		return registerLongRollingMetric(name, range, samplesPerRange, callable);
	}

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
	public boolean registerDoubleRollingMetric(String name, int range, int samplesPerRange, final ObjectName doubleCollector, final String opName, final String commandName, final String... args) {
		Callable<Double> callable = new Callable<Double>() {
			@Override
			public Double call() throws Exception {
				return (Double)JMXHelper.invoke(doubleCollector, JMXHelper.getHeliosMBeanServer(), opName, new Object[]{commandName, args}, COLLECT_SIGNATURE);
			}
		};
		return registerDoubleRollingMetric(name, range, samplesPerRange, callable);
	}
	
	/**
	 * Returns the raw long array for the passed metric name
	 * @param name The metric name
	 * @param windowSize The number of minutes to retrieve for. <code>-1</code> means the whole raw array.
	 * @return the raw long array
	 */
	public long[] getLongArray(String name, int windowSize) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		log.debug("Fetching long array [{}] with window size [{}]", name, windowSize);
		LongMemArray lma = longArrays.get(name);
		if(lma==null) throw new RuntimeException("No long rolling metric registered for metric name [" + name + "]", new Throwable());
		return windowSize ==-1 ? lma.get() : lma.get(windowSize);
	}
	
	/**
	 * Returns the raw long array for the passed metric name
	 * @param name The metric name
	 * @return the raw long array
	 */
	public long[] getLongArray(String name) {
		return getLongArray(name, -1);
	}
	
	/**
	 * Returns the raw double array for the passed metric name
	 * @param name The metric name
	 * @param windowSize The number of minutes to retrieve for. <code>-1</code> means the whole raw array.
	 * @return the raw double array
	 */
	public double[] getDoubleArray(String name, int windowSize) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		log.debug("Fetching double array [{}] with window size [{}]", name, windowSize);
		DoubleMemArray lma = doubleArrays.get(name);
		if(lma==null) throw new RuntimeException("No double rolling metric registered for metric name [" + name + "]", new Throwable());
		return windowSize ==-1 ? lma.get() : lma.get(windowSize);
	}
	
	/**
	 * Returns the double long array for the passed metric name
	 * @param name The metric name
	 * @return the raw double array
	 */
	public double[] getDoubleArray(String name) {
		return getDoubleArray(name, -1);
	}
	
	
	

	
	/**
	 * Requests a rolling window evaluation
	 * @param name The metric name
	 * @param type The type of evaluation as defined in {@link AggregateFunction}
	 * @param windowSize The length of the window to evaluate (e.g. 1, 5 or 15 minutes)
	 * @return The calculated value
	 */
	public long getLongEvaluation(String name, String type, int windowSize) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		if(type==null || type.trim().isEmpty()) throw new IllegalArgumentException("The passed type was null or empty", new Throwable());
		log.debug("Evaluating long expression [{}] for metric [{}]", type, name);
		LongMemArray lma = longArrays.get(name);
		if(lma==null) throw new RuntimeException("No long rolling metric registered for metric name [" + name + "]", new Throwable());
		AggregateFunction af = AggregateFunction.forName(type);
		return af.aggregate(lma.get(windowSize));
	}
	
	/**
	 * Requests a rolling window evaluation
	 * @param name The metric name
	 * @param type The type of evaluation as defined in {@link AggregateFunction}
	 * @param windowSize The length of the window to evaluate (e.g. 1, 5 or 15 minutes)
	 * @return The calculated value
	 */
	public double getDoubleEvaluation(String name, String type, int windowSize) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		if(type==null || type.trim().isEmpty()) throw new IllegalArgumentException("The passed type was null or empty", new Throwable());
		log.debug("Evaluating double expression [{}] for metric [{}]", type, name);
		DoubleMemArray lma = doubleArrays.get(name);
		if(lma==null) throw new RuntimeException("No double rolling metric registered for metric name [" + name + "]", new Throwable());
		AggregateFunction af = AggregateFunction.forName(type);
		return af.aggregate(lma.get(windowSize));
	}
	
	
	/**
	 * Returns a list of the registered rolling double metrics
	 * @return a list of the registered rolling double metrics
	 */
	public List<DoubleMemArrayMBean> getDoubleMemArrays() {
		return Collections.unmodifiableList(new ArrayList<DoubleMemArrayMBean>(doubleArrays.values()));
	}
	
	/**
	 * Returns a list of the registered rolling long metrics
	 * @return a list of the registered rolling long metrics
	 */
	public List<LongMemArrayMBean> getLongMemArrays() {
		return Collections.unmodifiableList(new ArrayList<LongMemArrayMBean>(longArrays.values()));
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.RollingMetricServiceMXBean#getLevel()
	 */
	@Override
	public String getLevel() {
		return LoggerManager.getInstance().getLoggerLevelManager().getLoggerLevel(getClass().getName());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.rolling.RollingMetricServiceMXBean#setLevel(java.lang.String)
	 */
	@Override
	public void setLevel(String level) {
		LoggerManager.getInstance().getLoggerLevelManager().setLoggerLevel(getClass().getName(), level);
	}
	
	
	/**
	 * Returns the number of double mem arrays currently tracked
	 * @return the number of double mem arrays currently tracked
	 */
	public int getDoubleArrayCount() {
		return doubleArrays.size();
	}
	
	/**
	 * Returns the number of long mem arrays currently tracked
	 * @return the number of long mem arrays currently tracked
	 */
	public int getLongArrayCount() {
		return longArrays.size();
	}
	
}
