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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.agent.net.active.ActiveHost.ActiveHostCheck;
import org.helios.jzab.agent.net.active.collection.IResultCollector;
import org.helios.jzab.agent.net.active.schedule.IScheduleBucket;
import org.helios.jzab.agent.net.active.schedule.PassiveScheduleBucket;
import org.helios.jzab.agent.net.routing.JSONResponseHandler;
import org.helios.jzab.agent.net.routing.RoutingObjectName;
import org.helios.jzab.agent.net.routing.RoutingObjectNameFactory;
import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.XMLHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: ActiveServer</p>
 * <p>Description: Represents a zabbix server for which active checks are being perfomed</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ActiveServer</code></p>
 * TODO: <ol>
 * 	<li>Need to check for duplicate target hosts and handle them. Probably should replace the existing, but need to make sure the old host instance is cleaned up</li>
 * </ol>
 */

public class ActiveServer implements JSONResponseHandler, ActiveServerMXBean, Iterable<ActiveHost>  {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The parent ActiveAgent for this server */
	protected final ActiveAgent agent;
	/** The ip address or host name of the zabbix server */
	protected final String address;
	/** The zabbix server's listening port */
	protected final int port;
	/** The frequency in seconds that this server should be interrogated for active checks on hosts being monitored for it */
	protected long refreshPeriod;
	/** The hosts monitored for this server keyed by host name */
	protected final Map<String, ActiveHost> activeHosts = new ConcurrentHashMap<String, ActiveHost>();
	/** The asynch task executor */
	protected final ThreadPoolExecutor executor;
	/** The routing object names for this host */
	protected final RoutingObjectName[] routingNames; 
	
	
	/** The configuration node name */
	public static final String NODE = "hosts";
	/** The JSON item key for the response status */
	public static final String RESPONSE_STATUS = "response";
	/** The JSON item value for a successful response */
	public static final String RESPONSE_STATUS_OK = "success";
	
	/** The JSON item key for the response data */
	public static final String RESPONSE_DATA_VALUE = "data";
	/** The JSON item key for the response data type */
	public static final String RESPONSE_DATA_TYPE = "active checks";
	
	/** The JSON item key for the active check submission response */
	public static final String RESPONSE_SUBMISSION = "info";
	
	/** THe regex pattern used to parse an active check submission response */
	public static final Pattern INFO_RESPONSE_REGEX = Pattern.compile("Processed (\\d+) Failed (\\d+) Total (\\d+) Seconds spent (.*)$");
	
	/** The scheduler bucket to manage and aggregate delay windows for this server's active hosts */
	protected final PassiveScheduleBucket<ActiveHost, ActiveServer> scheduleBucket;
	/** A set of registered server response listeners */
	protected final Set<ActiveServerResponseListener> listeners = new CopyOnWriteArraySet<ActiveServerResponseListener>();
	
	
	/**
	 * Creates a new ActiveServer
	 * @param The parent agent for this server
	 * @param address The ip address or host name of the zabbix server
	 * @param port The zabbix server's listening port
	 * @param refreshPeriod The frequency in seconds that this server should be interrogated for active checks on hosts being monitored for it
	 * @param executor The asynch task executor
	 * @param parentScheduler The parent scheduler bucket
	 * @param hosts The configuration nodes for the hosts to be monitored for this zabbix server
	 */
	public ActiveServer(ActiveAgent agent, String address, int port, long refreshPeriod, ThreadPoolExecutor executor, IScheduleBucket<ActiveServer> parentScheduler, Node hosts) {
		if(address==null || address.trim().isEmpty()) throw new IllegalArgumentException("The passed zabix address was null", new Throwable());
		this.address = address;
		this.agent = agent;
		this.port = port;
		this.refreshPeriod = refreshPeriod;
		this.executor = executor;
		// ==================  UPDATE ME  ===================		
		scheduleBucket = new PassiveScheduleBucket<ActiveHost,ActiveServer>(parentScheduler, this);
		// ==================================================
		int cnt = 0;
		if(hosts!=null) {
			String nodeName = hosts.getNodeName();
			if(!NODE.equalsIgnoreCase(nodeName)) {
				throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
			}
			
			for(Node hostNode: XMLHelper.getChildNodesByName(hosts, "host", false)) {
				String name = XMLHelper.getAttributeByName(hostNode, "name", "");
				if(name.trim().isEmpty()) {
					throw new RuntimeException("Hosts node contained a host with a null or empty name: [" + XMLHelper.getStringFromNode(hosts) + "]", new Throwable());
				}
				long refresh = XMLHelper.getAttributeByName(hostNode, "refresh", this.refreshPeriod);
				addActiveHost(name, refresh);
				cnt++;
			}
		}
		routingNames = new RoutingObjectName[]{RoutingObjectNameFactory.getInstance().getRoute(true, KEY_REQUEST, VALUE_ACTIVE_CHECK_SUBMISSION)};
		RoutingObjectNameFactory.getInstance().registerJSONResponseHandler(this);
		log.debug("Added [{}] hosts for active server [{}]", cnt, address + ":" + port);
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), JMXHelper.objectName(new StringBuilder("org.helios.jzab.agent.active:service=ActiveServer,server=").append(address).append(",port=").append(port)), this);
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.routing.JSONResponseHandler#jsonResponse(org.json.JSONObject)
	 */
	@Override
	public void jsonResponse(RoutingObjectName routing, JSONObject response) throws JSONException {
		String requestType = routing.getKeyProperty(KEY_REQUEST);
		if(VALUE_ACTIVE_CHECK_SUBMISSION.equals(requestType)) {
			processSubmissionResponse(response);
			 
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.routing.JSONResponseHandler#getRoutingObjectNames()
	 */
	@Override
	public RoutingObjectName[] getRoutingObjectNames() {
		return routingNames;
	}
		
	
	/**
	 * Determines if this active server has any hosts requiring a marching orders refresh
	 * @param currentTime The current time in ms.
	 * @return true if this server has at least one active host requiring a marching orders refresh, false otherwise
	 */
	public boolean requiresRefresh(long currentTime) {		
		for(ActiveHost ah: activeHosts.values()) {
			if(ah.requiresRefresh(currentTime)) return true;
		}
		return false;
	}
	
	/**
	 * Determines if this active server has any hosts requiring a marching orders refresh
	 * @return true if this server has at least one active host requiring a marching orders refresh, false otherwise
	 */
	public boolean isRequiresRefresh() {
		return requiresRefresh(SystemClock.currentTimeMillis());
	}
	
	
	/**
	 * Returns the logging level for this active agent listener
	 * @return the logging level for this active agent
	 */
	@Override
	public String getLevel() {
		return LoggerManager.getInstance().getLoggerLevelManager().getLoggerLevel(getClass().getName());
	}
	
	/**
	 * Sets the logger level for this active agent
	 * @param level The level to set this logger to
	 */
	@Override
	public void setLevel(String level) {
		LoggerManager.getInstance().getLoggerLevelManager().setLoggerLevel(getClass().getName(), level);
	}
	
	/**
	 * Returns a map of the number of hosts registered for checks for each delay
	 * @return a map of the number of hosts registered for checks for each delay
	 */
	public Map<Long, Integer> getScheduleCounts() {
		Map<Long, Integer> map = new HashMap<Long, Integer>(scheduleBucket.size());
		for(Map.Entry<Long, Set<ActiveHost>> entry: scheduleBucket.entrySet()) {
			map.put(entry.getKey(), entry.getValue().size());
		}
		return map;		
	}
	
	/**
	 * Returns the name ActiveHost
	 * @param hostName the ActiveHost name 
	 * @return the ActiveHost or null if it was not found
	 */
	public ActiveHost getActiveHost(String hostName) {
		if(hostName==null) throw new IllegalArgumentException("The passed hostName was null", new Throwable());
		return this.activeHosts.get(hostName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ActiveHost> iterator() {		
		return Collections.unmodifiableCollection(activeHosts.values()).iterator();
	}
	
	
	protected void fireResponseEvent(final JSONObject json) {
		final ActiveServer fserver = this;
		executor.execute(new Runnable(){
			public void run() {
				for(ActiveServerResponseListener listener: listeners) {
					listener.onResponse(fserver, json);
				}
			}
		});
	}
	
//	/**
//	 * Routes a received JSON response
//	 * @param hostName the host name that this response is for
//	 * @param json the received JSON response
//	 */
//	protected void handleJson(String hostName, final JSONObject json) {
//		if(hostName==null) throw new RuntimeException("The correlating host name was null", new Throwable());
//		log.debug("Host [{}], JSON Keys: {}",hostName,  Arrays.toString(JSONObject.getNames(json)));
//		try {
//			String responseStatus = json.getString(RESPONSE_STATUS);
//			String responseType = json.getString("request");
//			if(!RESPONSE_STATUS_OK.equalsIgnoreCase(responseStatus)) {
//				log.warn("Response indicated request failure [{}]", responseStatus);
//			} else {
//				if(RESPONSE_DATA_TYPE.equalsIgnoreCase(responseType)) {
//					updateActiveChecks(hostName, json.getJSONArray(RESPONSE_DATA_VALUE));
//				} else if(RESPONSE_SUBMISSION.equalsIgnoreCase(responseType)) {
//					processSubmissionResponse(hostName, json);
//				} else {
//					log.warn("Unrecognized Server Response Type [{}]", responseType);
//				}
//			}
//			
//		} catch (Exception e) {
//			log.warn("Failed to handle JSON response: {}", e.getMessage());
//			log.debug("Failed to handle JSON response: {}", json, e);
//		} finally {
//			fireResponseEvent(json);
//		}
//	}
	
	/**
	 * Parses and processes the active check submission response
	 * @param response The server response to the active check submission
	 * @throws JSONException Thrown if JSON response cannot be parsed
	 */
	protected void processSubmissionResponse(JSONObject response) throws JSONException {
		Matcher matcher = INFO_RESPONSE_REGEX.matcher(response.getString(RESPONSE_SUBMISSION));
		if(!matcher.matches()) {
			log.warn("Failed to match expected response with value [{}]", response.toString());
		} else {
			long processed = Long.parseLong(matcher.group(1));
			long failed = Long.parseLong(matcher.group(2));
			long total = Long.parseLong(matcher.group(3));
			float time = Float.parseFloat(matcher.group(1));
			log.info(String.format("\nActive Check Submission Results for [%s]\n\tProcessed:%s\n\tFailed:%s\n\tTotal:\n\tProcess Time:%s\n", processed, failed, total, time));
			// DO Something USEFUL with this data
		}
	}
	

	/**
	 * Updates the active checks for an active host 
	 * @param hostName the host name that this response is for
	 * @param dataArray The <code>data</code> segment of the json response which is an array of active checks
	 */
	protected void updateActiveChecks(String hostName, JSONArray dataArray) {
		log.debug("Processing [{}] Active Check Marching Orders for Host [{}]", dataArray.length(), hostName);
		ActiveHost ah = activeHosts.get(hostName);
		if(ah!=null) {
			ah.upsertActiveChecks(dataArray);
		}
		log.debug(ah.displayScheduleBuckets());
//		long start = System.currentTimeMillis();
//		String[] results = ah.executeChecks();
//		long elapsed = System.currentTimeMillis()-start;
//		log.info("Executed Checks for [{}] in [{}] ms.", ah.hostName, elapsed);
//		if(log.isTraceEnabled()) {
//			for(String result: results) {
//				log.trace("Refreshed Active Check [{}]", result);
//			}
//		}
	}
	
	/**
	 * Adds a new response listener
	 * @param listener the listener to add
	 */
	public void addListener(ActiveServerResponseListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes a response listener
	 * @param listener the listener to remove
	 */
	public void removeListener(ActiveServerResponseListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
	}
	
	
	
	/**
	 * Executes all the checks for all hosts for this server
	 * @param collector The result collection stream
	 */
	public void executeChecks(IResultCollector collector) {
		for(ActiveHost host: activeHosts.values()) {
			host.executeChecks(collector);
		}
	}

	
	/**
	 * Executes all the checks in all this server's active hosts for the passed delay
	 * @param delay The delay window
	 * @param collector The result collection stream
	 */
	public void executeChecks(long delay,IResultCollector collector) {		
		for(ActiveHost ah: activeHosts.values()) {
			ah.executeChecks(delay, collector);
		}
	}
	
	/**
	 * Returns a set of ActiveHostChecks for the passed delay
	 * @param delay the delay to get checks for
	 * @return a set of ActiveHostChecks 
	 */
	public Set<ActiveHostCheck> getChecksForDelay(long delay) {
		Set<ActiveHostCheck> set = new HashSet<ActiveHostCheck>();
		for(ActiveHost host: scheduleBucket.get(delay)) {
			set.addAll(host.getChecksForDelay(delay));
		}
		return set;
	}
	

	/**
	 * Adds a new host to be monitored
	 * @param hostName The name of the host
	 * @param refreshPeriod The frequency in seconds that marching orders should be refreshed for this host
	 */
	public void addActiveHost(String hostName, long refreshPeriod) {
		activeHosts.put(hostName, new ActiveHost(this, hostName, refreshPeriod));
	}

	/**
	 * Adds a new host to be monitored
	 * @param hostName The name of the host
	 */
	public void addActiveHost(String hostName) {
		addActiveHost(hostName, getRefreshPeriod());
	}
	
	/**
	 * Returns this server's address and port based ID.
	 * @return this server's address and port based ID.
	 */
	public String getId() {
		return address + ":" + port;
	}
	


	/**
	 * Returns the frequency in seconds that this server should be interrogated for active checks on hosts being monitored for it 
	 * @return the refreshPeriod
	 */
	public long getRefreshPeriod() {
		return refreshPeriod;
	}


	/**
	 * Sets the frequency in seconds that this server should be interrogated for active checks on hosts being monitored for it
	 * @param refreshPeriod the refreshPeriod to set
	 */
	public void setRefreshPeriod(long refreshPeriod) {
		this.refreshPeriod = refreshPeriod;
	}


	/**
	 * Returns the ip address or host name of the zabbix server 
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}


	/**
	 * Returns the zabbix server's listening port
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + port;
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ActiveServer other = (ActiveServer) obj;
		if (address == null) {
			if (other.address != null) {
				return false;
			}
		} else if (!address.equals(other.address)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		return true;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ActiveServer [address=").append(address)
				.append(", port=").append(port).append(", refreshPeriod=")
				.append(refreshPeriod).append("]");
		return builder.toString();
	}




	
	
}
