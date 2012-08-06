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
package org.helios.jzab.plugin.nativex.plugin;

import org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction;

/**
 * <p>Title: RegistrationType</p>
 * <p>Description: Enumerates the configurable registration type for plugin command processors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.RegistrationType</code></p>
 */

public enum RegistrationType {
	/** Plugins are registered directly with the Command Manager */
	DIRECT, 
	/** Plugins are registered as MBeans and expose an instance accessor */
	IPLUGIN, 
	/** Plugins are registered as MBeans but do not implement the core interfaces */
	GENERIC;
	
	/**
	 * Returns the RegistrationType for the passed name. Applies trim and toUpper to the name first.
	 * @param name The name of the type
	 * @return the named RegistrationType 
	 */
	public static RegistrationType forName(CharSequence name) {
		if(name==null) throw new IllegalArgumentException("The passed RegistrationType name was null", new Throwable());
		try {
			return RegistrationType.valueOf(name.toString().trim().toUpperCase());
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed RegistrationType name [" + name + "] is not a valid type name", new Throwable());
		}
	}
		
}
