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

/**
 * <p>Title: ActiveHostState</p>
 * <p>Description: Enumerates the states of an {@link ActiveHost} with respect to it's marching orders from the zabbix server.	</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ActiveHostState</code></p>
 */

public enum ActiveHostState {
	/** The host was just initialized */
	INIT,
	/** The request for marching orders from the zabbix server failed */
	MO_FAILED,
	/** The zabbix server did not recognize the host name */
	NO_HOST_DEF,
	/** Marching Orders have been defined and are fresh */
	ACTIVE(false),
	/** Marching Orders have been defined but are stale */
	STALE;
	
	private ActiveHostState(boolean requiresUpdate) {
		this.requiresUpdate = requiresUpdate;
	}
	
	private ActiveHostState() {
		this.requiresUpdate = true;
	}
	
	
	private final boolean requiresUpdate;
	
	/**
	 * Indicates if this state requires a marching orders update
	 * @return true if this state requires a marching orders update, false otherwise
	 */
	public boolean requiresUpdate() {
		return requiresUpdate;
	}
}
