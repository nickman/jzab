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
package org.helios.jzab.plugin.scripting.engine.script;

import java.util.Date;

import javax.script.ScriptException;

import org.helios.jzab.agent.commands.IPluginCommandProcessor;
import org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker;

/**
 * <p>Title: ScriptInstanceMBean</p>
 * <p>Description: JMX interface for {@link ScriptInstance}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean</code></p>
 */

public interface ScriptInstanceMBean extends IScriptInvoker, IPluginCommandProcessor {
	
	/**
	 * Simple parameterless invoke with a string return
	 * @return the result of the invoke
	 */
	public String invoke();	

	/**
	 * Returns the source of this script
	 * @return the source
	 */
	public abstract String getSource();

	/**
	 * Sets the source of this script and triggers a recompile if applicable
	 * @param source the source to set
	 */
	public abstract void setSource(String source);

	/**
	 * Returns the UTC timestamp of the last time this source was set
	 * @return the UTC timestamp of the last time this source was set
	 */
	public abstract long getSourceTime();
	
	/**
	 * Returns the last date this source was set
	 * @return the last date this source was set
	 */
	public abstract Date getSourceDate();
	

	/**
	 * Returns the name of this script
	 * @return the name of this script
	 */
	public abstract String getName();
	
	/**
	 * Invokes a script main.
	 * Convenience method to support invocation from JConsole.
	 * @param delim The delimiter to split the passed args into an array
	 * @param args The delimited positional arguments to the script
	 * @return the result of the script execution
	 * @throws ScriptException thrown on script execution errors
	 */
	public Object invoke(String delim, String args) throws ScriptException;
	/**
	 * Invokes a named function in the script
	 * Convenience method to support invocation from JConsole.
	 * @param delim The delimiter to split the passed args into an array
	 * @param functionName The function name
	 * @param args The delimited positional arguments to the script
	 * @return the result of the script execution
	 * @throws ScriptException thrown on script execution errors
	 * @throws NoSuchMethodException Thrown if the named method is not defined
	 */
	public Object invoke(String delim, String functionName, String args) throws ScriptException, NoSuchMethodException;
	
	/**
	 * Invokes a named method in a named object in the script
	 * Convenience method to support invocation from JConsole.
	 * @param delim The delimiter to split the passed args into an array
	 * @param objectName The name of the object in the script that contains the named method
	 * @param methodName The method name
	 * @param args The delimited positional arguments to the script
	 * @return the result of the script execution
	 * @throws ScriptException thrown on script execution errors
	 * @throws NoSuchMethodException Thrown if the named method is not defined
	 */
	public Object invoke(String delim, String objectName, String methodName, String args) throws ScriptException, NoSuchMethodException;
		

}