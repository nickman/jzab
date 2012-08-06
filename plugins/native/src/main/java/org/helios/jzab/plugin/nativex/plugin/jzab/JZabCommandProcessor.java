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
package org.helios.jzab.plugin.nativex.plugin.jzab;

import java.util.Properties;

import org.helios.jzab.agent.commands.ICommandProcessor;
import org.helios.jzab.agent.commands.IPluginCommandProcessor;
import org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor;

/**
 * <p>Title: JZabCommandProcessor</p>
 * <p>Description: Base command processor class for native plugins</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.jzab.plugin.AbstractCommandProcessor</code></p>
 */
public class JZabCommandProcessor implements IPluginCommandProcessor {
	/** The native plugin processor to wrap */
	protected final AbstractMultiCommandProcessor wrappedProcessor;

	/**
	 * Creates a new JZabCommandProcessor
	 * @param wrappedProcessor The processor to wrap
	 * @return a new JZabCommandProcessor
	 */
	public static JZabCommandProcessor wrap(AbstractMultiCommandProcessor wrappedProcessor) {
		return new JZabCommandProcessor(wrappedProcessor);
	}
	
	/**
	 * Creates a new JZabCommandProcessor
	 * @param wrappedProcessor The native plugin processor to wrap 
	 */
	public JZabCommandProcessor(AbstractMultiCommandProcessor wrappedProcessor) {
		this.wrappedProcessor = wrappedProcessor;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#execute(java.lang.String, java.lang.String[])
	 */
	@Override
	public Object execute(String commandName, String... args) {
		return wrappedProcessor.execute(commandName, args);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#getLocatorKey()
	 */
	@Override
	public String getLocatorKey() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#isDiscovery()
	 */
	@Override
	public boolean isDiscovery() {
		return wrappedProcessor.isDiscovery();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties props) {
		wrappedProcessor.setProperties(props);
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#init()
	 */
	@Override
	public void init() {
		wrappedProcessor.init();
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.IPluginCommandProcessor#getInstance()
	 */
	@Override
	public ICommandProcessor getInstance() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.IPluginCommandProcessor#getAliases()
	 */
	@Override
	public String[] getAliases() {
		return wrappedProcessor.getAliases();
	}
}
