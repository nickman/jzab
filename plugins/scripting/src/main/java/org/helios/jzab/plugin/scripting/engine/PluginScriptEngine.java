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

import javax.management.ObjectName;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: PluginScriptEngine</p>
 * <p>Description: Container for a script engine implementation and management of it's executed scripts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.PluginScriptEngine</code></p>
 */
public class PluginScriptEngine implements PluginScriptEngineMXBean {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** The singleton instance */
	private static volatile PluginScriptEngine instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The service's JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.agent.plugin.script:type=Plugin,name=PluginScriptEngine");

	/**
	 * Returns the PluginScriptEngine singleton instance
	 * @return the PluginScriptEngine singleton instance
	 */
	public static PluginScriptEngine getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new PluginScriptEngine();
				}
			}
		}
		return instance;
		
	}
	
	/**
	 * Creates a new PluginScriptEngine
	 */
	private PluginScriptEngine() {
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), OBJECT_NAME, this);
		log.info("Started PluginScriptEngine");
		ScriptEngineManager sem = new ScriptEngineManager();
		for(ScriptEngineFactory sef: sem.getEngineFactories()) {
			Engine engine = null;
			try {
				engine = new Engine(sef);
				log.info("Registered Scripting Engine [{}]", engine.objectName);
			} catch (Exception e) {
				log.error("Failed to load ScriptEngine [{}:{}]. Are you missing a dependency ?", sef.getEngineName(), sef.getEngineVersion());
			}
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.PluginScriptEngineMXBean#getLevel()
	 */
	@Override
	public String getLevel() {
		return LoggerManager.getInstance().getLoggerLevelManager().getLoggerLevel(getClass().getName());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.PluginScriptEngineMXBean#setLevel(java.lang.String)
	 */
	@Override
	public void setLevel(String level) {
		LoggerManager.getInstance().getLoggerLevelManager().setLoggerLevel(getClass().getName(), level);
	}	

}
