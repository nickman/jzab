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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.AttributeChangeNotification;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.commands.CommandManager;
import org.helios.jzab.agent.commands.ICommandProcessor;
import org.helios.jzab.agent.internal.jmx.ScheduledThreadPoolFactory;
import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.agent.internal.jmx.TrackedScheduledFuture;
import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.agent.net.active.ActiveHost.ActiveHostCheck;
import org.helios.jzab.agent.net.active.collection.IResultCollector;
import org.helios.jzab.agent.net.active.schedule.PassiveScheduleBucket;
import org.helios.jzab.agent.net.routing.JSONResponseHandler;
import org.helios.jzab.agent.net.routing.RoutingObjectName;
import org.helios.jzab.agent.net.routing.RoutingObjectNameFactory;
import org.helios.jzab.util.JMXHelper;
import org.helios.jzab.util.StringHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ActiveHost</p>
 * <p>Description: Represents a host with a list of items that will be actively monitored by the agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ActiveHost</code></p>
 */
public class ActiveHost implements JSONResponseHandler, ActiveHostMXBean, Iterable<ActiveHostCheck>, NotificationBroadcaster {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The host name we're monitoring for */
	protected final String hostName;
	/** The ActiveServer parent for this host */
	protected final ActiveServer server;
	/** The scheduled task to refresh active checks */
	protected TrackedScheduledFuture refreshTask = null;
	/** The frequency in seconds that this host's marching orders should be refreshed */
	protected long refreshPeriod;
	/** The changes in the last refresh */
	protected final LastRefreshChange lastRefreshChange = new LastRefreshChange();
	/** Checks that have been removed */
	protected final Set<String> removedCheckNames = new CopyOnWriteArraySet<String>();
	
	/** The command manager to execute checks */
	protected final CommandManager commandManager = CommandManager.getInstance();
	/** The notification manager */
	protected final NotificationBroadcasterSupport notificationBroadcaster = new NotificationBroadcasterSupport(ThreadPoolFactory.getInstance("NotificationProcessor"));
	/** The notif info */
	protected final MBeanNotificationInfo[] notificationInfos = new MBeanNotificationInfo[]{
			new MBeanNotificationInfo(new String[]{"activehost.statechange"}, AttributeChangeNotification.class.getName(), "Fired when the state of an ActiveHost changes")
	}; 
	/** The sequence number generator for JMX notification sequences */
	protected final AtomicLong notificationSequence = new AtomicLong(0L);
	
	/** This host's JMX ObjectName */
	protected final ObjectName objectName;
	/** The state of this host */
	protected final AtomicReference<ActiveHostState> state = new AtomicReference<ActiveHostState>(ActiveHostState.INIT);
	/** The timestamp of the currently defined state in ms. */
	protected long stateTimestamp = System.currentTimeMillis();
	/** The configured active host checked keyed by item name */
	protected final Map<String, ActiveHostCheck> hostChecks = new ConcurrentHashMap<String, ActiveHostCheck>();
	/** The configured discovery active host checked keyed by item name */
	protected final Map<String, ActiveHostCheck> hostDiscoveryChecks = new ConcurrentHashMap<String, ActiveHostCheck>();
	
	/** The schedule bucket map for this active host */
	protected final PassiveScheduleBucket<ActiveHostCheck, ActiveHost> scheduleBucket; 
	/** The routing object names for this host */
	protected final RoutingObjectName[] routingNames; 
	
	
	/** The JSON key for the active check mtime */
	public static final String CHECK_MTIME = "mtime";
	/** The JSON key for the active check delay */
	public static final String CHECK_DELAY = "delay";
	/** The JSON key for the active check item key */
	public static final String CHECK_ITEM_KEY = "key";
	/** The JSON key for the active check last log size */
	public static final String CHECK_LASTLOG_SIZE = "lastlogsize";
	
	
	
	
	/**
	 * Creates a new ActiveHost
	 * @param server The parent ActiveServer
	 * @param hostName The host name we're monitoring for
	 * @param refreshPeriod The frequency in seconds that this host's marching orders should be refreshed 
	 */
	public ActiveHost(ActiveServer server, String hostName, long refreshPeriod) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("The passed host name was null or blank", new Throwable());
		this.server = server;
		this.hostName = hostName;
		this.refreshPeriod = refreshPeriod;
		scheduleBucket = new PassiveScheduleBucket<ActiveHostCheck, ActiveHost>(server.scheduleBucket, this);
		routingNames = new RoutingObjectName[]{RoutingObjectNameFactory.getInstance().getRoute(true, KEY_REQUEST, VALUE_ACTIVE_CHECK_REQUEST, KEY_HOST, hostName)};
		RoutingObjectNameFactory.getInstance().registerJSONResponseHandler(this);
		log.debug("Created ActiveHost [{}]", hostName);
		objectName = JMXHelper.objectName(new StringBuilder(
				server.getObjectName().toString())
				.append(",host=").append(hostName)
		);
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), objectName, this);
		scheduleNextRefresh();
	}
	
	/**
	 * Returns the scheduled task for the next refresh
	 * @return the refreshTask
	 */
	@Override
	public TrackedScheduledFuture getRefreshTask() {
		return refreshTask;
	}
	
	/**
	 * Schedules the next refresh
	 */
	protected void scheduleNextRefresh() {
		final ActiveHost finalHost = this;
		refreshTask = ScheduledThreadPoolFactory.getInstance("Scheduler").schedule("Refresh Task for [" + getId() + "]", new Runnable(){
			@Override
			public void run() {
				try {
					ActiveAgent.getInstance().requestActiveChecks(server, finalHost, true);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					scheduleNextRefresh();
				}
			}
		}, refreshPeriod, TimeUnit.SECONDS);
	}
	
	/**
	 * Returns a set of ActiveHostChecks for the passed delay
	 * @param delay The delay to get checks for
	 * @return a set of ActiveHostChecks 
	 */
	public Set<ActiveHostCheck> getChecksForDelay(long delay) {
		return Collections.unmodifiableSet(scheduleBucket.get(delay));
	}
	
	/**
	 * Returns the discovery checks for this host
	 * @return A set of discovery checks 
	 */
	public List<ActiveHostCheck> getDiscoveryChecks() {
		return Collections.unmodifiableList(new ArrayList<ActiveHostCheck>(hostDiscoveryChecks.values()));
	}
	
	
	/**
	 * Determines if this active host requires a marching orders refresh
	 * @param currentTime The current time in ms.
	 * @return true if this active host requires a marching orders refresh
	 */
	public boolean requiresRefresh(long currentTime) {
		if(state.get().requiresUpdate()) return true;
		return TimeUnit.SECONDS.convert(currentTime - stateTimestamp, TimeUnit.MILLISECONDS) > refreshPeriod; 
	}
	
	/**
	 * Returns the state of this host
	 * @return the state of this host
	 */
	@Override
	public String getState() {
		return state.get().name();
	}
	
	/**
	 * Returns the effective time of the last state change in seconds
	 * @return the effective time of the last state change in seconds
	 */
	@Override
	public long getStateTimestamp() {
		return TimeUnit.SECONDS.convert(stateTimestamp, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Returns the effective time of the last state change as a java date
	 * @return the effective time of the last state change as a java date
	 */
	@Override
	public Date getStateDate() {
		return new Date(stateTimestamp);
	}
	
	/**
	 * Returns the number of active checks for this host
	 * @return the number of active checks for this host
	 */
	@Override
	public int getActiveCheckCount() {
		return hostChecks.size();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.routing.JSONResponseHandler#jsonResponse(org.helios.jzab.agent.net.routing.RoutingObjectName, org.json.JSONObject)
	 */
	@Override
	public void jsonResponse(RoutingObjectName routing, JSONObject response) throws JSONException {
		log.debug("Handling JSON Response [{}]", response);
		String requestType = routing.getKeyProperty(KEY_REQUEST);
		if(VALUE_ACTIVE_CHECK_REQUEST.equals(requestType)) {
			int[] results = upsertActiveChecks(response.getJSONArray(KEY_DATA));
			log.debug("Active Host [{}] Check Update Results (adds/updates/nochanges/removeds) {}", hostName, Arrays.toString(results)); 
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
	 * Returns a string displaying each schedule bucket delay and the number of checks in each bucket.
	 * @return a string displaying the schedule bucket
	 */
	public String displayScheduleBuckets() {
		StringBuilder sb = new StringBuilder("Schedule Bucket for [");
		sb.append(this.toString()).append("]");
		for(Map.Entry<Long, Set<ActiveHostCheck>> entry: scheduleBucket.entrySet()) {
			sb.append("\n\t").append(entry.getKey()).append(":").append(entry.getValue().size());
		}
		sb.append("\n");
		return sb.toString();
	}
	
	/**
	 * Returns an array of the unique schedule windows for this active host's checks
	 * @return an array of longs representing the unique delays for this active host's checks
	 */
	@Override
	public long[] getDistinctSchedules() {
		Set<Long> setOfTimes = new HashSet<Long>(scheduleBucket.keySet());
		long[] times = new long[setOfTimes.size()];
		int cnt = 0;
		for(Long time: setOfTimes) {
			times[cnt] = time;
			cnt++;
		}
		return times;
	}
	
	/**
	 * Executes all the checks for the passed delay window
	 * @param delay The delay window
	 * @param collector The collector stream to write the results to
	 */
	public void executeChecks(long delay, IResultCollector collector) {
		Set<ActiveHostCheck> checks = scheduleBucket.get(delay);
		for(ActiveHostCheck check: checks) {
			try {
				check.execute(collector);				
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the logging level for this active host
	 * @return the logging level for this active host
	 */
	@Override
	public String getLevel() {
		return LoggerManager.getInstance().getLoggerLevelManager().getLoggerLevel(getClass().getName());
	}
	
	/**
	 * Sets the logger level for this active host
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
	@Override
	public Map<Long, Integer> getScheduleCounts() {
		Map<Long, Integer> map = new HashMap<Long, Integer>(scheduleBucket.size());
		for(Map.Entry<Long, Set<ActiveHostCheck>> entry: scheduleBucket.entrySet()) {
			map.put(entry.getKey(), entry.getValue().size());
		}
		return map;		
	}
	
	/**
	 * Returns the ID of this active host
	 * @return the ID of this active host
	 */
	@Override
	public String getId() {
		return server.getId() + "/" + hostName;
	}
	
	/**
	 * Updates the state of this active host, sending a JMX attribute change notification if this is really a valid state change
	 * @param state The new state
	 */
	protected void setState(ActiveHostState state) {
		if(state==null) throw new IllegalArgumentException("The passed ActiveHostState was null", new Throwable());
		ActiveHostState priorState  = this.state.getAndSet(state);		
		if(priorState!=state) {			
			this.stateTimestamp = SystemClock.currentTimeMillis();
			sendNotification(new AttributeChangeNotification(objectName, notificationSequence.incrementAndGet(), this.stateTimestamp, String.format("State change from [%s] to [%s]", priorState, state), "State", ActiveHostState.class.getName(), priorState.name(), state.name()));					
		}		
	}
	
	/**
	 * Executes all the active checks for this host and submits them
	 */
	@Override
	public void executeChecks() {
		ActiveAgent.getInstance().executeChecks(this);
	}
	
	/**
	 * Requests an updates on active checks assigned to this host (forced)
	 */
	@Override
	public void requestMarchingOrders() {
		ActiveAgent.getInstance().requestActiveChecks(getServer(), this, true);
	}
	
	/**
	 * Executes all the checks for this host
	 * @param collector The result collection stream
	 */
	public void executeChecks(IResultCollector collector) {
		for(ActiveHostCheck check: hostChecks.values()) {
			collector.addResult(check.call());
		}
	}
	 
	/**
	 * Returns the changes made in the last refresh 
	 * @return the changes made in the last refresh
	 */
	@Override
	public LastRefreshChange getLastRefreshChange() {
		return lastRefreshChange;
	}
	
	
	
	/**
	 * Upserts the active checks for this host
	 * @param activeChecks An array of json formatted active checks
	 * @return An array of ints representing the counts of: <ol>
	 * 	<li>The number of checks added<li>
	 *  <li>The number of checks updated<li>
	 *  <li>The number of checks with no change<li>
	 *  <li>The number of checks deleted<li>
	 * </ol>
	 * 
	 */
	public synchronized int[] upsertActiveChecks(JSONArray activeChecks) {
		final long start = SystemClock.currentTimeMillis();
		markAllChecks(true);
		int adds = 0, updates = 0, nochanges = 0;
		try {
			for(int i = 0; i < activeChecks.length(); i++) {
				JSONObject activeCheck = activeChecks.getJSONObject(i);
				String key = activeCheck.getString(CHECK_ITEM_KEY);
				long mtime = activeCheck.getLong(CHECK_MTIME);
				long delay = activeCheck.getLong(CHECK_DELAY);
				ActiveHostCheck ahc = hostChecks.get(key);
				if(ahc==null) {
					// new ActiveHostCheck
					try {
						ahc = new ActiveHostCheck(hostName, key, delay, mtime);
						if(ahc.isDiscovery()) {
							hostDiscoveryChecks.put(key, ahc);					
						} else {
							hostChecks.put(key, ahc);					
						}
						
						log.trace("New ActiveHostCheck [{}]", ahc);
						sendNotification(new Notification("host.activecheck.added", objectName, notificationSequence.incrementAndGet(), this.stateTimestamp, String.format("Removed Active Check [%s]", ahc.itemKey)));
						adds++;
					} catch (Exception e) {
						log.error("Failed to create active host check for host/key [{}]: [{}]", hostName + "/" + key, e.getMessage());
						//log.debug("Failed to create active host check for host/key [{}]", hostName + "/" + key, e);
					}
				} else {
					if(ahc.update(delay, mtime)) {
						// updated ActiveHostCheck
						log.debug("Updated ActiveHostCheck [{}]", ahc);
						sendNotification(new Notification("host.activecheck.updated", objectName, notificationSequence.incrementAndGet(), this.stateTimestamp, String.format("Removed Active Check [%s]", ahc.itemKey)));
						updates++;
					} else {
						// no change ActiveHostCheck
						nochanges++;
					}
					ahc.marked = false;
				}
			}
			int checksRemoved = clearMarkedChecks();
			log.info("Removed [{}] Active Host Checks", checksRemoved);
			setState(ActiveHostState.ACTIVE);
			long elapsed = SystemClock.currentTimeMillis()-start;
			lastRefreshChange.update(elapsed, checksRemoved, adds, updates, nochanges);
			return new int[]{adds, updates , nochanges, checksRemoved };
		} catch (Exception e) {
			log.error("Failed to upsert Active Host Checks [{}]", e.getMessage());
			log.debug("Failed to upsert Active Host Checks for JSON [{}]", activeChecks,  e);
			return null;
		} finally {
			scheduleNextRefresh();
		}
	}
	
	/**
	 * Sets the upsert markerson all active host checks
	 * @param enabled the state to set the markers to 
	 */
	protected void markAllChecks(boolean enabled) {
		for(ActiveHostCheck ac: hostChecks.values()) {
			ac.marked = enabled;
		}
	}
	
	/**
	 * Removes all marked active host checks
	 * @return The number of active host checks removed
	 */
	protected int clearMarkedChecks() {
		Set<ActiveHostCheck> checksToRemove = new HashSet<ActiveHostCheck>();
		for(ActiveHostCheck ac: hostChecks.values()) {
			if(ac.marked) {
				checksToRemove.add(ac);
			}
		}
		for(ActiveHostCheck ac: checksToRemove) {
			hostChecks.remove(ac.itemKey);
			scheduleBucket.removeItem(ac.delay, ac);	
			if(log.isDebugEnabled()) removedCheckNames.add(ac.itemKey);
			sendNotification(new Notification("host.activecheck.removed", objectName, notificationSequence.incrementAndGet(), this.stateTimestamp, String.format("Removed Active Check [%s]", ac.itemKey)));
		}
		return checksToRemove.size();
	}
	
	
	/**
	 * Determines if this active host requires a marching orders refresh
	 * @return true if this active host requires a marching orders refresh
	 */
	public boolean isRequiresRefresh() {
		return requiresRefresh(System.currentTimeMillis());
	}
	
	/**
	 * Returns the refresh period in seconds.
	 * @return the refresh period in seconds.
	 */
	@Override
	public long getRefreshPeriod() {
		return refreshPeriod;
	}



	/**
	 * Sets the refresh period in seconds
	 * @param refreshPeriod the refresh period in seconds
	 */
	@Override
	public void setRefreshPeriod(long refreshPeriod) {
		if(refreshPeriod<2) throw new IllegalArgumentException("Refresh period must be at least 1 second", new Throwable());
		this.refreshPeriod = refreshPeriod;
	}



	/**
	 * Return the host name
	 * @return the hostName
	 */
	@Override
	public String getHostName() {
		return hostName;
	}
	
	/**
	 * Returns a collection of the currently active host checks
	 * @return a collection of the currently active host checks
	 */
	public Set<ActiveHostCheck> getHostChecks() {
		return Collections.unmodifiableSet(new HashSet<ActiveHostCheck>(hostChecks.values()));
	}



	/**
	 * Returns the parent active server
	 * @return the parent active server
	 */
	public ActiveServer getServer() {
		return server;
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ActiveHostCheck> iterator() {		
		return Collections.unmodifiableCollection(hostChecks.values()).iterator();
	}
	
	/**
	 * Returns a set of the names of removed checks. 
	 * Only populated when the logger is in DEBUG.
	 * @return the removed Check Names
	 */
	public Set<String> getRemovedCheckNames() {
		return Collections.unmodifiableSet(removedCheckNames);
	}
	
	/**
	 * Clears the removed check names
	 */
	public void clearRemovedCheckNames() {
		removedCheckNames.clear();
	}
	
	
	public interface ActiveHostCheckMBean {
		public String getItemKey();
		public long getDelay();
		public long getMTime();
		public String getArguments();
		public long getLastExecuteTime();
		public Date getLastExecuteDate();
		public long getLastRefreshTime();
		public Date getLastRefreshDate();
		public String call();
		public boolean isDiscovery();
	}
	
	/**
	 * <p>Title: ActiveHostCheck</p>
	 * <p>Description: Represents an active check on host item being performed on behalf of the zabbix server</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.net.active.ActiveHost.ActiveHostCheck</code></p>
	 */
	public class ActiveHostCheck implements Callable<String>, ActiveHostCheckMBean {
		/** The host name the item being checked  is for */
		protected final String hostName;		
		/** The key of the item being checked */
		protected final String itemKey;
		/** The key of the item being checked escaped */
		protected final String itemKeyEsc;
		/** Indicates if this is a discovery command */
		protected final boolean discovery;
		
		/** The period of the check in seconds */
		protected long delay;
		/** The last mtime of the check (whatever that is)  */
		protected long mtime;
		/** The last time this check was refreshed */
		protected long lastRefreshTime;
		/** The last time this check was executed */
		protected long lastExecuteTime;
		
		/** The update mark, set when upsert starts, cleared on match, deletes this check if still marked on upsert end */
		protected boolean marked = false;
		/** The command processor for this check */
		protected final ICommandProcessor commandProcessor;
		/** The parsed arguments to pass to the command processor for this check */
		protected final String[] processorArguments;
		
		/**
		 * Creates a new ActiveHostCheck
		 * @param hostName The host name the item being checked  is for 
		 * @param itemKey The key of the item being checked
		 * @param delay The period of the check in seconds 
		 * @param mtime The mtime of the active check
		 */
		public ActiveHostCheck(String hostName, String itemKey, long delay, long mtime) {
			this.hostName = hostName;
			this.itemKey = itemKey;
			this.delay = delay;
			this.mtime = mtime;
			lastRefreshTime = System.currentTimeMillis();
			itemKeyEsc = StringHelper.escapeQuotes(this.itemKey);
			String[] ops = commandManager.parseCommandString(itemKey);
			if(ops==null) {
				throw new RuntimeException("Command Manager Failed to parse item key [" + itemKey + "]", new Throwable());
			}
			commandProcessor = commandManager.getCommandProcessor(ops[0]);
			if(commandProcessor==null) {
				throw new RuntimeException("Command Manager Failed to get command processor for name [" + ops[0] + "]", new Throwable());
			}
			discovery = commandProcessor.isDiscovery();
			if(ops.length>1) {
				processorArguments = new String[ops.length-1];
				System.arraycopy(ops, 1, processorArguments, 0, ops.length-1);
			} else {
				processorArguments = CommandManager.EMPTY_ARGS;
			}
			scheduleBucket.addItem(delay, this);			
		}
		
		/** The JSON response template */
		public static final String RESPONSE_TEMPLATE = "{ \"host\": \"%s\", \"key\": \"%s\", \"value\": \"%s\", \"clock\": %s },"; 
		
		
		/**
		 * Executes this check and returns the formated string result
		 * {@inheritDoc}
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public String call()  {
			Object result = commandProcessor.execute(processorArguments);
			return String.format(RESPONSE_TEMPLATE, hostName, itemKeyEsc, StringHelper.escapeQuotes(result.toString()), SystemClock.currentTimeSecs() );
		}

		/**
		 * Executes a discovery check
		 * @return the discovery check result
		 */
		public Object discover() {
			return commandProcessor.execute(processorArguments);
		}

		/**
		 * Indicates if this is a discovery command
		 * @return true if this is a discovery command
		 */
		@Override
		public boolean isDiscovery() {
			return discovery;
		}
		
		/**
		 * Executes this check and writes the result to the passed byte buffer
		 * @param collector The collector stream to write the results to
		 */
		public void execute(IResultCollector collector) {
			lastExecuteTime = collector.getCollectTime();
			collector.addResult(call());
		}
		
		
		/**
		 * Updates the delay and mtime of this check
		 * @param delay The delay
		 * @param mtime The mtime
		 * @return true if either value changed, false otherwise
		 */
		public boolean update(long delay, long mtime) {
			lastRefreshTime = System.currentTimeMillis();
			boolean updated = false;
			if(this.delay!=delay) {
				// removes this check from it's current bucket
				scheduleBucket.removeItem(this.delay, this);
				// update this check's delay
				this.delay=delay;
				// add this check back into the new bucket
				scheduleBucket.addItem(this.delay, this);
				updated = true;
			}
			if(this.mtime!=mtime) {
				this.mtime=mtime;
				updated = true;
			}
			return updated;			
		}


		/**
		 * Returns the period of the check in seconds
		 * @return the delay
		 */
		public long getDelay() {
			return delay;
		}

		/**
		 * Returns the mtime of the check (whatever that is)
		 * @return the mtime
		 */
		public long getMTime() {
			return mtime;
		}

		/**
		 * Sets the period of the check in seconds
		 * @param delay the delay to set
		 */
		public void setDelay(long delay) {
			this.delay = delay;
		}




		/**
		 * Returns the key of the item being checked
		 * @return the item Key
		 */
		public String getItemKey() {
			return itemKey;
		}




		private ActiveHost getOuterType() {
			return ActiveHost.this;
		}


		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("ActiveHostCheck [host=%s, itemKey=%s, delay=%s]",
					hostName, itemKey, delay);
		}


		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((hostName == null) ? 0 : hostName.hashCode());
			result = prime * result
					+ ((itemKey == null) ? 0 : itemKey.hashCode());
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
			ActiveHostCheck other = (ActiveHostCheck) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (hostName == null) {
				if (other.hostName != null) {
					return false;
				}
			} else if (!hostName.equals(other.hostName)) {
				return false;
			}
			if (itemKey == null) {
				if (other.itemKey != null) {
					return false;
				}
			} else if (!itemKey.equals(other.itemKey)) {
				return false;
			}
			return true;
		}


		/**
		 * The last date this check was refreshed 
		 * @return the last Refresh date
		 */
		public Date getLastRefreshDate() {
			return new Date(lastRefreshTime);
		}


		/**
		 * The last time this check was refreshed in long UTC ms.
		 * @return the last Refresh Time
		 */
		public long getLastRefreshTime() {
			return lastRefreshTime;
		}


		/**
		 * The last time this check was executed 
		 * @return the last Execute date
		 */
		public Date getLastExecuteDate() {
			return new Date(lastExecuteTime);
		}


		/**
		 * The last time this check was executed in long UTC ms.
		 * @return the last Execute Time
		 */
		public long getLastExecuteTime() {
			return lastExecuteTime;
		}




		/**
		 * Returns the processor arguments flattened out to a string
		 * @return the processor arguments 
		 */
		public String getArguments() {
			return Arrays.toString(processorArguments);
		}
		
	}



    /**
     * Adds a listener.
     *
     * @param listener The listener to receive notifications.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback An opaque object to be sent back to the
     * listener when a notification is emitted. This object cannot be
     * used by the Notification broadcaster object. It should be
     * resent unchanged with the notification to the listener.
     *
     * @exception IllegalArgumentException thrown if the listener is null.
     *
     * @see #removeNotificationListener
     */
	@Override
	public void addNotificationListener(NotificationListener listener,
			NotificationFilter filter, Object handback) {
		notificationBroadcaster.addNotificationListener(listener, filter,
				handback);
	}



	/**
	 * Returns the notification info for this ActiveHost
	 * @return the notification info for this ActiveHost
	 * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return notificationBroadcaster.getNotificationInfo();
	}



    /**
     * Removes a listener from this MBean.  If the listener
     * has been registered with different handback objects or
     * notification filters, all entries corresponding to the listener
     * will be removed.
     *
     * @param listener A listener that was previously added to this MBean.
     *
     * @exception ListenerNotFoundException The listener is not
     * registered with the MBean.
     *
     * @see #addNotificationListener
     * @see NotificationEmitter#removeNotificationListener
     */
	/**
	 * @param listener A listener that was previously added to this MBean.
	 * @param filter The filter that the listener was registered with
	 * @param handback The handback that the listener was registered with
	 * @throws ListenerNotFoundException The listener is not registered with the MBean.
	 */
	public void removeNotificationListener(NotificationListener listener,
			NotificationFilter filter, Object handback)
			throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener, filter,
				handback);
	}



    /**
     * Removes a listener from this MBean.  If the listener
     * has been registered with different handback objects or
     * notification filters, all entries corresponding to the listener
     * will be removed.
     *
     * @param listener A listener that was previously added to this
     * MBean.
     *
     * @exception ListenerNotFoundException The listener is not registered with the MBean.
     *
     * @see #addNotificationListener
     * @see NotificationEmitter#removeNotificationListener
     */
	@Override
	public void removeNotificationListener(NotificationListener listener)
			throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener);
	}



	/**
	 * Sends a JMX notification
	 * @param notif The notification to send
	 * @see javax.management.NotificationBroadcasterSupport#sendNotification(javax.management.Notification)
	 */
	public void sendNotification(Notification notif) {
		notificationBroadcaster.sendNotification(notif);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ActiveHost [");
		if (hostName != null) {
			builder.append("hostName=");
			builder.append(hostName);
			builder.append(", ");
		}
		if (server != null) {
			builder.append("server=");
			builder.append(server);
		}
		builder.append("]");
		return builder.toString();
	}
	
	public static interface LastRefreshChangeMBean {
		public int getRemoved();
		public int getAdded();
		public int getUpdated();
		public int getNoChange();
		public long getElapsedTime();
		public Date getDate();
	}
	
	public static class LastRefreshChange implements LastRefreshChangeMBean {
		private int removed = 0;
		private int added = 0;
		private int updated = 0;
		private int noChange = 0;
		private long elapsed = 0L;
		private long time = 0L;
		
		/**
		 * Updates in this order: removed, added, updated, noChange
		 * @param values
		 */
		public void update(long elapsed, int...values) {
			removed = values[0];
			added = values[1];
			updated = values[2];
			noChange = values[3];
			time = SystemClock.currentTimeMillis();
			this.elapsed = elapsed;
		}
		
		public long getElapsedTime() {
			return elapsed;
		}
		
		public Date getDate() {
			return new Date(time);
		}
		
		/**
		 * Returns 
		 * @return the removed
		 */
		@Override
		public int getRemoved() {
			return removed;
		}
		/**
		 * Returns 
		 * @return the added
		 */
		@Override
		public int getAdded() {
			return added;
		}
		/**
		 * Returns 
		 * @return the updated
		 */
		@Override
		public int getUpdated() {
			return updated;
		}
		/**
		 * Returns 
		 * @return the noChange
		 */
		@Override
		public int getNoChange() {
			return noChange;
		}
		
		
	}




}
