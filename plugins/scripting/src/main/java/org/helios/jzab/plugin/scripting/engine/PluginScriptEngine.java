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
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.helios.jzab.agent.commands.CommandManager;
import org.helios.jzab.agent.commands.ICommandProcessor;
import org.helios.jzab.agent.commands.IPluginCommandProcessor;
import org.helios.jzab.agent.logging.LoggerManager;
import org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker;
import org.helios.jzab.plugin.scripting.engine.invokers.ScriptInvokerFactory;
import org.helios.jzab.plugin.scripting.engine.script.IScriptUpdateListener;
import org.helios.jzab.plugin.scripting.engine.script.ScriptInstance;
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
public class PluginScriptEngine implements PluginScriptEngineMBean, IScriptUpdateListener {
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
	public static final String OBJECT_NAME_PATTERN = "org.helios.jzab.agent.plugin:type=Plugin,name=%s";
	/** The default plugin name */
	public static final String DEFAULT_PLUGIN_NAME = "ScriptEnginePlugin";

	/**
	 * Returns the default PluginScriptEngine singleton instance
	 * @return the default PluginScriptEngine singleton instance
	 */
	public static PluginScriptEngine get() {
		return get(DEFAULT_PLUGIN_NAME);
	}


	/**
	 * Returns the named PluginScriptEngine singleton instance
	 * @param name The name of the PluginScriptEngine to initialize or acquire
	 * @return the named PluginScriptEngine singleton instance
	 */
	public static PluginScriptEngine get(String name) {
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
		ScriptEngineManager sem = new ScriptEngineManager();
		//Bindings bindings = sem.getBindings();
		for(ScriptEngineFactory sef: sem.getEngineFactories()) {
			Engine engine = null;
			try {
				engine = new Engine(sef, objectName);
				engine.addScriptUpdateListener(this);
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
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), objectName, this);
		log.info("Started PluginScriptEngine [{}]", name);
		
		//sem.setBindings(bindings);
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.PluginScriptEngineMBean#getLevel()
	 */
	@Override
	public String getLevel() {
		return LoggerManager.getInstance().getLoggerLevelManager().getLoggerLevel(getClass().getName());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.PluginScriptEngineMBean#setLevel(java.lang.String)
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
	@Override
	public void addScript(String src, String name, String ext) {
		log.debug("Adding script [{}] of type [{}]", name, ext);
		Engine engine = enginesByExt.get(ext);
		if(engine==null) engine = enginesByMime.get(ext);
		if(engine==null) {
			log.warn("Failed to add script [{}]. No engine for type [{}]", name, ext);
		}
		engine.addScriptInstance(new ScriptInstance(src, name , ScriptInvokerFactory.invokerFor(engine, name, src.toString())));
		
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.IScriptUpdateListener#onScriptSourceChange(org.helios.jzab.plugin.scripting.engine.Engine, org.helios.jzab.plugin.scripting.engine.script.ScriptInstance, boolean)
	 */
	@Override
	public void onScriptSourceChange(Engine engine, ScriptInstance instance, boolean statusOk) {
		log.debug("Updating invoker in engine [{}] for script named [{}]", engine.getEngineName(), instance.getName());
		if(statusOk) {
			invokersByName.put(instance.getName(), instance.getInvoker());
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.PluginScriptEngineMBean#getScriptNames()
	 */
	@Override
	public String[] getScriptNames() {
		return invokersByName.keySet().toArray(new String[invokersByName.size()]);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#execute(java.lang.String, java.lang.String[])
	 */
	@Override
	public Object execute(String commandName, String... args) {
		if(commandName==null || commandName.trim().isEmpty()) return COMMAND_NOT_SUPPORTED;
		IScriptInvoker invoker = invokersByName.get(commandName);
		if(invoker==null) return COMMAND_NOT_SUPPORTED;
		try {
			return invoker.invoke(args);
		} catch (Exception e) {
			log.error("Failed to execute script command [{}]:[{}]", commandName, e.toString());
			log.debug("Failed to execute script command [{}]", commandName, e);
			return COMMAND_ERROR;
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#getLocatorKey()
	 */
	@Override
	public String getLocatorKey() {
		return "ScriptEngine";
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#isDiscovery()
	 */
	@Override
	public boolean isDiscovery() {
		return false;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties props) {
		
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#init()
	 */
	@Override
	public void init() {
//		for(Map.Entry<String, IScriptInvoker> inv: invokersByName.entrySet()) {
//			final String name = inv.getKey();
//			final IScriptInvoker invoker = inv.getValue();			
//			CommandManager.getInstance().registerCommandProcessor(new ICommandProcessor() {
//				@Override
//				public void setProperties(Properties props) {}
//				@Override
//				public boolean isDiscovery() {
//					return invoker.isDiscovery();
//				}
//				@Override
//				public void init() {}
//				@Override
//				public String getLocatorKey() {
//					return name;
//				}
//				@Override
//				public Object execute(String commandName, String... args) {
//					try {
//						return invoker.invoke(args);
//					} catch (Exception e) {
//						return COMMAND_ERROR;
//					}
//				}
//			});
//		}
		
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.IPluginCommandProcessor#getInstance()
	 */
	@Override
	public ICommandProcessor getInstance() {
		return this;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.IPluginCommandProcessor#getAliases()
	 */
	@Override
	public String[] getAliases() {
		return new String[]{};
	}

}
