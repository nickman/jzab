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

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: ScheduledThreadPoolFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.internal.jmx.ScheduledThreadPoolFactory</code></p>
 */

public class ScheduledThreadPoolFactory extends ScheduledThreadPoolExecutor implements ThreadFactory, SchedulerMXBean, RejectedExecutionHandler, UncaughtExceptionHandler {
	/** The ObjectName that will be used to register the scheduled thread pool management interface */
	protected final ObjectName objectName;
	/** Serial number factory for thread names */
	protected final AtomicInteger serial = new AtomicInteger(0);
	/** The scheduler name */
	protected final String name;
	/** Indicates if threads should be daemons */
	protected final boolean daemonThreads;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** A map of created and started scheduler factories */
	protected static final Map<String, ScheduledThreadPoolFactory> tpSchedulers = new ConcurrentHashMap<String, ScheduledThreadPoolFactory>();
	
	/** The configuration node name */
	public static final String NODE = "scheduler";
	
	/**
	 * Returns the named scheduler
	 * @param name The name of the scheduler to retrieve
	 * @return The named scheduler
	 * @throws  IllegalStateException Thrown if the named scheduler does not exist
	 */
	public static ScheduledThreadPoolFactory getInstance(String name) {
		if(name==null) throw new IllegalArgumentException("The passed name was null", new Throwable());
		ScheduledThreadPoolFactory tps = tpSchedulers.get(name);
		if(tps==null) throw new IllegalStateException("The scheduler named  [" + name + "] has not been initialized" , new Throwable());
		return tps;
	}	
	

	/**
	 * Creates a new ScheduledThreadPoolFactory
	 * @param tps A thread pool configuration 
	 */
	protected ScheduledThreadPoolFactory(ScheduledThreadPoolConfig tps) {
		super(tps.coreSize);		
		setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		setRejectedExecutionHandler(this);
		setThreadFactory(this);
		name = tps.name;
		daemonThreads = tps.daemonThreads;
		objectName = JMXHelper.objectName("org.helios.jzab.agent.jmx:service=Scheduler,name=" + name);
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), objectName, this);
		log.info("Created and registered ScheduledThreadPoolFactory [{}], daemon:[{}]", objectName, daemonThreads);
	}	
	
	/**
	 * Creates or retrieves a ScheduledThreadPoolFactory
	 * @param configNode The XML configuration node 
	 * @return the ScheduledThreadPoolFactory named in the passed configNode
	 */
	public static ScheduledThreadPoolFactory newScheduler(Node configNode) {
		if(configNode==null) throw new IllegalArgumentException("Passed configuration node was null", new Throwable());
		String nodeName = configNode.getNodeName(); 
		if(!NODE.equals(nodeName)) {
			throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
		}
		String poolName = XMLHelper.getAttributeByName(configNode, "name", null);
		if(poolName==null || poolName.trim().isEmpty()) {
			throw new RuntimeException("Scheduler Node had null name [" + XMLHelper.renderNode(configNode), new Throwable());
		}
		ScheduledThreadPoolFactory tps = tpSchedulers.get(poolName);
		if(tps==null) {
			synchronized(tpSchedulers) {
				tps = tpSchedulers.get(poolName);
				if(tps==null) {
					tps = new ScheduledThreadPoolFactory(ScheduledThreadPoolConfig.getInstance(configNode));
					tpSchedulers.put(poolName, tps);
				}
			}
		}
		return tps;
	}	
	
	/**
	 * Returns the assigned JMX ObjectName
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * Returns the scheduler name
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		log.warn("Scheduler [{}] rejected execution of [{}]", name, r);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.warn("Scheduler [{}] had uncaught exception", name, e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, name + "SchedulerThread#" + serial.incrementAndGet());
		t.setDaemon(daemonThreads);
		t.setUncaughtExceptionHandler(this);
		return t;
	}

	
	/**
	 * <p>Title: ScheduledThreadPoolConfig</p>
	 * <p>Description: Value container and parser for a scheduled thread pool config</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.internal.jmx.ScheduledThreadPoolFactory.ScheduledThreadPoolConfig</code></p>
	 */
	protected static class ScheduledThreadPoolConfig {
		/** The name of the pool */
		protected final String name;
		/** The pool's core thread count */
		protected final int coreSize;
		/** The maximum amount of time allowed for threads to finish their work on a non-immediate shutdown  */
		protected final long termTime;
		/** Indicates if the pool should be terminated immdeiately on shutdown notice */
		protected final boolean immediateTerm;
		
		/** Indicates if threads should be daemons */
		protected final boolean daemonThreads;
		
		
		/** The default core size which is the number of core available */
		public static final int DEF_CORE_SIZE = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		/** The default max size which is the number of core available X 4 */
		public static final int DEF_MAX_SIZE = DEF_CORE_SIZE * 4;
		/** The default keep alive time for idles threads which is 60s */
		public static final long DEF_KEEPALIVE = 60;
		/** The default daemon status of pool threads which is true */
		public static final boolean DEF_DAEMON = true;
		/** The default immediate termination which is true */
		public static final boolean DEF_IMMEDIATE_TERM = true;
		/** The default allowed termination time which is 5 s */
		public static final long DEF_TERM_TIME = 5;
		
		/**
		 * Creates a new ScheduledThreadPoolConfig
		 * @param configNode The configuration node
		 */
		public ScheduledThreadPoolConfig(Node configNode) {
			if(configNode==null) throw new RuntimeException("Passed configuration node was null", new Throwable());
			String nodeName = configNode.getNodeName(); 
			if(!NODE.equals(nodeName)) {
				throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
			}
			name = XMLHelper.getAttributeByName(configNode, "name", null);
			if(name==null || name.trim().isEmpty()) {
				throw new RuntimeException("ThreadPool Node had null name [" + XMLHelper.renderNode(configNode), new Throwable());
			}
			
			// ===== Pool Stuff ===== 
			Node currentNode = XMLHelper.getChildNodeByName(configNode, "pool", false);
			coreSize = XMLHelper.getAttributeByName(currentNode, "core", DEF_CORE_SIZE);
			daemonThreads = XMLHelper.getAttributeByName(currentNode, "daemon", DEF_DAEMON);
			
			// ===== Termination Stuff =====
			currentNode = XMLHelper.getChildNodeByName(configNode, "termination", false);
			immediateTerm = XMLHelper.getAttributeByName(currentNode, "immediate", DEF_IMMEDIATE_TERM);
			if(immediateTerm) {
				termTime = 0;
			} else {
				termTime = XMLHelper.getAttributeByName(currentNode, "termTime", DEF_TERM_TIME);
			}
					
		}
		

		/**
		 * Returns a ScheduledThreadPoolConfig for the passed config node
		 * @param configNode The configuration node
		 * @return a ThreadPoolConfig for the passed config node
		 */
		static ScheduledThreadPoolConfig getInstance(Node configNode) {
			return new ScheduledThreadPoolConfig(configNode);
		}

		/**
		 * Returns the name of the pool
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the core pool size
		 * @return the coreSize
		 */
		public int getCoreSize() {
			return coreSize;
		}


		/**
		 * Returns the amount of time in s. that threads will be given to complete their tasks on a shutdown notice.
		 * Not relevant if {@link #immediateTerm} is true.
		 * @return the termTime
		 */
		public long getTermTime() {
			return termTime;
		}

		/**
		 * Indicates if this pool will be shutdown immediately on a shutdown notice
		 * @return the immediateTerm
		 */
		public boolean isImmediateTerm() {
			return immediateTerm;
		}



		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ScheduledThreadPoolConfig [name=").append(name)
					.append(", coreSize=").append(coreSize)
					.append(", termTime=").append(termTime)
					.append(", immediateTerm=").append(immediateTerm);
			return builder.toString();
		}
	}


	

}
