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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: LoggerManager</p>
 * <p>Description: Logger manager proxy for slf4j to support changing logger levels at runtime</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.logging.LoggerManager</code></p>
 */

public class LoggerManager {
	/** The JMX ObjectName of the logback JMXManager */
	public static final ObjectName LOGBACK_OBJECT_NAME = JMXHelper.objectName("ch.qos.logback.classic:Name=jzab,Type=ch.qos.logback.classic.jmx.JMXConfigurator");
	/** The class name of the logback Logger implementation */
	public static final String LOGBACK_CLASS = "ch.qos.logback.classic.Logger";
	/** Logback Recognized logger level names */
	public static final Set<String> LOGBACK_LEVEL_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{
			"OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"
	})));
	
	/** The JMX ObjectName of the jdk logging JMXManager */
	public static final ObjectName JDK_OBJECT_NAME = JMXHelper.objectName("java.util.logging:type=Logging");
	/** JDK Logging Recognized logger level names */
	public static final Set<String> JDK_LEVEL_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{
			"SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST"
	})));

	
	/** The LoggerManager singleton instance */
	protected static volatile LoggerManager instance = null;
	/** The LoggerManager singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The determined logger level manager */
	protected final ILoggerLevelManager currentLevelManager;
	
	/**
	 * Acquires the LoggerManager singleton instance
	 * @return the LoggerManager singleton instance
	 */
	public static LoggerManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new LoggerManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new LoggerManager
	 */
	protected LoggerManager() {
		currentLevelManager = sniffLoggerLevelManager();
	}
	
	/**
	 * Returns the logger level manager installed for this agent
	 * @return the logger level manager installed for this agents
	 */
	public ILoggerLevelManager getLoggerLevelManager() {
		return currentLevelManager;
	}

	
	/**
	 * Determines the slf4j logger manager
	 * @return the determined level manager 
	 */
	protected ILoggerLevelManager sniffLoggerLevelManager() {
		if(JMXHelper.getHeliosMBeanServer().isRegistered(LOGBACK_OBJECT_NAME)) {
			return new LogbackLoggerLevelManager();
		}
		return new JDKLoggerLevelManager();
	}
	
}
