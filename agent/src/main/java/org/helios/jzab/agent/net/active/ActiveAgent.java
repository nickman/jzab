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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.internal.jmx.ScheduledThreadPoolFactory;
import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.agent.net.active.schedule.ActiveScheduleBucket;
import org.helios.jzab.agent.net.active.schedule.CommandThreadPolicy;
import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: ActiveAgent</p>
 * <p>Description: Service that coordinates the configuration and execution of active host checks</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ActiveAgent</code></p>
 */
public class ActiveAgent implements ActiveAgentMXBean {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** The task scheduler */
	protected final ScheduledThreadPoolExecutor scheduler;
	/** The asynch task executor */
	protected final ThreadPoolExecutor executor;
	
	/** The collection threading policy */
	protected CommandThreadPolicy commandThreadPolicy;
	/** Indicates if in memory collation of collection results should be used */
	protected boolean inMemoryCollation;
	
	/** The master schedule bucket for this agent */
	protected final ActiveScheduleBucket<ActiveServer, ActiveAgent> scheduleBucket;
	
	/** The agent level refresh period in seconds */
	protected long agentRefreshPeriod;
	
	/** A map of the configured active servers keyed by <code>address:port</code> */
	protected final Map<String, ActiveServer> activeServers = new ConcurrentHashMap<String, ActiveServer>();
	
	/** The active agent singleton instance */
	private static volatile ActiveAgent instance = null;
	/** The active agent singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** Indicates if the active agent has been started */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	
	
	/** The ActiveAgent JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.agent.active:service=ActiveAgent");
	/** The configuration node name */
	public static final String NODE = "active-agent";
	/** The default agent refresh period on which the agent attempts to refresh marching orders for all monitored servers, which is 3600 or 1 hour */
	public static final long DEFAULT_AGENT_REFRESH = 60 *60;
	
	
	/**
	 * Initializes the ActiveAgent singleton instance
	 * @param configNode The configuration node
	 * @return the ActiveAgent singleton instance
	 */
	public static ActiveAgent getInstance(Node configNode) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ActiveAgent(configNode);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Acquires the ActiveAgent singleton instance
	 * @return the ActiveAgent singleton instance
	 */
	public static ActiveAgent getInstance() {
		if(instance==null) {
			throw new IllegalStateException("The ActiveAgent has not been initialized", new Throwable());
		}
		return instance;
	}
	
	/**
	 * Returns the number of active zabbix servers configured
	 * @return the number of active zabbix servers configured
	 */
	public int getActiveServerCount() {
		return activeServers.size();
	}
	
	
	/**
	 * IMPLEMENT THIS:
	 * 		collection-policy="THREAD_PER_HOST" collate-in-memory="true"
	 */
	
	/** The default collection threading policy */
	public static final String DEFAULT_COLLECTION_POLICY = CommandThreadPolicy.THREAD_PER_HOST.name();
	/** The collection threading policy attribute name */
	public static final String COLLECTION_POLICY_ATTR = "collection-policy";
	/** The default collate in memory flag */
	public static final boolean DEFAULT_COLLATE_IN_MEM = true;
	/** The collate in memory  attribute name */
	public static final String COLLATE_IN_MEM_ATTR = "collate-in-memory";
	
	
	/**
	 * Creates a new ActiveAgent
	 * @param configNode The configuration node
	 */
	private ActiveAgent(Node configNode) {
		if(configNode==null) throw new IllegalArgumentException("The passed configuration node was null", new Throwable());
		log.info("Configuring ActiveAgent");
		String nodeName = configNode.getNodeName(); 
		if(!NODE.equals(nodeName)) {
			throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
		}
		agentRefreshPeriod = XMLHelper.getAttributeByName(configNode, "refresh", DEFAULT_AGENT_REFRESH);		
		String schedulerName = null, executorName = null;
		try {
			schedulerName = XMLHelper.getAttributeByName(XMLHelper.getChildNodeByName(configNode, "scheduler-pool", false), "name", "Scheduler");
			scheduler = ScheduledThreadPoolFactory.getInstance(schedulerName);
		} catch (Exception e) {
			throw new RuntimeException("ActiveAgent failed to get scheduler named [" + schedulerName + "]", e);
		}
		try {
			executorName = XMLHelper.getAttributeByName(XMLHelper.getChildNodeByName(configNode, "task-pool", false), "name", "TaskExecutor");
			executor = ThreadPoolFactory.getInstance(executorName);
		} catch (Exception e) {
			throw new RuntimeException("ActiveAgent failed to get task executor named [" + executorName + "]", e);
		}
		commandThreadPolicy = CommandThreadPolicy.forName(XMLHelper.getAttributeByName(configNode, COLLECTION_POLICY_ATTR, DEFAULT_COLLECTION_POLICY));		
		inMemoryCollation = XMLHelper.getAttributeByName(configNode, COLLATE_IN_MEM_ATTR, DEFAULT_COLLATE_IN_MEM);
		
		scheduleBucket = new ActiveScheduleBucket<ActiveServer, ActiveAgent>(
				scheduler, commandThreadPolicy, inMemoryCollation, executor, ActiveServer.class
		);
		
		Node servers = XMLHelper.getChildNodeByName(configNode, "servers", false);
		if(servers==null) {
			log.warn("ActiveAgent had no configured servers to be active for....");
			return;
		}
		int serverCount = 0;
		for(Node serverNode: XMLHelper.getChildNodesByName(servers, "server", false)) {
			String address = XMLHelper.getAttributeByName(serverNode, "address", null);
			if(address==null || address.trim().isEmpty()) {
				log.warn("Empty active agent server name. Node was [{}]", XMLHelper.getStringFromNode(serverNode));
				continue;
			}			
			int port = XMLHelper.getAttributeByName(serverNode, "port", 10051);
			long refresh = XMLHelper.getAttributeByName(serverNode, "refresh", agentRefreshPeriod);
			String key = address + ":" + port;
			if(activeServers.containsKey(key)) {
				log.warn("Duplicate active server definition [{}]", key);
				continue;
			}
			activeServers.put(key, new ActiveServer(address, port, refresh, scheduleBucket,  XMLHelper.getChildNodeByName(serverNode, "hosts", false)));
			serverCount++;
		}
		if(serverCount==0) {
			log.warn("ActiveAgent had no configured servers to be active for....");
		}
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), JMXHelper.objectName("org.helios.jzab.agent.active:service=ActiveAgent"), this);
	}
	
	/**
	 *  Asynchronously starts the active agent
	 */
	public void start() {
		if(started.get()) {
			log.warn("ActiveAgent already started");
			return;
		}
		executor.execute(new Runnable() {
			public void run() {
				initializeServers();
				setupServerSchedules();
				started.set(true);
			}
		});		
		
	}
	
	/**
	 * Initializes this agent's active servers
	 */
	protected void initializeServers() {
		log.debug("Initializing Servers");
		for(ActiveServer server: activeServers.values()) {
			long currentTime = SystemClock.currentTimeMillis();
			if(server.requiresRefresh(currentTime)) {
				server.refreshActiveChecks();				
			}
		}		
	}
	
	/** Sets of active servers with scheduled checks keyed by the delay of the checks */
	protected final Map<Long, Set<ActiveServer>> serverSchedules = new ConcurrentHashMap<Long, Set<ActiveServer>>();
	
	protected void setupServerSchedules() {
		
	}
	
	/**
	 * Returns a map of the number of servers registered for checks for each delay
	 * @return a map of the number of servers registered for checks for each delay
	 */
	public Map<Long, Integer> getScheduleCounts() {
		Map<Long, Integer> map = new HashMap<Long, Integer>(scheduleBucket.size());
		for(Map.Entry<Long, Set<ActiveServer>> entry: scheduleBucket.entrySet()) {
			map.put(entry.getKey(), entry.getValue().size());
		}
		return map;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.ActiveAgentMXBean#getLevel()
	 */
	@Override
	public String getLevel() {
		return LoggerManager.getInstance().getLoggerLevelManager().getLoggerLevel(getClass().getName());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.ActiveAgentMXBean#setLevel(java.lang.String)
	 */
	@Override
	public void setLevel(String level) {
		LoggerManager.getInstance().getLoggerLevelManager().setLoggerLevel(getClass().getName(), level);
	}

	/**
	 * Returns the command thread policy
	 * @return the commandThreadPolicy
	 */
	public String getCommandThreadPolicy() {
		return commandThreadPolicy.name();
	}

	/**
	 * Sets the command thread policy
	 * @param commandThreadPolicyName the commandThreadPolicy to set
	 */
	public void setCommandThreadPolicy(String commandThreadPolicyName) {
		this.commandThreadPolicy = CommandThreadPolicy.forName(commandThreadPolicyName);
	}

	/**
	 * Indicates if in-memory collation is being used
	 * @return the inMemoryCollation true if using memory, false if using disk
	 */
	public boolean isInMemoryCollation() {
		return inMemoryCollation;
	}

	/**
	 * Sets the in-memory collation 
	 * @param inMemoryCollation true to use in memory, false to use disk
	 */
	public void setInMemoryCollation(boolean inMemoryCollation) {
		this.inMemoryCollation = inMemoryCollation;
	}
	
}


/*
 <active-agent refresh="10">
 	<scheduler-pool name="Scheduler" />
 	<servers>   
	 	<server address="10.230.12.145" port="10051" refresh="20">
	 		<hosts>
	 			<host name="NE-WK-NWHI-01 Active" refresh="120" />
	 		</hosts>
	 	</server>
 	</servers>
 </active-agent>
 
 "response":"failed",
  "info":"host [NE-WK-NWHI-01 ActiveX] not found"}
  
{
	"response":"success",
	"info":"Processed 47 Failed 0 Total 47 Seconds spent 0.000708"}
	
{
	"response":"success",
	"info":"Processed 0 Failed 14 Total 14 Seconds spent 0.000368"}	  

 */ 
