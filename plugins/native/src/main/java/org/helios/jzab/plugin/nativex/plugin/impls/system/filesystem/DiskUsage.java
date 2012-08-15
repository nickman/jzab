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
package org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: DiskUsage</p>
 * <p>Description: Enumerates the available disk type metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskUsage</code></p>
 */
public enum DiskUsage {
	DISKREADS("DiskReads"),
	USED("Used"),
	AVAILABLE("Avail"),
	DISKQUEUE("DiskQueue"),
	DISKREADBYTES("DiskReadBytes"),
	DISKSERVICETIME("DiskServiceTime"),
	DISKWRITEBYTES("DiskWriteBytes"),
	FREE("Free"),
	TOTAL("Total"),
	USEDPERCENT("UsePercent"),
	DISKWRITES("DiskWrites");
	
	private static final Map<String, DiskUsage> ALIASES = new HashMap<String, DiskUsage>(24);
	
	
	static {
		for(DiskUsage du: DiskUsage.values()) {
			ALIASES.put(du.internal.toLowerCase(), du);
			for(String alias: du.aliases) {
				ALIASES.put(alias.toLowerCase(), du);
			}
		}
	}

	public static DiskUsage forValue(CharSequence name) {
		if(name==null) throw new IllegalArgumentException("The passed name was null", new Throwable());
		DiskUsage du = ALIASES.get(name.toString().trim().toLowerCase());
		if(du==null) throw new IllegalArgumentException("The passed name was not a valid DiskUsage [" + name + "]", new Throwable());
		return du;
	}
	
	public static boolean isValidDiskUsage(CharSequence name) {
		if(name==null) return false;
		else return ALIASES.get(name.toString().trim().toLowerCase())!=null;
	}
	
	private DiskUsage(String internal, String...aliases) {
		this.aliases = aliases;
		this.internal = internal;
	}
	
	private final String[] aliases;
	private final String internal;
	/**
	 * @return the internal
	 */
	public String getInternal() {
		return internal;
	}

}
