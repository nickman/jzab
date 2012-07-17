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

/**
 * <p>Title: AbstractCommandProcessor</p>
 * <p>Description: Abstract base class for command processor implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.AbstractCommandProcessor</code></p>
 */

public abstract class AbstractCommandProcessor implements ICommandProcessor {
	
	/*
		jmx["java.lang:type=Compilation",TotalCompilationTime]
		java.ping	 
	 */

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#execute(java.lang.String[])
	 */
	@Override
	public Object execute(String... args) {
		try {
			return doExecute(args);
		} catch (Exception e) {
			return "ZBX_NOTSUPPORTED";
		}		
	}
	
	/**
	 * Delegate to concrete implementations
	 * @param args The command arguments 
	 * @return the result of the command
	 */
	protected abstract Object doExecute(String... args);
}
