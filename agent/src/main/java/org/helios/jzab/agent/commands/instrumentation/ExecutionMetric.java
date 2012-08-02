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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.jzab.agent.SystemClock;

/**
 * <p>Title: ExecutionMetric</p>
 * <p>Description: An execution metric for command processors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.instrumentation.ExecutionMetric</code></p>
 */
public class ExecutionMetric implements ExecutionMetricMBean {
	/** The name of this metric */
	private final String name;
	/** The last execution date */
	private long lastExecutionDate = -1L;
	/** The last execution elapsed */
	private long lastExecutionElapsed = -1L;
	/** The total number of executions */
	private long executionCount= 0L;
	/** The last <i>n</i> execution times */
	private long[] lastNExectionTimes = new long[0];

	/** The rolling average window size which is 5 unless overridden by a sys property called <b><code>jzsab.exec.window</code></b> */
	protected static final int WINDOW_SIZE;
	
	static {
		int tmp = -1;
		try {
			tmp = Integer.parseInt(System.getProperty("jzsab.exec.window", "5"));
		} catch (Exception e) {
			tmp = 5;
		}
		WINDOW_SIZE = tmp;
	}
	
	/** A map of registered execution metrics keyed by name */
	protected static final Map<String, ExecutionMetric> metrics = new ConcurrentHashMap<String, ExecutionMetric>();
	
	/**
	 * Clears the metrics.
	 */
	public static void clear() {
		metrics.clear();
	}
	
	/**
	 * Submits a metric
	 * @param name The name of the metric
	 * @param elapsed The elapsed time in ms.
	 */
	public static void submit(String name, long elapsed) {
		if(name==null) throw new IllegalArgumentException("The passed metric name was null", new Throwable());
		ExecutionMetric metric = metrics.get(name);
		if(metric==null) {
			synchronized(metrics) {
				metric = metrics.get(name);
				if(metric==null) {
					metric = new ExecutionMetric(name);
					metrics.put(name, metric);
				}
			}
		}
		metrics.get(name).process(elapsed);
	}
	
	/**
	 * Processes a new submission
	 * @param elapsed The elapsed time of the last execution
	 */
	private void process(long elapsed) {
		lastExecutionDate = SystemClock.currentTimeMillis();
		lastExecutionElapsed = elapsed;
		incrementWindow(elapsed);
		executionCount++;		
	}
	
	/**
	 * Pushes a new elapsed time into the rolling average window
	 * @param elapsed The latest elapsed time
	 */
	private void incrementWindow(long elapsed) {
		int length = lastNExectionTimes.length;
		if(length==0) {
			lastNExectionTimes = new long[]{elapsed};
		} else {
			int nlen = length+1>WINDOW_SIZE ? WINDOW_SIZE : length+1;
			long tmp[] = new long[nlen];
			tmp[0] = elapsed;
			System.arraycopy(lastNExectionTimes, 0, tmp, 1, nlen-1);
			lastNExectionTimes = tmp;	
			tmp = null;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean#getLastNElapsed()
	 */
	public long[] getLastNElapsed() {
		return lastNExectionTimes;
	}
	
	/**
	 * Returns a set of the currently registered execution metrics
	 * @return a set of the currently registered execution metrics
	 */
	public static Set<ExecutionMetricMBean> getExecutionMetrics() {
		return Collections.unmodifiableSet(new HashSet<ExecutionMetricMBean>(metrics.values()));
	}
	
	/**
	 * Creates a new ExecutionMetric
	 * @param name The name of this metric
	 */
	private ExecutionMetric(String name) {
		this.name = name;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean#getLastExecutionDate()
	 */
	@Override
	public Date getLastExecutionDate() {
		return new Date(lastExecutionDate);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean#getLastExecutionElapsed()
	 */
	@Override
	public long getLastExecutionElapsed() {
		return lastExecutionElapsed;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean#getExecutionCount()
	 */
	@Override
	public long getExecutionCount() {
		return executionCount;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean#getAvgExecutionElapsed()
	 */
	@Override
	public long getAvgExecutionElapsed() {
		long[] tmp = lastNExectionTimes;
		int length = tmp.length;
		if(length<1) return -1L;
		long total = 0;
		for(int i = 0; i < length; i++) {
			total += tmp[i];
		}
		return avg(total, length);
	}
	
	protected long avg(double total, double count) {
		if(total==0 || count==0) return 0;
		double d = total/count;
		return (long)d;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ExecutionMetric [name=");
		builder.append(name);
		builder.append(", lastExecutionDate=");
		builder.append(new Date(lastExecutionDate));
		builder.append(", lastExecutionElapsed=");
		builder.append(lastExecutionElapsed);
		builder.append(", executionCount=");
		builder.append(executionCount);
		builder.append(", lastNExectionTimes=");
		builder.append(Arrays.toString(lastNExectionTimes));
		builder.append("]");
		builder.append(", avgExecutionElapsed=");
		builder.append(getAvgExecutionElapsed());
		builder.append("]");
		return builder.toString();
	}

	public static void log(Object msg) {
		System.out.println(msg);		
	}
	
	public static void main(String[] args) {
		log("ExecMetric Test");
		Random random = new Random(System.nanoTime());
		for(int i = 0; i < 10; i++) {
			ExecutionMetric.submit("foo", Math.abs(random.nextInt(1000)));
			ExecutionMetric.submit("bar", Math.abs(random.nextInt(1000)));
		}
		
		for(ExecutionMetricMBean metric: ExecutionMetric.getExecutionMetrics()) {
			log(metric);
		}
	}

}
