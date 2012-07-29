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
package org.helios.jzab.agent.net.active.schedule;

/**
 * <p>Title: CommandThreadPolicy</p>
 * <p>Description: Enumerates the policy that specifies how chunks of command executions are apportioned across executing threads</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.CommandThreadPolicy</code></p>
 */

public enum CommandThreadPolicy {
	/** One thread is allocated to execute checks for each configured zabbix server */
	SERVER,
	/** One thread is allocated to execute checks for each configured active host. The default.  */
	HOST,
	/** One thread is allocated to execute checks for each configured active check */
	CHECK;
	
	/**
	 * Decodes the passed string into a CommandThreadPolicy, applying trim and uppercase to the passed value
	 * @param name The name to decode
	 * @return the decoded CommandThreadPolicy
	 */
	public static CommandThreadPolicy forName(CharSequence name) {
		if(name==null || name.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		try {
			return CommandThreadPolicy.valueOf(name.toString().trim().toUpperCase());
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed name [" + name + "] was not a valid CommandThreadPolicy", new Throwable());
		}
	}
}
