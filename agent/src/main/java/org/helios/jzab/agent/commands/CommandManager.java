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
package org.helios.jzab.agent.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.commands.instrumentation.ExecutionMetric;
import org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean;
import org.helios.jzab.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVParser;

/**
 * <p>Title: CommandManager</p>
 * <p>Description: The main command repository to index, resolve requests and manage execution of agent commands.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.CommandManager</code></p>
 */

public class CommandManager implements CommandManagerMXBean, NotificationListener, NotificationFilter  {
	/**  */
	private static final long serialVersionUID = -3028399370725973533L;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The map of command processors keyed by command name */
	protected final Map<String, ICommandProcessor> commandProcessors = new ConcurrentHashMap<String, ICommandProcessor>();
	/** The map of plugin command processor locator keys keyed by ObjectName */
	protected final Map<ObjectName, String> pluginRegistry = new ConcurrentHashMap<ObjectName, String>();
	/** Indicates if execution instrumentation is enabled */
	protected final boolean[] instrumentation = new boolean[]{true};

	/** The singleton instance */
	protected static volatile CommandManager instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The CommandManager object name */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.agent.command:service=CommandManager");
	/** The ObjectName pattern for plugins that may register themselves with the CommandManager */
	public static final ObjectName PLUGIN_OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.agent.plugin:type=Plugin,*");
	
	
	/** Empty string array */
	public static final String[] EMPTY_ARGS = {};
	
	/**
	 * Acquires the command manager singleton instance
	 * @return the command manager singleton instance
	 */
	public static CommandManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new CommandManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new CommandManager and registers its management interface.
	 */
	protected CommandManager() {
		log.info("Created CommandManager");
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), OBJECT_NAME, this);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.CommandManagerMXBean#isInstrumentationEnabled()
	 */
	@Override
	public boolean isInstrumentationEnabled() {		
		return instrumentation[0];
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.CommandManagerMXBean#setInstrumentationEnabled(boolean)
	 */
	@Override
	public void setInstrumentationEnabled(boolean state) {
		instrumentation[0] = state;
		if(state==false) {
			ExecutionMetric.clear();
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.CommandManagerMXBean#getExecutionMetrics()
	 */
	@Override
	public Set<ExecutionMetricMBean> getExecutionMetrics() {
		return ExecutionMetric.getExecutionMetrics();
	}
	
	
	/**
	 * Registers a new Command Processor
	 * @param commandProcessor the processor to register
	 * @param aliases An optional array of aliases for this command processor
	 */
	public void registerCommandProcessor(ICommandProcessor commandProcessor, String...aliases) {
		
		if(commandProcessor==null) throw new IllegalArgumentException("The passed command processor was null", new Throwable());
		Set<String> keys = new HashSet<String>();
		keys.add(commandProcessor.getLocatorKey());
		if(aliases!=null) {
			for(String s: aliases) {
				if(s!=null && !s.trim().isEmpty()) {
					keys.add(s.trim().toLowerCase());
				}
			}
		}
		for(String key: keys) {			
			if(!commandProcessors.containsKey(key)) {
				synchronized(commandProcessors) {
					if(!commandProcessors.containsKey(key)) {
						commandProcessors.put(key, commandProcessor);
						continue;
					}
				}
			}
			throw new RuntimeException("The command processor [" + key + "] was already registered");
		}
	}
	
	/**
	 * Returns the named command processor
	 * @param processorName The name of the command processor to retrieve
	 * @return the named command processor or null if one was not found
	 */
	public ICommandProcessor getCommandProcessor(String processorName) {
		if(processorName==null || processorName.trim().isEmpty()) throw new IllegalArgumentException("The passed processor name was null or empty", new Throwable());
		return wrap(commandProcessors.get(processorName), processorName);
	}
	
	protected ICommandProcessor wrap(final ICommandProcessor cp, final String processorName) {
		return new ICommandProcessor() {
			public Object execute(String... args) {
				long start = instrumentation[0] ? SystemClock.currentTimeMillis() : 0;
				Object result = cp.execute(args);
				if(instrumentation[0]) {
					ExecutionMetric.submit(processorName, SystemClock.currentTimeMillis()-start);
				}
				return result;
			}
			public String getLocatorKey() {
				return cp.getLocatorKey();
			}
			public boolean isDiscovery() {
				return cp.isDiscovery();
			}
			public void setProperties(Properties props) {
				cp.setProperties(props);
			}
			public void init() {
				cp.init();
			}
		};
	}
	
	/**
	 * Parses a command string
	 * @param commandString The command string to parse
	 * @return A string of commands where the first item is the command processor name and the remainder are the arguments.
	 */
	public String[] parseCommandString(CharSequence commandString) {
			if(commandString==null) return null;
			String cstring = commandString.toString().trim();
			if(cstring.isEmpty()) return null;
			int length = cstring.length();
			int paramOpener = cstring.indexOf('[');
			
			if(paramOpener==-1 || cstring.charAt(length-1)!=']') {
				return new String[]{cstring.toLowerCase()};
			}
			List<String> ops = new ArrayList<String>();
			ops.add(cstring.substring(0, paramOpener).toLowerCase());
			try {
				Collections.addAll(ops, new CSVParser(',', '"').parseLine(cstring.substring(paramOpener+1, length-1).trim()));
			} catch (IOException e) {
				log.error("Failed to parse arguments in command string \n\t-->{}<--", commandString, e);
				return null;
			}
			return ops.toArray(new String[ops.size()]);
	}
	
	/**
	 * Processes a command string and returns the result
	 * @param commandString The command string specified by the zabbix server
	 * @return the result of the command execution
	 */
	public String processCommand(CharSequence commandString) {
		long start = instrumentation[0] ? SystemClock.currentTimeMillis() : -1L;
		if(commandString==null) return ICommandProcessor.COMMAND_ERROR;
		String cstring = commandString.toString().trim();
		if(cstring.isEmpty()) return ICommandProcessor.COMMAND_ERROR;
		int length = cstring.length();
		int paramOpener = cstring.indexOf('[');
		String[] strArgs = null;
		String commandName = null;
		if(paramOpener==-1 || cstring.charAt(length-1)!=']') {
			strArgs = EMPTY_ARGS;
			commandName = cstring.toLowerCase();
		} else {
			commandName = cstring.substring(0, paramOpener).toLowerCase();
			try {
				strArgs = new CSVParser(',', '"').parseLine(cstring.substring(paramOpener+1, length-1).trim());
				log.debug("Command [{}] with arguments {}", commandName, Arrays.toString(strArgs));
				//strArgs = cstring.substring(paramOpener+1, length-1).trim().split("\\|");
			//} catch (Exception e) {
			} catch (IOException e) {
				log.error("Failed to parse arguments in command string [{}]", commandString, e);
				return ICommandProcessor.COMMAND_ERROR;
			}
		}
		ICommandProcessor cp = commandProcessors.get(commandName);
		if(cp==null) {
			log.debug("No command registered called [{}]", commandName);
			return ICommandProcessor.COMMAND_NOT_SUPPORTED;
		}
		try {
			Object result =  cp.execute(strArgs);
			if(result==null) {
				log.warn("Null result executing command [{}]", commandString);
				return ICommandProcessor.COMMAND_NOT_SUPPORTED;				
			}
			if(instrumentation[0]) {
				ExecutionMetric.submit(commandName, SystemClock.currentTimeMillis()-start);
			}
			return result.toString();
		} catch (Exception e) {
			log.warn("Failed to execute command [{}]", commandString, e);
			return ICommandProcessor.COMMAND_ERROR;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.CommandManagerMXBean#getProcessors()
	 */
	public Map<String, String> getProcessors() {
		Map<String, String> map = new HashMap<String, String>(commandProcessors.size());
		for(Map.Entry<String, ICommandProcessor> entry: commandProcessors.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getClass().getName());
		}
		return map;
	}
	
	/**
	 * Registers a new command processor
	 * @param objectName The JMX ObjectName of the new command processor
	 */
	protected void registerPlugin(ObjectName objectName) {
		if(objectName==null) throw new IllegalArgumentException("The passed objectName was null", new Throwable());
		log.debug("Registering plugin at [{}]", objectName);
		if(!JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			throw new IllegalStateException("The plugin ObjectName [" + objectName + "] is not registered", new Throwable());
		}
		try {
			boolean isPluginCommandProcessor = JMXHelper.getHeliosMBeanServer().isInstanceOf(objectName, IPluginCommandProcessor.class.getName());
			log.debug("Plugin at [{}] is command processor:{}", objectName, isPluginCommandProcessor);
			if(isPluginCommandProcessor) {
				try {
					IPluginCommandProcessor pluginProcessor = (IPluginCommandProcessor)JMXHelper.getAttribute(JMXHelper.getHeliosMBeanServer(), objectName, "Instance");
					registerCommandProcessor(pluginProcessor, pluginProcessor.getLocatorKey());
					pluginRegistry.put(objectName, pluginProcessor.getLocatorKey());
					log.info("Registered Plugin CommandProcessor [{}]", pluginProcessor.getLocatorKey());
					return;
				} catch (Exception e) {
					log.warn("Failed to retrieve instance of IPluginCommandProcessor for [{}]. Will attempt proxy", objectName);					
				}
			}
			try {
				ICommandProcessor processor = (IPluginCommandProcessor)MBeanServerInvocationHandler.newProxyInstance(JMXHelper.getHeliosMBeanServer(), objectName, ICommandProcessor.class, false);
				registerCommandProcessor(processor, processor.getLocatorKey());
				pluginRegistry.put(objectName, processor.getLocatorKey());
				log.info("Registered Proxy Command Processor [{}]", processor.getLocatorKey());
				return;
			} catch (Exception e) {
				log.warn("Failed to register Proxy CommandProcessor for [{}]", objectName);					
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to register plugin at ObjectName [" + objectName + "]");
		}
	}
	
	/**
	 * Unregisters a command processor
	 * @param objectName The JMX ObjectName of the command processor that was unregistered
	 */
	protected void unRegisterPlugin(ObjectName objectName) {
		if(objectName==null) throw new IllegalArgumentException("The passed objectName was null", new Throwable());
		log.debug("Unregistering plugin at [{}]", objectName);
		String key = pluginRegistry.remove(objectName);
		if(key==null || commandProcessors.remove(key)==null) {
			log.warn("No command processor found to unregister for key [{}] for ObjectName [{}]", key, objectName);
		} else {
			log.info("Unregistered command processor [{}] for ObjectName [{}]", key, objectName);
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		if(notification instanceof MBeanServerNotification) {
			MBeanServerNotification msn = (MBeanServerNotification)notification;
			if(MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(msn.getType())) {
				registerPlugin(msn.getMBeanName());
			} else if(MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(msn.getType())) {
				unRegisterPlugin(msn.getMBeanName());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		if(notification instanceof MBeanServerNotification) {
			MBeanServerNotification msn = (MBeanServerNotification)notification;
			if(PLUGIN_OBJECT_NAME.apply(msn.getMBeanName())) {
				if(MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(msn.getType())) {
					return true;
				} else if(MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(msn.getType())) {
					return true;
				}
				return false;
			}
		}
		return false;
	}

	
	
	
	
	
}






