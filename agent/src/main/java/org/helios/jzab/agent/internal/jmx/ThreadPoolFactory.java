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

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: ThreadPoolFactory</p>
 * <p>Description: JMX instrumented thread pool executor factory</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.internal.jmx.ThreadPoolFactory</code></p>
 */
public class ThreadPoolFactory extends ThreadPoolExecutor implements ThreadFactory, ThreadPoolMXBean {
	/** The ObjectName that will be used to register the thread pool management interface */
	protected final ObjectName objectName;
	/** Serial number factory for thread names */
	protected final AtomicInteger serial = new AtomicInteger(0);
	/** The pool name */
	protected final String name;
	/** Indicates if threads should be daemons */
	protected final boolean daemonThreads;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	
	/** A map of created and started factories */
	protected static final Map<String, ThreadPoolFactory> tpFactories = new ConcurrentHashMap<String, ThreadPoolFactory>();
	
	/** The configuration node name */
	public static final String NODE = "thread-pool";
	
	/**
	 * Returns the named thread pool
	 * @param name The name of the thread pool to retrieve
	 * @return The named thread pool
	 * @throws  IllegalStateException Thrown if the named thread pool does not exist
	 */
	public static ThreadPoolFactory getInstance(String name) {
		if(name==null) throw new IllegalArgumentException("The passed name was null", new Throwable());
		ThreadPoolFactory tpf = tpFactories.get(name);
		if(tpf==null) throw new IllegalStateException("The thread pool named  [" + name + "] has not been initialized" , new Throwable());
		return tpf;
	}
	
	/**
	 * Creates or retrieves a ThreadPool
	 * @param configNode The XML configuration node 
	 * @return the ThreadPoolFactory named in the passed configNode
	 */
	public static ThreadPoolFactory newCachedThreadPool(Node configNode) {
		if(configNode==null) throw new IllegalArgumentException("Passed configuration node was null", new Throwable());
		String nodeName = configNode.getNodeName(); 
		if(!NODE.equals(nodeName)) {
			throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
		}
		String poolName = XMLHelper.getAttributeByName(configNode, "name", null);
		if(poolName==null || poolName.trim().isEmpty()) {
			throw new RuntimeException("ThreadPool Node had null name [" + XMLHelper.renderNode(configNode), new Throwable());
		}
		ThreadPoolFactory tpf = tpFactories.get(poolName);
		if(tpf==null) {
			synchronized(tpFactories) {
				tpf = tpFactories.get(poolName);
				if(tpf==null) {
					tpf = new ThreadPoolFactory(ThreadPoolConfig.getInstance(configNode));
					tpFactories.put(poolName, tpf);
				}
			}
		}
		return tpf;
	}
	
	
	
	/**
	 * Creates a new ThreadPool
	 * @param tpc A thread pool configuration 
	 */
	protected ThreadPoolFactory(ThreadPoolConfig tpc) {
		super(tpc.coreSize, tpc.maxSize, tpc.keepAlive, TimeUnit.SECONDS, tpc.buildQueue());
		setThreadFactory(this);
		name = tpc.name;
		daemonThreads = tpc.daemonThreads;
		if(tpc.preStart==-1) {
			prestartAllCoreThreads();
		} else if(tpc.preStart>0) {
			for(int i = 0; i < tpc.preStart; i++) {
					if(!this.prestartCoreThread()) break;
			}
		}
		objectName = JMXHelper.objectName("org.helios.jzab.agent.jmx:service=ThreadPool,name=" + name);
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), objectName, this);
		log.info("Created and registered ThreadPool [{}], daemon:[{}]", objectName, daemonThreads);
	}
	


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, name + "Thread#" + serial.incrementAndGet());
		t.setDaemon(daemonThreads);
		return t;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.internal.jmx.ThreadPoolMXBean#getWorkQueueType()
	 */
	@Override
	public String getWorkQueueType() {
		BlockingQueue<Runnable> q = getQueue();
		return q.getClass().getSimpleName() + ":" + q.remainingCapacity();
	}
	
	
	/**
	 * <p>Title: ThreadPoolConfig</p>
	 * <p>Description: Value container and parser for a thread pool config</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.internal.jmx.ThreadPoolFactory.ThreadPoolConfig</code></p>
	 */
	protected static class ThreadPoolConfig {
		/** The name of the pool */
		protected final String name;
		/** The pool's core thread count */
		protected final int coreSize;
		/** The pool's maximum thread count */
		protected final int maxSize;
		/** The maximum amount of time an idle thread should be kept around (s.) */
		protected final long keepAlive;
		/** Indicates if core threads should be allowed to timeout */
		protected final boolean allowCoreTimeout;
		/** The number of core threads to prestart on pool creation */
		protected final int preStart;
		/** The maximum amount of time allowed for threads to finish their work on a non-immediate shutdown  */
		protected final long termTime;
		/** Indicates if the pool should be terminated immdeiately on shutdown notice */
		protected final boolean immediateTerm;
		/** The size of the work queue for this pool */
		protected final int queueSize;
		/** Indicates if the work queue should be fair */
		protected final boolean fairQueue;
		
		/** Indicates if threads should be daemons */
		protected final boolean daemonThreads;
		
		
		/** The default core size which is the number of core available */
		public static final int DEF_CORE_SIZE = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		/** The default max size which is the number of core available X 4 */
		public static final int DEF_MAX_SIZE = DEF_CORE_SIZE * 4;
		/** The default thread prestart which is 1 */
		public static final int DEF_PRESTART = 1;		
		/** The default allow core thread timeout which is true */
		public static final boolean DEF_ALLOW_TIMEOUT = true;
		/** The default keep alive time for idles threads which is 60s */
		public static final long DEF_KEEPALIVE = 60;
		/** The default daemon status of pool threads which is true */
		public static final boolean DEF_DAEMON = true;
		
		
		/** The default immediate termination which is true */
		public static final boolean DEF_IMMEDIATE_TERM = true;
		/** The default allowed termination time which is 5 s */
		public static final long DEF_TERM_TIME = 5;
		/** The default queue fairness which is false */
		public static final boolean DEF_QUEUE_FAIR = false;
		/** The default queue size which is 100. If the queue size is 0, a synchronous queue will be used */
		public static final int DEF_QUEUE_SIZE = 100;
		
		/**
		 * Creates a new ThreadPoolConfig
		 * @param configNode The configuration node
		 */
		public ThreadPoolConfig(Node configNode) {
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
			maxSize = XMLHelper.getAttributeByName(currentNode, "max", DEF_MAX_SIZE);
			allowCoreTimeout = XMLHelper.getAttributeByName(currentNode, "allowCoreTimeout", DEF_ALLOW_TIMEOUT);
			preStart = XMLHelper.getAttributeByName(currentNode, "prestart", DEF_PRESTART);
			keepAlive  = XMLHelper.getAttributeByName(currentNode, "keepalive", DEF_KEEPALIVE);
			daemonThreads = XMLHelper.getAttributeByName(currentNode, "daemon", DEF_DAEMON);
			
			// ===== Termination Stuff =====
			currentNode = XMLHelper.getChildNodeByName(configNode, "termination", false);
			immediateTerm = XMLHelper.getAttributeByName(currentNode, "immediate", DEF_IMMEDIATE_TERM);
			if(immediateTerm) {
				termTime = 0;
			} else {
				termTime = XMLHelper.getAttributeByName(currentNode, "termTime", DEF_TERM_TIME);
			}
					
			// ===== Work Queue Stuff =====
			currentNode = XMLHelper.getChildNodeByName(configNode, "queue", false);
			fairQueue = XMLHelper.getAttributeByName(currentNode, "fair", DEF_QUEUE_FAIR);
			queueSize = XMLHelper.getAttributeByName(currentNode, "size", DEF_QUEUE_SIZE);
		}
		
		/**
		 * Builds a worker queue for this configuration
		 * @return a blocking queue for a thread pool work queue
		 */
		public BlockingQueue<Runnable> buildQueue() {
			if(queueSize<1) {
				return new SynchronousQueue<Runnable>(fairQueue);
			} 
			return new ArrayBlockingQueue<Runnable>(queueSize, fairQueue);			
		}

		/**
		 * Returns a ThreadPoolConfig for the passed config node
		 * @param configNode The configuration node
		 * @return a ThreadPoolConfig for the passed config node
		 */
		static ThreadPoolConfig getInstance(Node configNode) {
			return new ThreadPoolConfig(configNode);
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
		 * Returns the maximum pool size
		 * @return the maxSize
		 */
		public int getMaxSize() {
			return maxSize;
		}

		/**
		 * Returns how long idle threads will be kept around in s. 
		 * @return the keepAlive
		 */
		public long getKeepAlive() {
			return keepAlive;
		}

		/**
		 * Indicates of core threads are allowed to time out
		 * @return the allowCoreTimeout
		 */
		public boolean isAllowCoreTimeout() {
			return allowCoreTimeout;
		}

		/**
		 * Returns the number of core threads to pre-start. A prestart value of -1 means all core threads.
		 * @return the preStart
		 */
		public int getPreStart() {
			return preStart;
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
		 * Returns the size of the pool's work queue. If the queue size is 0, a synchronous queue will be used.
		 * @return the queueSize
		 */
		public int getQueueSize() {
			return queueSize;
		}

		/**
		 * Indicates if the work queue will be fair.
		 * @return the fairQueue
		 */
		public boolean isFairQueue() {
			return fairQueue;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ThreadPoolConfig [name=").append(name)
					.append(", coreSize=").append(coreSize)
					.append(", maxSize=").append(maxSize)
					.append(", keepAlive=").append(keepAlive)
					.append(", allowCoreTimeout=").append(allowCoreTimeout)
					.append(", preStart=").append(preStart)
					.append(", termTime=").append(termTime)
					.append(", immediateTerm=").append(immediateTerm)
					.append(", queueSize=").append(queueSize)
					.append(", fairQueue=").append(fairQueue).append("]");
			return builder.toString();
		}
	}


	/**
	 * Returns 
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * Returns 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ThreadPoolFactory [objectName=").append(objectName)
				.append("]");
		return builder.toString();
	}


}