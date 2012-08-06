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
package org.helios.jzab.plugin.nativex.plugin.impls.system;

import java.util.Arrays;

import org.helios.jzab.plugin.nativex.plugin.CommandHandler;
import org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor;



/**
 * <p>Title: AgentCommandPlugin</p>
 * <p>Description: Implements basic agent checks </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.jzab.plugin.system.AgentCommandPlugin</code></p>
 */
public class AgentCommandPlugin extends AbstractMultiCommandProcessor {

	/**
	 * Creates a new AgentCommandPlugin
	 */
	public AgentCommandPlugin() {
		super();
	}

	/**
	 * Returns the fully qualified host name
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the fully qualified host name
	 */
	@CommandHandler("agent.hostname")
	public String getHostName(String commandName, String... args) {
		return sigar.getFQDN();
	}

	/**
	 * Returns the process pid for this agent
	 * @param commandName The command name
	 * @param args The arguments 
	 * @return the process pid for this agent
	 */
	@CommandHandler("agent.pid")
	public String getPid(String commandName, String... args) {
		return "" + sigar.getPid();
	}

	/**
	 * Returns the process startup arguments
	 * @param commandName The command name
	 * @param args The arguments 
	 * @return the process startup arguments
	 */
	@CommandHandler("agent.process.args")
	public String getProcessArgs(String commandName, String... args) {
		return Arrays.toString(sigar.getProcArgs(sigar.getPid()));
	}

}
