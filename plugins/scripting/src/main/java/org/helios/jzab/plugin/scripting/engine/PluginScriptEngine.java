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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker;
import org.helios.jzab.plugin.scripting.engine.invokers.ScriptInvokerFactory;
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
	/** The name of this plugin script engine instance */
	protected final String name;
	/** The ObjectName for this plugin */
	protected final ObjectName objectName;
	/** A map of engines keyed by their supported extensions */
	protected final ConcurrentHashMap<String, Engine> enginesByExt = new ConcurrentHashMap<String, Engine>();
	/** A map of engines keyed by their supported mime types */
	protected final ConcurrentHashMap<String, Engine> enginesByMime = new ConcurrentHashMap<String, Engine>();
	/** A map of script invokers keyed by their advertised name */
	protected final ConcurrentHashMap<String, IScriptInvoker> invokersByName = new ConcurrentHashMap<String, IScriptInvoker>();
	
	
	/** The registered script engines keyed by name */
	private static final Map<String, PluginScriptEngine> scriptEngines = new ConcurrentHashMap<String, PluginScriptEngine>();
	
	
	/** The ObjectName template for this script engine plugin instance */
	public static final String OBJECT_NAME_PATTERN = "org.helios.jzab.agent.plugin.script:type=Plugin,name=%s";
	/** The default plugin name */
	public static final String DEFAULT_PLUGIN_NAME = "ScriptEnginePlugin";

	/**
	 * Returns the default PluginScriptEngine singleton instance
	 * @return the default PluginScriptEngine singleton instance
	 */
	public static PluginScriptEngine getInstance() {
		return getInstance(DEFAULT_PLUGIN_NAME);
	}


	/**
	 * Returns the named PluginScriptEngine singleton instance
	 * @param name The name of the PluginScriptEngine to initialize or acquire
	 * @return the named PluginScriptEngine singleton instance
	 */
	public static PluginScriptEngine getInstance(String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed plugin script engine name was null or empty", new Throwable());
		PluginScriptEngine plugin = scriptEngines.get(name);
		if(plugin==null) {
			synchronized(scriptEngines) {
				plugin = scriptEngines.get(name);
				if(plugin==null) {
					plugin = new PluginScriptEngine(name);
					scriptEngines.put(name, plugin);
				}
			}
		}
		return plugin;
		
	}
	
	/**
	 * Creates a new PluginScriptEngine
	 * @param name The name of the PluginScriptEngine
	 */
	private PluginScriptEngine(String name) {
		this.name = name;
		objectName = JMXHelper.objectName(String.format(OBJECT_NAME_PATTERN, name));
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), objectName, this);
		log.info("Started PluginScriptEngine [{}]", name);
		ScriptEngineManager sem = new ScriptEngineManager();
		//Bindings bindings = sem.getBindings();
		for(ScriptEngineFactory sef: sem.getEngineFactories()) {
			Engine engine = null;
			try {
				engine = new Engine(sef, objectName);
				sem.put(engine.getObjectName().toString(), engine);
				for(String ext: engine.getExtensions()) {
					if(enginesByExt.putIfAbsent(ext, engine)!=null) {
						log.warn("Engine [{}] advertised extension [{}] but was already registered by another engine", engine.getEngineName(), ext);
					}
				}
				for(String mime: engine.getMimeTypes()) {
					if(enginesByMime.putIfAbsent(mime, engine)!=null) {
						log.warn("Engine [{}] advertised mime-type [{}] but was already registered by another engine", engine.getEngineName(), mime);
					}
				}
				
				log.info("Registered Scripting Engine [{}]", engine.objectName);
			} catch (Throwable e) {
				log.error("Failed to load ScriptEngine [{}:{}]. Are you missing a dependency ?", sef.getEngineName(), sef.getEngineVersion());
			}
		}
		//sem.setBindings(bindings);
		
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
	
	
	/**
	 * Adds a new script
	 * @param src The source
	 * @param name The name of the script
	 * @param ext The extension, or in a pinch, the mime-type
	 */
	public void addScript(CharSequence src, String name, String ext) {
		log.debug("Adding script [{}] of type [{}]", name, ext);
		Engine engine = enginesByExt.get(ext);
		if(engine==null) engine = enginesByMime.get(ext);
		if(engine==null) {
			log.warn("Failed to add script [{}]. No engine for type [{}]", name, ext);
		}
		IScriptInvoker invoker = ScriptInvokerFactory.invokerFor(engine, src.toString());
		
	}

}
