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
package org.helios.jzab.plugin.nativex.plugin.generic;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.helios.jzab.plugin.nativex.HeliosSigar;
import org.helios.jzab.plugin.nativex.plugin.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: AbstractMultiCommandProcessor</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor</code></p>
 */

public abstract class AbstractMultiCommandProcessor implements AbstractMultiCommandProcessorMBean {
	/** The processor locator key aliases */
	protected final String[] aliases;
	/** The jzab agent supplied properties */
	protected final Properties props = new Properties();
	/** A map of command invocation wrappers keyed by command name */
	protected final Map<String, ICommandInvoker> invokers = new HashMap<String, ICommandInvoker>();
	/** The sigar native interface */
	protected final HeliosSigar sigar = HeliosSigar.getInstance();
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** This instance's JMX ObjectName */
	protected ObjectName objectName;
	
	/** The jZab identified mbean server */
	protected static final MBeanServer server;
	
	static {
		MBeanServer tmp = null;
		String jmxDomain = System.getProperty(ZABX_DOMAIN_PROP, null);
		if(jmxDomain==null || jmxDomain.trim().isEmpty()) {
			tmp = ManagementFactory.getPlatformMBeanServer();
		} else {			
			for(MBeanServer mbs: MBeanServerFactory.findMBeanServer(null)) {
				if(jmxDomain.equals(mbs.getDefaultDomain())) {
					tmp = mbs;
					break;
				}
			}
			if(tmp==null) {
				tmp = ManagementFactory.getPlatformMBeanServer();
			}
		}
		server = tmp;
	}
	
	
	
	/**
	 * Creates a new AbstractMultiCommandProcessor
	 * @param aliases The processor locator key aliases
	 */
	protected AbstractMultiCommandProcessor(String...aliases) {
		Set<String> als = new HashSet<String>();
		if(aliases!=null) {
			for(String s: aliases) {
				if(s!=null && !s.isEmpty()) {
					als.add(s);
				}
			}
		}
		initializeInvokers();
		als.addAll(invokers.keySet());
		this.aliases = als.toArray(new String[als.size()]);
	}
	
	/**
	 * Registers this instance's MBean interface
	 * @param objectName The object name string
	 */
	protected void register(CharSequence objectName) {
		try {
			this.objectName = new ObjectName(objectName.toString());
			server.registerMBean(this, this.objectName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register MBean with name [" + objectName + "]", e);
		}
	}
	
	/**
	 * Inspects the annotations of all the methods in this class and generates 
	 * invokers for the methods annotated with {@link @CommandHandler}.
	 */
	private void initializeInvokers() {
		processMethods(getClass().getMethods());
		processMethods(getClass().getDeclaredMethods());
	}
	
	/**
	 * Inspects the passed methods for command handlers to register
	 * @param methods An array of methods to inspect
	 */
	private void processMethods(Method[] methods) {
		for(final Method m: methods) {
			try {
				CommandHandler ch = m.getAnnotation(CommandHandler.class);
				if(ch==null) continue;
				// validate signature  
				
				if(!m.getReturnType().equals(String.class)) {
					log.warn("The method [{}] is annotated with @CommandHandler but has a return type of [{}]", m, m.getReturnType().getName());
					continue;
				}
				if(m.getParameterTypes().length!=2) {
					log.warn("The method [{}] is annotated with @CommandHandler but has an invalid parameter count [{}]", m, m.getParameterTypes().length);
					continue;
				}
				Class<?> param0 = m.getParameterTypes()[0];
				Class<?> param1 = m.getParameterTypes()[1];
				if(
						(param1.isArray() && param1.getComponentType().equals(String.class))
						||
						(m.isVarArgs() && param1.equals(String.class))
				&& (param0.equals(String.class)) ) 
				
				{
					log.debug("Qualified method [{}] as  @CommandHandler", m);
					m.setAccessible(true);
					final String key = ch.value();
					final Object invTarget = Modifier.isStatic(m.getModifiers()) ? null : this;
					invokers.put(key, new ICommandInvoker(){
						/**
						 * {@inheritDoc}
						 * @see org.helios.jzab.plugin.nativex.plugin.generic.ICommandInvoker#execute(java.lang.String, java.lang.String[])
						 */
						@Override
						public String execute(String commandName, String... args) {
							try {
								Object result = m.invoke(invTarget, new Object[]{commandName, args});
								return result==null ? "" : result.toString();
							} catch (Exception e) {
								log.warn("Failed to invoke command [{}]:[{}]", key, e.toString());
								return COMMAND_ERROR;								
							}
						}
					});						
				} else {
					log.warn("The method [{}] is annotated with @CommandHandler but has an invalid parameter type [{}]", m, m.getParameterTypes()[0].getName());
					continue;					
				}
			} catch (Exception e) {
				log.debug("Failed to process method [{}]", m, e);
			}
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessorMBean#execute(java.lang.String, java.lang.String[])
	 */
	@Override
	public String execute(String commandName, String... args) {
		return doExecute(commandName, args);
	}
	
	/**
	 * Delegates the execution of the command to a concrete implementation
	 * @param commandName The command name
	 * @param args The command arguments
	 * @return The argument result
	 */
	protected String doExecute(String commandName, String... args) {
		if(commandName==null || commandName.trim().isEmpty()) throw new IllegalArgumentException("The passed command name was null or empty", new Throwable());
		ICommandInvoker invoker = invokers.get(commandName);
		if(invoker==null) {
			return COMMAND_NOT_SUPPORTED;
		}
		try {
			return invoker.execute(commandName, args);
		} catch (Exception e) {
			log.error("Failed to execute command [{}]:[{}]", commandName, e.toString());
			return COMMAND_ERROR;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessorMBean#getLocatorKey()
	 */
	@Override
	public String getLocatorKey() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessorMBean#isDiscovery()
	 */
	@Override
	public boolean isDiscovery() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessorMBean#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties props) {
		if(props!=null) {
			this.props.putAll(props);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessorMBean#init()
	 */
	@Override
	public void init() {

	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessorMBean#getAliases()
	 */
	@Override
	public String[] getAliases() {
		return aliases;
	}

}
