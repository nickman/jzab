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

import java.util.Map;
import java.util.Set;

import javax.management.MXBean;

import org.helios.jzab.agent.commands.instrumentation.ExecutionMetricMBean;

/**
 * <p>Title: CommandManagerMXBean</p>
 * <p>Description: The CommandManager JMX management interface</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.CommandManagerMXBean</code></p>
 */
@MXBean
public interface CommandManagerMXBean {
	/**
	 * Returns true if execution instrumentation is enabled, false if it is not
	 * @return true if execution instrumentation is enabled, false if it is not
	 */
	public boolean isInstrumentationEnabled();
	/**
	 * Sets the enabled state of execution instrumentation 
	 * @param state true to enable, false to disable and clear any existing metrics
	 */
	public void setInstrumentationEnabled(boolean state);
	/**
	 * Returns a set of current exection metrics
	 * @return a set of current exection metrics
	 */
	public Set<ExecutionMetricMBean> getExecutionMetrics();
	
	/**
	 * Returns the command name and implementation class name for all registered command processors
	 * @return the command name and implementation class name for all registered command processors
	 */
	public Map<String, String> getProcessors();
	
	/**
	 * Invokes the named command and returns the result. 
	 * @param commandName The command name
	 * @param arguments The command arguments optionally wrapped in <code>"["</code> <code>"]"</code>
	 * @return The result of the command execution
	 */
	public String invokeCommand(String commandName, String arguments);
	
	/**
	 * Invokes the named command and returns the result. 
	 * @param commandString The full command string
	 * @return The result of the command execution
	 */
	public String invokeCommand(String commandString);
	
	/**
	 * Parses a command string
	 * @param commandString The command string to parse
	 * @return A string of commands where the first item is the command processor name and the remainder are the arguments.
	 */
	public String[] parseCommandString(String commandString);
}
