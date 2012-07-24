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

import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.agent.net.active.schedule.IScheduleBucket;
import org.helios.jzab.agent.net.active.schedule.PassiveScheduleBucket;
import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.XMLHelper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
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

public class ActiveServer implements ChannelUpstreamHandler, Runnable, ActiveServerMXBean  {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

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
	 * @param address The ip address or host name of the zabbix server
	 * @param port The zabbix server's listening port
	 * @param refreshPeriod The frequency in seconds that this server should be interrogated for active checks on hosts being monitored for it
	 * @param executor The asynch task executor
	 * @param parentScheduler The parent scheduler bucket
	 * @param hosts The configuration nodes for the hosts to be monitored for this zabbix server
	 */
	public ActiveServer(String address, int port, long refreshPeriod, ThreadPoolExecutor executor, IScheduleBucket<ActiveServer> parentScheduler, Node hosts) {
		if(address==null || address.trim().isEmpty()) throw new IllegalArgumentException("The passed zabix address was null", new Throwable());
		this.address = address;
		this.port = port;
		this.refreshPeriod = refreshPeriod;
		this.executor = executor;
		// ==================  UPDATE ME  ===================		
		scheduleBucket = new PassiveScheduleBucket<ActiveHost,ActiveServer>(parentScheduler, this);
		// ==================================================
		
		if(hosts!=null) {
			String nodeName = hosts.getNodeName();
			if(!NODE.equalsIgnoreCase(nodeName)) {
				throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
			}
			int cnt = 0;
			for(Node hostNode: XMLHelper.getChildNodesByName(hosts, "host", false)) {
				String name = XMLHelper.getAttributeByName(hostNode, "name", "");
				if(name.trim().isEmpty()) {
					throw new RuntimeException("Hosts node contained a host with a null or empty name: [" + XMLHelper.getStringFromNode(hosts) + "]", new Throwable());
				}
				long refresh = XMLHelper.getAttributeByName(hostNode, "refresh", this.refreshPeriod);
				addActiveHost(name, refresh);
				cnt++;
			}
			log.debug("Added [{}] hosts for active server [{}]", cnt, address + ":" + port);
			JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), JMXHelper.objectName(new StringBuilder("org.helios.jzab.agent.active:service=ActiveServer,server=").append(address).append(",port=").append(port)), this);
		}
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
	
	/** The active check request template */
	public static final String ACTIVE_CHECK_REQUEST_TEMPLATE = "{\"request\":\"active checks\",\"host\":\"%s\"}";

	/** A map of host names keyed by the channel id of the channel issuing the asynch request */
	protected final Map<Integer, String> channelHostNameDecode = new ConcurrentHashMap<Integer, String>();
	
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
	 * Executes an active check request for each host requiring an update. 
	 */
	public void refreshActiveChecks() {
		for(final ActiveHost ah: activeHosts.values()) {
			if(ah.isRequiresRefresh()) {
				final Channel channel = ActiveClient.getInstance().newChannel(address, port);
				channel.getPipeline().addLast("jsonHandler", this);		
				channelHostNameDecode.put(channel.getId(), ah.hostName);
				try {
					channel.write(new JSONObject(String.format(ACTIVE_CHECK_REQUEST_TEMPLATE, ah.hostName))).addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if(future.isSuccess()) {
								log.debug("Request Sent to [{}] for Host [{}]", address, ah.hostName);
							} else {
								log.debug("Request Sent to Host [{}] failed", ah.hostName, future.getCause());
								channelHostNameDecode.remove(channel.getId());
							}
						}
					});
				} catch (JSONException je) {
					log.error("Failed to create ActiveCheck Request", je);
				}
			}
		}
	}
	
	/**
	 * Handles decoded JSON responses from the associated zabbix server
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			Object msg = ((MessageEvent)e).getMessage();
			if(msg instanceof JSONObject) {				
				handleJson(channelHostNameDecode.remove(e.getChannel().getId()), (JSONObject)msg);
			} else {
				log.warn("Unexpected message type received: [{}]", msg.getClass().getName());
			}
		}
		ctx.sendUpstream(e);		
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
	
	/**
	 * Routes a received JSON response
	 * @param hostName the host name that this response is for
	 * @param json the received JSON response
	 */
	protected void handleJson(String hostName, final JSONObject json) {
		if(hostName==null) throw new RuntimeException("The correlating host name was null", new Throwable());
		log.debug("Host [{}], JSON Keys: {}",hostName,  Arrays.toString(JSONObject.getNames(json)));
		try {
			String responseStatus = json.getString(RESPONSE_STATUS);
			String responseType = json.getString("request");
			if(!RESPONSE_STATUS_OK.equalsIgnoreCase(responseStatus)) {
				log.warn("Response indicated request failure [{}]", responseStatus);
			} else {
				if(RESPONSE_DATA_TYPE.equalsIgnoreCase(responseType)) {
					updateActiveChecks(hostName, json.getJSONArray(RESPONSE_DATA_VALUE));
				} else if(RESPONSE_SUBMISSION.equalsIgnoreCase(responseType)) {
					processSubmissionResponse(hostName, json);
				} else {
					log.warn("Unrecognized Server Response Type [{}]", responseType);
				}
			}
			
		} catch (Exception e) {
			log.warn("Failed to handle JSON response: {}", e.getMessage());
			log.debug("Failed to handle JSON response: {}", json, e);
		} finally {
			fireResponseEvent(json);
		}
	}
	
	/**
	 * Parses and processes the active check submission response
	 * @param hostName the host name that this active check response is for
	 * @param response The server response to the active check submission
	 * @throws JSONException Thrown if JSON response cannot be parsed
	 */
	protected void processSubmissionResponse(String hostName, JSONObject response) throws JSONException {
		Matcher matcher = INFO_RESPONSE_REGEX.matcher(response.getString(RESPONSE_SUBMISSION));
		if(!matcher.matches()) {
			log.warn("Failed to match expected response with value [{}]", response.toString());
		} else {
			long processed = Long.parseLong(matcher.group(1));
			long failed = Long.parseLong(matcher.group(2));
			long total = Long.parseLong(matcher.group(3));
			float time = Float.parseFloat(matcher.group(1));
			log.info(String.format("\nActive Check Submission Results for [%s]\n\tProcessed:%s\n\tFailed:%s\n\tTotal:\n\tProcess Time:%s\n", hostName, processed, failed, total, time));
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
	 * Executes all the checks in all this server's active hosts
	 * @param os The output stream to write the check results to
	 */
	public void executeChecks(OutputStream os) {			
		for(ActiveHost ah: activeHosts.values()) {
			ah.executeChecks(os);
		}
	}
	
	/**
	 * Executes all the checks in all this server's active hosts for the passed delay
	 * @param delay The delay window
	 * @param os The output stream to write the check results to
	 */
	public void executeChecks(long delay,OutputStream os) {		
		for(ActiveHost ah: activeHosts.values()) {
			ah.executeChecks(delay, os);
		}
	}
	

	/**
	 * Adds a new host to be monitored
	 * @param hostName The name of the host
	 * @param refreshPeriod The frequency in seconds that marching orders should be refreshed for this host
	 */
	public void addActiveHost(String hostName, long refreshPeriod) {
		activeHosts.put(hostName, new ActiveHost(hostName, refreshPeriod, scheduleBucket, address, port));
	}

	/**
	 * Adds a new host to be monitored
	 * @param hostName The name of the host
	 */
	public void addActiveHost(String hostName) {
		addActiveHost(hostName, getRefreshPeriod());
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

	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	
	
}
