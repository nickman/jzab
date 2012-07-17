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
package org.helios.jzab.agent.commands.impl;

import org.helios.jzab.agent.commands.AbstractCommandProcessor;

/**
 * <p>Title: CoreJMXAttributesCommandProcessor</p>
 * <p>Description: The core JMX command processor for standard attribute getters.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.CoreJMXAttributesCommandProcessor</code></p>
 */

public class CoreJMXAttributesCommandProcessor extends AbstractCommandProcessor {
	/** This processors command keys */
	protected static final String COMMAND_KEY  = "jmxattr"; 
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#getLocatorKey()
	 */
	@Override
	public String getLocatorKey() {
		return COMMAND_KEY;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.AbstractCommandProcessor#doExecute(java.lang.String[])
	 */
	@Override
	protected Object doExecute(String... args) {
		// TODO Auto-generated method stub
		return null;
	}

}
