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
package org.helios.jzab.plugin.nativex.jzab.plugin;

import java.util.Properties;

import org.helios.jzab.agent.commands.ICommandProcessor;
import org.helios.jzab.agent.commands.IPluginCommandProcessor;
import org.helios.jzab.plugin.nativex.HeliosSigar;

/**
 * <p>Title: AbstractCommandProcessor</p>
 * <p>Description: Base command processor class for native plugins</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.jzab.plugin.AbstractCommandProcessor</code></p>
 */
public abstract class AbstractCommandProcessor implements IPluginCommandProcessor {
	/** The processor locator key */
	protected final String name;
	/** The processor locator key aliases */
	protected final String[] aliases;
	/** The jzab agent supplied properties */
	protected final Properties props = new Properties();
	
	/** The sigar native interface */
	protected final HeliosSigar sigar = HeliosSigar.getInstance();
	
	/**
	 * Creates a new AbstractCommandProcessor
	 * @param name The processor locator key
	 * @param aliases The processor locator key aliases
	 */
	protected AbstractCommandProcessor(String name, String...aliases) {
		this.name = name;
		if(aliases==null) {
			this.aliases = new String[]{};
		} else {
			this.aliases = aliases;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#execute(java.lang.String[])
	 */
	@Override
	public String execute(String... args) {
		return doExecute(args);
	}
	
	/**
	 * @param args
	 * @return
	 */
	protected abstract String doExecute(String... args);

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#getLocatorKey()
	 */
	@Override
	public String getLocatorKey() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#isDiscovery()
	 */
	@Override
	public boolean isDiscovery() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties props) {
		if(props!=null) {
			this.props.putAll(props);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#init()
	 */
	@Override
	public void init() {

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
		return aliases;
	}

}
