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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ActiveHost</p>
 * <p>Description: Represents a host with a list of items that will be actively monitored by the agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ActiveHost</code></p>
 */
public class ActiveHost {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The host name we're monitoring for */
	protected final String hostName;
	/** The configured active host checked keyed by item name */
	protected final Map<String, ActiveHostCheck> hostChecks = new ConcurrentHashMap<String, ActiveHostCheck>();
	
	/**
	 * Creates a new ActiveHost
	 * @param hostName The host name we're monitoring for
	 */
	public ActiveHost(String hostName) {
		if(hostName==null || hostName.trim().isEmpty()) throw new IllegalArgumentException("The passed host name was null or blank", new Throwable());		
		this.hostName = hostName;
		log.debug("Created ActiveHost [{}]", hostName);
	}
	
	
	
	/**
	 * <p>Title: ActiveHostCheck</p>
	 * <p>Description: Represents an active check on host item being performed on behalf of the zabbix server</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.net.active.ActiveHost.ActiveHostCheck</code></p>
	 */
	public class ActiveHostCheck {
		/** The key of the item being checked */
		protected final String itemKey;
		/** The period of the check in seconds */
		protected long delay;
		/** The last value collected for this check */
		protected Object lastValue;
		/** The last time data was successfully collected for this check */
		protected long lastCheck;
		
		
		/**
		 * Creates a new ActiveHostCheck
		 * @param itemKey The key of the item being checked
		 * @param delay The period of the check in seconds 
		 */
		public ActiveHostCheck(String itemKey, long delay) {
			this.itemKey = itemKey;
			this.delay = delay;
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
					+ ((itemKey == null) ? 0 : itemKey.hashCode());
			return result;
		}


		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ActiveHostCheck other = (ActiveHostCheck) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (itemKey == null) {
				if (other.itemKey != null)
					return false;
			} else if (!itemKey.equals(other.itemKey))
				return false;
			return true;
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
			return String.format("ActiveHostCheck [itemKey=%s, delay=%s]",
					itemKey, delay);
		}
		
		
	}
}
