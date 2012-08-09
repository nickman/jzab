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

import java.util.List;

import javax.management.ObjectName;
import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: Engine</p>
 * <p>Description: JMX manageable wrapper for {@link javax.script.ScriptEngineFactory}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>javax.script.Engine</code></p>
 */
public class Engine implements EngineMXBean {
	/** The wrapped ScriptEngineFactory */
	protected final ScriptEngineFactory scriptEngineFactory;
	/** The JMX ObjectName for this engine */
	protected final ObjectName objectName;
	/** The script engine instance */
	protected final ScriptEngine engine;
	/**
	 * Creates a new Engine
	 * @param scriptEngineFactory The wrapped {@link javax.script.ScriptEngineFactory}
	 */
	public Engine(ScriptEngineFactory scriptEngineFactory) {
		this.scriptEngineFactory = scriptEngineFactory;
		engine = scriptEngineFactory.getScriptEngine();
		objectName = JMXHelper.objectName("org.helios.jzab.agent.plugin.script:type=Engine,name=" + ObjectName.quote(this.scriptEngineFactory.getEngineName()));
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), objectName, this);
	}
	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getEngineName()
	 */
	public String getEngineName() {
		return scriptEngineFactory.getEngineName();
	}
	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getEngineVersion()
	 */
	public String getEngineVersion() {
		return scriptEngineFactory.getEngineVersion();
	}
	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getExtensions()
	 */
	public List<String> getExtensions() {
		return scriptEngineFactory.getExtensions();
	}
	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getMimeTypes()
	 */
	public List<String> getMimeTypes() {
		return scriptEngineFactory.getMimeTypes();
	}
	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getNames()
	 */
	public List<String> getNames() {
		return scriptEngineFactory.getNames();
	}
	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getLanguageName()
	 */
	public String getLanguageName() {
		return scriptEngineFactory.getLanguageName();
	}
	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getLanguageVersion()
	 */
	public String getLanguageVersion() {
		return scriptEngineFactory.getLanguageVersion();
	}
	/**
	 * @param key
	 * @return
	 * @see javax.script.ScriptEngineFactory#getParameter(java.lang.String)
	 */
	public Object getParameter(String key) {
		return scriptEngineFactory.getParameter(key);
	}
	/**
	 * @param obj
	 * @param m
	 * @param args
	 * @return
	 * @see javax.script.ScriptEngineFactory#getMethodCallSyntax(java.lang.String, java.lang.String, java.lang.String[])
	 */
	public String getMethodCallSyntax(String obj, String m, String... args) {
		return scriptEngineFactory.getMethodCallSyntax(obj, m, args);
	}
	/**
	 * @param toDisplay
	 * @return
	 * @see javax.script.ScriptEngineFactory#getOutputStatement(java.lang.String)
	 */
	public String getOutputStatement(String toDisplay) {
		return scriptEngineFactory.getOutputStatement(toDisplay);
	}
	/**
	 * @param statements
	 * @return
	 * @see javax.script.ScriptEngineFactory#getProgram(java.lang.String[])
	 */
	public String getProgram(String... statements) {
		return scriptEngineFactory.getProgram(statements);
	}
	/**
	 * @return
	 * @see javax.script.ScriptEngineFactory#getScriptEngine()
	 */
	public ScriptEngine getScriptEngine() {
		return engine;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.EngineMXBean#isCompilable()
	 */
	public boolean isCompilable() {
		return (engine instanceof Compilable);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.scripting.engine.EngineMXBean#isInvocable()
	 */
	public boolean isInvocable() {
		return (engine instanceof Invocable);
	}
	
	
	
	

}
