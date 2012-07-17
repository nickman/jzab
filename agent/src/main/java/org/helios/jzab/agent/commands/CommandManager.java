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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

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

public class CommandManager implements CommandManagerMXBean {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	/** The map of command processors keyed by command name */
	protected final Map<String, ICommandProcessor> commandProcessors = new ConcurrentHashMap<String, ICommandProcessor>();

	/** The singleton instance */
	protected static volatile CommandManager instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The CommandManager object name */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.agent.command:service=CommandManager");
	
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
	 * Processes a command string and returns the result
	 * @param commandString The command string specified by the zabbix server
	 * @return the result of the command execution
	 */
	public String processCommand(CharSequence commandString) {
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
			return result.toString();
		} catch (Exception e) {
			log.warn("Failed to execute command [{}]", commandString, e);
			return ICommandProcessor.COMMAND_ERROR;
		}
	}
	
	
	
	
}






