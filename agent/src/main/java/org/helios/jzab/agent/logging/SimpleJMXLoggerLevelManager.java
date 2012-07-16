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
package org.helios.jzab.agent.logging;

import java.util.Set;

import javax.management.ObjectName;

import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: SimpleJMXLoggerLevelManager</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.logging.SimpleJMXLoggerLevelManager</code></p>
 */

public class SimpleJMXLoggerLevelManager implements ILoggerLevelManager {
	/** The set level action name */
	protected final String setActionName;
	/** The get level action name */
	protected final String getActionName;
	/** The JMX MBean's ObjectName */
	protected final ObjectName objectName;
	/** The valid level names in upper case */
	protected final Set<String> validLevels;
	
	
	
	

	/**
	 * Creates a new SimpleJMXLoggerLevelManager
	 * @param setActionName The set level action name
	 * @param getActionName The get level action name
	 * @param objectName The JMX MBean's ObjectName
	 * @param validLevels The valid level names in upper case 
	 */
	public SimpleJMXLoggerLevelManager(String setActionName, String getActionName, ObjectName objectName, Set<String> validLevels) {
		this.setActionName = setActionName;
		this.getActionName = getActionName;
		this.objectName = objectName;
		this.validLevels = validLevels;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.logging.ILoggerLevelManager#setLoggerLevel(java.lang.String, java.lang.String)
	 */
	@Override
	public void setLoggerLevel(String name, String level) {
		if(name==null) throw new IllegalArgumentException("The passed logger name was null", new Throwable());
		if(level==null) throw new IllegalArgumentException("The passed logger level was null", new Throwable());
		level = level.trim().toUpperCase();
		if(!validLevels.contains(level)) {
			throw new IllegalArgumentException("Invalid level name for Logging [" + level + "] against [" + objectName + "]", new Throwable());
		}		
		JMXHelper.invoke(objectName, JMXHelper.getHeliosMBeanServer(), setActionName, new Object[]{name, level}, new String[]{String.class.getName(), String.class.getName()});
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.logging.ILoggerLevelManager#getLoggerLevel(java.lang.String)
	 */
	@Override
	public String getLoggerLevel(String name) {
		if(name==null) throw new IllegalArgumentException("The passed logger name was null", new Throwable());
		return (String)JMXHelper.invoke(objectName, JMXHelper.getHeliosMBeanServer(), getActionName, new Object[]{name}, new String[]{String.class.getName()});		
	}

}
