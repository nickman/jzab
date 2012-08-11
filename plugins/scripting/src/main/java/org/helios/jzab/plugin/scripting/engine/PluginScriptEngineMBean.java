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

import org.helios.jzab.agent.commands.IPluginCommandProcessor;


/**
 * <p>Title: PluginScriptEngineMBean</p>
 * <p>Description: JMX interface for {@link PluginScriptEngine}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.PluginScriptEngineMBean</code></p>
 */
public interface PluginScriptEngineMBean extends IPluginCommandProcessor {
	/**
	 * Returns the logging level for this active agent listener
	 * @return the logging level for this active agent
	 */
	public String getLevel();
	
	/**
	 * Sets the logger level for this active agent
	 * @param level The level to set this logger to
	 */
	public void setLevel(String level);
	
	/**
	 * Adds a new script
	 * @param src The source
	 * @param name The name of the script
	 * @param ext The extension, or in a pinch, the mime-type
	 */
	public void addScript(String src, String name, String ext);	
	
	/**
	 * Returns the registered script names
	 * @return the registered script names
	 */
	public String[] getScriptNames();

	

}
