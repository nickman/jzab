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
package org.helios.jzab.plugin.scripting.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;

/**
 * <p>Title: BindingEntry</p>
 * <p>Description: Simple container to display bindings contents in an MXBean.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.BindingEntry</code></p>
 */
public class BindingEntry implements BindingEntryMBean {
	/** The binding key */
	private final String key;
	/** The binding value */
	private final String value;
	
	/**
	 * Creates a new BindingEntry
	 * @param key The binding key
	 * @param value The binding value
	 */
	private BindingEntry(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	/**
	 * Creates a new BindingEntry
	 * @param key The binding key
	 * @param value The binding value
	 * @return a new BindingEntry
	 */
	public static BindingEntry newInstance(String key, Object value) {
		return new BindingEntry(key, value==null ? null : value.toString());
	}
	
	/**
	 * Creates a new BindingEntry list
	 * @param bindings the binding
	 * @return a list of BindingEntrys
	 */
	public static List<BindingEntry> newInstance(Bindings bindings) {
		List<BindingEntry> list = new ArrayList<BindingEntry>();
		if(bindings != null) {
			for(Map.Entry<String, Object> b: bindings.entrySet()) {
				list.add(newInstance(b.getKey(), b.getValue()));
			}
		}
		return list;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.BindingEntryMBean#getKey()
	 */
	@Override
	public String getKey() {
		return key;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.BindingEntryMBean#getValue()
	 */
	@Override
	public String getValue() {
		return value;
	}
	
	
}
