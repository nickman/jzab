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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;
import javax.script.ScriptException;

import org.helios.jzab.agent.SystemClock;
import org.helios.jzab.agent.commands.ICommandProcessor;
import org.helios.jzab.plugin.scripting.engine.Engine;
import org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker;
import org.helios.jzab.plugin.scripting.engine.invokers.ScriptInvokerFactory;
import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: ScriptInstance</p>
 * <p>Description: A wrapper for an individual script</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.script.ScriptInstance</code></p>
 */
public class ScriptInstance implements ScriptInstanceMBean {
	/** The script source */
	protected String source;
	/** The script name */
	protected final String name;
	/** The object name of this script instance */
	protected final ObjectName objectName;
	
	/** The timestamp of the script source */
	protected long sourceTime;
	/** The generic script invoker */
	protected IScriptInvoker invoker;
	
	
	/**
	 * Creates a new ScriptInstance
	 * @param source The source
	 * @param name The script name
	 * @param invoker The script invoker
	 */
	public ScriptInstance(CharSequence source, String name, IScriptInvoker invoker) {
		this.source = source.toString();
		this.name = name;
		this.invoker = invoker;
		sourceTime = SystemClock.currentTimeMillis();
		objectName = JMXHelper.objectName(new StringBuilder(invoker.getEngine().getObjectName().toString()).append(",script=").append(name));		
	}
	
	/**
	 * Simple parameterless invoke with a string return
	 * @return the result of the invoke
	 */
	@Override
	public String invoke() {
		try {
			Object ret = invoker.invoke();
			return ret==null ? null : ret.toString();
		} catch (ScriptException e) {
			throw new RuntimeException("Failed to invoke script [" + name + "]", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean#getSource()
	 */
	@Override
	public String getSource() {
		return source;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean#setSource(java.lang.String)
	 */
	@Override
	public void setSource(String source) {
		if(source==null) throw new IllegalArgumentException("The passed source was null", new Throwable());
		if(!this.source.equals(source)) {
			this.source = source;
			Engine engine = invoker.getEngine();
			invoker = ScriptInvokerFactory.invokerFor(engine, name, this.source);
			sourceTime = SystemClock.currentTimeMillis();
			engine.updateScriptSource(this);
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean#getSourceTime()
	 */
	@Override
	public long getSourceTime() { 
		return sourceTime;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns 
	 * @return the invoker
	 */
	public IScriptInvoker getInvoker() {
		return invoker;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean#getSourceDate()
	 */
	@Override
	public Date getSourceDate() {
		return new Date(sourceTime);
	}

	/**
	 * Returns the ObjectName for this script instance
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker#invoke(java.lang.Object[])
	 */
	@Override
	public Object invoke(Object... args) throws ScriptException {
		return invoker.invoke(args);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker#invoke(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Object invoke(String functionName, Object... args) throws ScriptException, NoSuchMethodException {		
		return invoker.invoke(functionName, args);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker#invoke(java.lang.String, java.lang.String, java.lang.Object[])
	 */
	@Override
	public Object invoke(String objectName, String methodName, Object... args) throws ScriptException, NoSuchMethodException {		
		return invoke(objectName, methodName, args);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker#getEngine()
	 */
	@Override
	public Engine getEngine() {
		return invoker.getEngine();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean#invoke(java.lang.String[])
	 */
	@Override
	public Object invoke(String delim, String args) throws ScriptException {
		return invoke(split(delim, args));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean#invoke(java.lang.String, java.lang.String[])
	 */
	@Override
	public Object invoke(String delim, String functionName, String args)
			throws ScriptException, NoSuchMethodException {
		return invoke(functionName, split(delim, args));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.script.ScriptInstanceMBean#invoke(java.lang.String, java.lang.String, java.lang.String[])
	 */
	@Override
	public Object invoke(String delim, String objectName, String methodName, String args)
			throws ScriptException, NoSuchMethodException {
		return invoke(objectName, methodName, split(delim, args));
	}
	
	/**
	 * Parses the arguments, splitting by the delimiter, trims each items and returns the array
	 * @param delim The delimiter
	 * @param args The args to split
	 * @return The split and trimmed args
	 */
	protected static Object[] split(String delim, String args) {
		if(args==null||args.trim().isEmpty()||delim==null||delim.trim().isEmpty()) return new Object[0];		
		String[] splits = args.split(delim);
		for(int i = 0; i < splits.length; i++) {
			splits[i] = splits[i].trim(); 
		}
		return splits;
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker#isCommand()
	 */
	@Override
	public boolean isCommand() {
		return invoker.isCommand();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker#isDiscovery()
	 */
	@Override
	public boolean isDiscovery() {
		return invoker.isDiscovery();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#execute(java.lang.String, java.lang.String[])
	 */
	@Override
	public Object execute(String commandName, String... args) {
		try {
			return invoker.invoke(args);
		} catch (Exception e) {
			return COMMAND_ERROR;
		}
	}

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
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties props) {
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
		return new String[]{};
	}
}
