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

import java.util.Properties;


/**
 * <p>Title: ICommandProcessor</p>
 * <p>Description: Defines an executable command.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.ICommandProcessor</code></p>
 */
public interface ICommandProcessor {
	
	/** Return result for a command when the command is not supported or cannot be executed */
	public static final String COMMAND_NOT_SUPPORTED = "ZBX_NOTSUPPORTED";
	/** Return result for a command when the command fails execution */
	public static final String COMMAND_ERROR = "ZBX_ERROR";
	
    /**
     * Executes the command and returns the result
     * @param args The arguments to the command
     * @return the return value of the executed command
     */
    public Object execute(String...args);
    
    /**
     * Returns the locator key that the command manager will index this processor by
     * to route incoming requests for invocation here
     * @return the command key
     */
    public String getLocatorKey();
    
    /**
     * Indicates if this is a discovery processor
     * @return true if this is a discovery processor, false otherwise
     */
    public boolean isDiscovery();
    
    /**
     * Sets the optional command processor properties
     * @param props The properties to set on a new command processor
     */
    public void setProperties(Properties props);
    
    /**
     * Callback to initialize the command processor after properties have been set
     */
    public void init();

}