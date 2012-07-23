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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.commands.CommandManager;
import org.helios.jzab.agent.commands.ICommandProcessor;
import org.helios.jzab.agent.net.codecs.ZabbixConstants;
import org.helios.jzab.util.StringHelper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.json.JSONArray;
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
public class ActiveHost implements Runnable {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The host name we're monitoring for */
	protected final String hostName;
	/** The frequency in seconds that this host's marching orders should be refreshed */
	protected long refreshPeriod;
	/** The command manager to execute checks */
	protected final CommandManager commandManager = CommandManager.getInstance();
	
	/** The state of this host */
	protected final AtomicReference<ActiveHostState> state = new AtomicReference<ActiveHostState>(ActiveHostState.INIT);
	/** The timestamp of the currently defined state in ms. */
	protected long stateTimestamp = System.currentTimeMillis();
	/** The configured active host checked keyed by item name */
	protected final Map<String, ActiveHostCheck> hostChecks = new ConcurrentHashMap<String, ActiveHostCheck>();
	/** The schedule bucket map for this active host */
	protected final Map<Long, Set<ActiveHostCheck>> scheduleBucket = new ConcurrentHashMap<Long, Set<ActiveHostCheck>>();
	
	
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
	 * @param hostName The host name we're monitoring for
	 * @param refreshPeriod The frequency in seconds that this host's marching orders should be refreshed 
	 */
	public ActiveHost(String hostName, long refreshPeriod) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("The passed host name was null or blank", new Throwable());		
		this.hostName = hostName;
		this.refreshPeriod = refreshPeriod;
		log.debug("Created ActiveHost [{}]", hostName);
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
	 * Runnable entry point for executing this host's checks
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		ByteBuffer buffer = ZabbixConstants.collectionBuffer.get();
		Long delay = ZabbixConstants.currentScheduleWindow.get();
		log.debug("Collecting for host [{}] on delay [{}]", hostName, delay);
		for(ActiveHostCheck check:  delay==-1 ? hostChecks.values() : scheduleBucket.get(delay)) {
			if(!check.collect(buffer)) {
				// submit
				check.collect(buffer);
			}
		}		
	}
	
	protected void submit(ByteBuffer bb) {
		ChannelBuffer cb = ChannelBuffers.directBuffer(ZabbixConstants.BASELINE_SIZE + bb.position());
		
		long payloadSize = 0;
		cb.writeBytes(ZabbixConstants.ZABBIX_HEADER);
		cb.writeByte(ZabbixConstants.ZABBIX_PROTOCOL);
		cb.writeBytes(ZabbixConstants.encodeLittleEndianLongBytes(payloadSize));
		cb.writeBytes(bb);
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
	 * @return A string array of all the results
	 */
	public String[] executeChecks(long delay) {
		Set<ActiveHostCheck> checks = scheduleBucket.get(delay);
		if(checks==null || checks.isEmpty()) return new String[]{};
		Set<String> results = new HashSet<String>(checks.size());
		for(ActiveHostCheck check: checks) {
			try {
				results.add(check.call());
			} catch (Exception e) {}
		}
		return results.toArray(new String[results.size()]);
	}
	
	
	/**
	 * Executes all the checks for this host
	 * @return A string array of all the results
	 */
	public String[] executeChecks() {
		Set<String> results = new HashSet<String>(hostChecks.size());
		for(long delay: getDistinctSchedules()) {
			Collections.addAll(results, executeChecks(delay));
		}
		return results.toArray(new String[results.size()]);
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
	 */
	public int[] upsertActiveChecks(JSONArray activeChecks) {
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
						hostChecks.put(key, ahc);					
						log.trace("New ActiveHostCheck [{}]", ahc);
						adds++;
					} catch (Exception e) {
						log.error("Failed to create active host check for host/key [{}]: [{}]", hostName + "/" + key, e.getMessage());
						//log.debug("Failed to create active host check for host/key [{}]", hostName + "/" + key, e);
					}
				} else {
					if(ahc.update(delay, mtime)) {
						// updated ActiveHostCheck
						log.debug("Updated ActiveHostCheck [{}]", ahc);
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
			return new int[]{adds, updates , nochanges, checksRemoved };
		} catch (Exception e) {
			log.error("Failed to upsert Active Host Checks [{}]", e.getMessage());
			log.debug("Failed to upsert Active Host Checks for JSON [{}]", activeChecks,  e);
			return null;
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
			removeCheckFromScheduleBucket(ac);			
		}
		return checksToRemove.size();
	}
	
	/**
	 * Updates the schedule bucket for the passed active host check
	 * @param check The check to update for
	 */
	protected void updateScheduleBucket(ActiveHostCheck check) {
		Set<ActiveHostCheck> checks = scheduleBucket.get(check.delay);
		if(checks==null) {
			checks = new CopyOnWriteArraySet<ActiveHostCheck>();
			scheduleBucket.put(check.delay, checks);
		}
		checks.add(check);
	}
	
	/**
	 * Removes an active check from the schedule bucket
	 * @param check The check to remove
	 * @return true if the removal of this check also resulted in the removal of the delay bucket, false otherwise
	 */
	protected boolean removeCheckFromScheduleBucket(ActiveHostCheck check) {
		Set<ActiveHostCheck> checks = scheduleBucket.get(check.delay);
		if(checks!=null) {
			checks.remove(check);
			if(checks.isEmpty()) {
				scheduleBucket.remove(check.delay);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determines if this active host requires a marching orders refresh
	 * @return true if this active host requires a marching orders refresh
	 */
	public boolean isRequiresRefresh() {
		return requiresRefresh(System.currentTimeMillis());
	}
	
	
	
	
	/**
	 * <p>Title: ActiveHostCheck</p>
	 * <p>Description: Represents an active check on host item being performed on behalf of the zabbix server</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.net.active.ActiveHost.ActiveHostCheck</code></p>
	 */
	public class ActiveHostCheck implements Callable<String> {
		/** The host name the item being checked  is for */
		protected final String hostName;		
		/** The key of the item being checked */
		protected final String itemKey;
		/** The key of the item being checked escaped */
		protected final String itemKeyEsc;
		
		/** The period of the check in seconds */
		protected long delay;
		/** The last mtime of the check (whatever that is)  */
		protected long mtime;
		/** The update mark, set when upsert starts, cleared on match, deletes this check if still marked on upsert end */
		protected boolean marked = false;
		/** The command processor for this check */
		protected final ICommandProcessor commandProcessor;
		/** The parsed arguments to pass to the command processor for this check */
		protected final String[] processorArguments;
		
		/** The last value collected for this check */
		protected Object lastValue;
		/** The last time data was successfully collected for this check */
		protected long lastCheck;
		
		
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
			itemKeyEsc = StringHelper.escapeQuotes(this.itemKey);
			String[] ops = commandManager.parseCommandString(itemKey);
			if(ops==null) {
				throw new RuntimeException("Command Manager Failed to parse item key [" + itemKey + "]", new Throwable());
			}
			commandProcessor = commandManager.getCommandProcessor(ops[0]);
			if(commandProcessor==null) {
				throw new RuntimeException("Command Manager Failed to get command processor for name [" + ops[0] + "]", new Throwable());
			}
			if(ops.length>1) {
				processorArguments = new String[ops.length-1];
				System.arraycopy(ops, 1, processorArguments, 0, ops.length-1);
			} else {
				processorArguments = CommandManager.EMPTY_ARGS;
			}
			updateScheduleBucket(this);
		}
		
		/** The JSON response template */
		public static final String RESPONSE_TEMPLATE = "{ \"host\": \"%s\", \"key\": \"%s\", \"value\": \"%s\", \"clock\": %s }"; 
		
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
		 * Executes this check and writes the result to the passed byte buffer
		 * @param bb The collection byte buffer
		 * @return true if the result was successfully written to the buffer and the buffer was marked. Otherwise, the buffer is reset to it's original mark and false is returned.
		 */
		public boolean collect(ByteBuffer bb) {
			try {
				bb.mark();
				bb.asCharBuffer().append(call());
				bb.mark();
				return true;
			} catch (Exception e) {
				bb.reset();
				return false;
			}
		}
		
		
		/**
		 * Updates the delay and mtime of this check
		 * @param delay The delay
		 * @param mtime The mtime
		 * @return true if either value changed, false otherwise
		 */
		public boolean update(long delay, long mtime) {
			boolean updated = false;
			if(this.delay!=delay) {
				// removes this check from it's current bucket
				removeCheckFromScheduleBucket(this);
				// update this check's delay
				this.delay=delay;
				// add this check back into the new bucket
				updateScheduleBucket(this);
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
		 * Sets the period of the check in seconds
		 * @param delay the delay to set
		 */
		public void setDelay(long delay) {
			this.delay = delay;
		}


		/**
		 * Returns the last value collected for this check
		 * @return the last value collected
		 */
		public Object getLastValue() {
			return lastValue;
		}


		/**
		 * Sets the last value collected for this check
		 * @param lastValue the last value collected to set
		 */
		public void setLastValue(Object lastValue) {
			this.lastValue = lastValue;
		}


		/**
		 * Returns the last time (UTC long) data was successfully collected for this check
		 * @return the last Check time
		 */
		public long getLastCheck() {
			return lastCheck;
		}


		/**
		 * Sets the last time (UTC long) data was successfully collected for this check
		 * @param lastCheck the lastCheck to set
		 */
		public void setLastCheck(long lastCheck) {
			this.lastCheck = lastCheck;
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
		
	}
}
