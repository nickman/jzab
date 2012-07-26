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
package org.helios.jzab.plugin.nativex.win;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.helios.jzab.plugin.nativex.HeliosSigar;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

/**
 * <p>Title: Windows</p>
 * <p>Description: Simplified constructs for Windows.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.win.Windows</code></p>
 */
public class Windows {
	/** The registry name to retrieve the windows perfmon counters */
	public static final String REGISTRY_PERF_CTRS = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";
	/** The registry name to retrieve the windows perfmon descriptions */
	public static final String REGISTRY_PERF_CTR_HELP = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009\\Help";
	
	private static final Map<String, RegistryKey> RKEYS;
	
	static {
		HeliosSigar.getInstance();
		Map<String, RegistryKey> tmp = new HashMap<String, RegistryKey>(3);
		tmp.put("HKEY_LOCAL_MACHINE", RegistryKey.LocalMachine);
		tmp.put("HKEY_CURRENT_USER", RegistryKey.CurrentUser);
		tmp.put("HKEY_CLASSES_ROOT", RegistryKey.ClassesRoot);
		RKEYS = Collections.unmodifiableMap(tmp);
		
	}
	
	public static String getRegistryString(String key) {
		if(key==null) throw new IllegalArgumentException("The passed registry key was null");
		key = key.trim();
		int firstSlash = key.indexOf('\\');
		if(firstSlash==-1) {
			throw new RuntimeException("The passed registry key [" + key + "] could not be found", new Throwable());
		}
		String rootKey = key.substring(0, firstSlash).toUpperCase();
		key = key.substring(firstSlash+1);
		
		RegistryKey registry = RKEYS.get(rootKey);
		if(registry==null) {
			throw new RuntimeException("The passed registry key root [" + rootKey + "] could not be found", new Throwable());
		}
		try {
			RegistryKey rkey = registry.openSubKey(key);
			ArrayList values = new ArrayList();
			rkey.getMultiStringValue("Counter", values);
			return values.toString();
		} catch (Win32Exception e) {
			throw new RuntimeException("Failed to retrieve value for key [" + key + "]", e); 			
		}
	}
	
	public static void main(String[] args) {
		log("Registry Test");
		String perfCounters = getRegistryString(REGISTRY_PERF_CTRS);
		log(perfCounters);
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
