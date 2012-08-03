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
package org.helios.jzab.agent.commands.impl.system;

import java.lang.management.ManagementFactory;

import org.helios.jzab.agent.commands.AbstractCommandProcessor;

/**
 * <p>Title: SystemNameCommandProcessor</p>
 * <p>Description: Simple system name command processor that returns the system name</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.system.SystemNameCommandProcessor</code></p>
 */
public class SystemNameCommandProcessor extends AbstractCommandProcessor {
	/** The system name */
	public static final String systemName = ManagementFactory.getRuntimeMXBean().getName().split("@")[1];
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#getLocatorKey()
	 */
	@Override
	public String getLocatorKey() {
		return "system.uname";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.AbstractCommandProcessor#doExecute(java.lang.String, java.lang.String[])
	 */
	@Override
	protected Object doExecute(String commandName, String... args) throws Exception {	
		log.info("\n\t===================================\n\tProcessing SystemName\n\tResult [{}]\n\t===================================\n", systemName);
		return systemName;
	}

}
