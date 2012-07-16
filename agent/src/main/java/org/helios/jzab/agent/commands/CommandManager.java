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

import javax.management.ObjectName;

import org.helios.jzab.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	/** The singleton instance */
	protected static volatile CommandManager instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The CommandManager object name */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.agent.command:service=CommandManager");
	
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
		
	}
}
