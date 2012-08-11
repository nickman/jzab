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
package org.helios.jzab.plugin.scripting.engine.invokers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.helios.jzab.plugin.scripting.engine.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ScriptInvokerFactory</p>
 * <p>Description: Creates invocation proxies for passed script engines</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.invokers.ScriptInvokerFactory</code></p>
 * TODO: The command and discovery method is awkward, but could be complicated to replace.
 * We need a way of wrapping scripts that we know have command or discovery in a way that
 * the command manager can index them correctly.
 */
public class ScriptInvokerFactory {
	/** Instance logger */
	protected static final Logger log = LoggerFactory.getLogger(ScriptInvokerFactory.class);
	
	/**
	 * Determines if the passed script name indicates command support.
	 * @param scriptName The script name
	 * @return true if the name indicates a command, false otherwise
	 */
	protected static boolean isCommand(String scriptName) {
		return scriptName.trim().toLowerCase().endsWith("command");
	}
	
	/**
	 * Determines if the passed script name indicates discovery command support.
	 * @param scriptName The script name
	 * @return true if the name indicates a discovery command, false otherwise
	 */
	protected static boolean isDiscovery(String scriptName) {
		return scriptName.trim().toLowerCase().endsWith("discover");
	}
	
	
	/**
	 * Determines if the passed engine contains an invocable function <code>execute[ScriptName]</code>.
	 * @param engine The invocable
	 * @param scriptName The script name
	 * @return true if the method was found, false otherwise
	 */
	protected static boolean isCommand(Invocable engine, String scriptName) {
		try {
			try {
				engine.invokeFunction("execute" + scriptName, "", new String[]{});
				return true;
			} catch (NoSuchMethodException nse) {
				return false;
			} catch (ScriptException se) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Determines if the passed engine contains an invocable function <code>discover[ScriptName]</code>.
	 * @param engine The invocable
	 * @param scriptName The script name
	 * @return true if the method was found, false otherwise
	 */
	protected static boolean isDiscovery(Invocable engine, String scriptName) {
		try {
			try {
				engine.invokeFunction("discover" + scriptName, "", new String[]{});
				return true;
			} catch (NoSuchMethodException nse) {
				return false;
			} catch (ScriptException se) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	
	/**
	 * Creates an invoker for the passed engine and source
	 * @param engine The engine
	 * @param name The name of the script
	 * @param source The source
	 * @return an invoker
	 * TODO: Add name sniffer if name was null
	 */
	public static IScriptInvoker invokerFor(Engine engine, String name, String source) {
		if(engine==null) throw new IllegalArgumentException("The passed engine was null", new Throwable());
		if(source==null) throw new IllegalArgumentException("The passed script source was null", new Throwable());
		IScriptInvoker invoker = null;
		try {
			engine.getScriptEngine().eval(source);
		} catch (Exception e) {
			log.error("Failed to compile source for engine [{}]", engine.getEngineName(), e);
		}
		if(engine.isCompilable()) {
			if(engine.isInvocable()) {
				invoker = compiledInvocable(engine, name, source);
			} else {
				invoker = compiled(engine, name, source);
			}
		} else {
			if(engine.isInvocable()) {
				invoker = invocable(engine, name, source);
			} else {
				invoker = evaled(engine, name, source);
			}			
		}
		return invoker;
	}
	
	/**
	 * Creates a non-compiled and invocable script invoker
	 * @param engine The engine
	 * @param name The name of the script
	 * @param source The source
	 * @return a script invoker
	 */
	protected static IScriptInvoker invocable(final Engine engine, final String name, final String source) {
		final boolean command = ScriptInvokerFactory.isCommand((Invocable)engine.getScriptEngine(), name);
		final boolean discovery = ScriptInvokerFactory.isDiscovery((Invocable)engine.getScriptEngine(), name);
		return new IScriptInvoker() {
			/** A cache of pre-looked up target objects */
			protected final Map<String, Object> targets = new ConcurrentHashMap<String, Object>();
			@Override
			public String getName() {
				return name;
			}
			@Override
			public Object invoke(Object... args) throws ScriptException {
				try {
					Bindings bindings = engine.getScriptEngine().createBindings();
					bindings.put(ScriptEngine.ARGV, args);
					return engine.getScriptEngine().eval(source, bindings);
				} catch (Exception e) {
					log.error("Evaled Script Exception [{}]", engine.getEngineName(), e);
					throw new ScriptException(e);
				}
			}
			@Override
			public Object invoke(String functionName, Object...args) throws ScriptException, NoSuchMethodException {
				return ((Invocable)engine.getScriptEngine()).invokeFunction(functionName, args);
			}			
			@Override
			public Object invoke(String objectName, String methodName, Object...args) throws ScriptException, NoSuchMethodException {				
				Object target = targets.get(objectName);
				if(target==null) {
					target = engine.getScriptEngine().get(objectName);
					targets.put(objectName, target);
				}						
				return ((Invocable)engine.getScriptEngine()).invokeMethod(target, methodName, args);
			}

			@Override
			public Engine getEngine() {
				return engine;
			}
			@Override
			public boolean isCommand() {
				return command;
			}
			@Override
			public boolean isDiscovery() {
				return discovery;
			}
			
		};

	}
	
	
	/**
	 * Creates a compiled and invocable script invoker
	 * @param engine The engine
	 * @param name The name of the script
	 * @param source The source
	 * @return a script invoker
	 */
	protected static IScriptInvoker compiledInvocable(final Engine engine, final String name, String source) {
		final CompiledScript cs;
		
		final boolean icommand = ScriptInvokerFactory.isCommand((Invocable)engine.getScriptEngine(), name);
		final boolean command = ScriptInvokerFactory.isCommand(name);
		
		final boolean idiscovery = ScriptInvokerFactory.isDiscovery((Invocable)engine.getScriptEngine(), name);
		final boolean discovery = ScriptInvokerFactory.isDiscovery(name);
		
		try {
			cs = ((Compilable)engine.getScriptEngine()).compile(source);
		} catch (ScriptException e) {
			throw new RuntimeException("Failed to compile script for engine [" + engine.getEngineName() + "]", e);
		}
		return new IScriptInvoker() {
			@Override
			public String getName() {
				return name;
			}		
			
			@Override
			public Object invoke(Object... args) throws ScriptException {
				try {
					Bindings bindings = engine.getScriptEngine().createBindings();
					bindings.put("args", args);
					return cs.eval(bindings);
				} catch (Exception e) {
					log.error("Compiled Script Exception [{}]", engine.getEngineName(), e);
					throw new ScriptException(e);
				}				
			}
			@Override
			public Object invoke(String functionName, Object...args) throws ScriptException, NoSuchMethodException {
				return ((Invocable)engine.getScriptEngine()).invokeFunction(functionName, args);
			}			
			@Override
			public Object invoke(String objectName, String methodName, Object...args) throws ScriptException, NoSuchMethodException {
				Object target = engine.getScriptEngine().get(objectName);
				return ((Invocable)engine.getScriptEngine()).invokeMethod(target, methodName, args);
			}

			@Override
			public Engine getEngine() {
				return engine;
			}

			@Override
			public boolean isCommand() {
				return command||icommand;
			}

			@Override
			public boolean isDiscovery() {
				return discovery||idiscovery;
			}
			
		};
		
	}
	
	
	/**
	 * Creates a compiled script invoker
	 * @param engine The engine
	 * @param name The name of the script
	 * @param source The source
	 * @return a script invoker
	 */
	protected static IScriptInvoker compiled(final Engine engine, final String name, String source) {
		final CompiledScript cs;
		final boolean command = ScriptInvokerFactory.isCommand(name);
		final boolean discovery = ScriptInvokerFactory.isDiscovery(name);		
		try {
			cs = ((Compilable)engine.getScriptEngine()).compile(source);
		} catch (ScriptException e) {
			throw new RuntimeException("Failed to compile script for engine [" + engine.getEngineName() + "]", e);
		}
		return new IScriptInvoker() {
			
			@Override
			public String getName() {
				return name;
			}
			
			private Object _invoke(Object... args) throws ScriptException {
				try {
					Bindings bindings = engine.getScriptEngine().createBindings();
					bindings.put("args", args);
					return cs.eval(bindings);
				} catch (Exception e) {
					log.error("Compiled Script Exception [{}]", engine.getEngineName(), e);
					throw new ScriptException(e);
				}				
			}
			@Override
			public Object invoke(Object... args) throws ScriptException {
				return _invoke(args);
			}
			// Ignores the function name and just calls the main
			@Override
			public Object invoke(String functionName, Object...args) throws ScriptException {
				return _invoke(args);
			}
			// Ignores the object name and method name and just calls the main
			@Override
			public Object invoke(String objectName, String methodName, Object...args) throws ScriptException {
				return _invoke(args);
			}

			@Override
			public Engine getEngine() {
				return engine;
			}
			@Override
			public boolean isCommand() {
				return command;
			}

			@Override
			public boolean isDiscovery() {
				return discovery;
			}
			
			
		};
	}
	
	/**
	 * Creates a non-compiled script evaluator
	 * @param engine The engine
	 * @param name The name of the script
	 * @param source The source
	 * @return a script invoker
	 */
	protected static IScriptInvoker evaled(final Engine engine, final String name, final String source) {
		final boolean command = ScriptInvokerFactory.isCommand(name);
		final boolean discovery = ScriptInvokerFactory.isDiscovery(name);					
		return new IScriptInvoker() {
			@Override
			public String getName() {
				return name;
			}			
			@Override
			public Object invoke(Object... args) throws ScriptException {
				try {
					Bindings bindings = engine.getScriptEngine().createBindings();
					bindings.put(ScriptEngine.ARGV, args);
					return engine.getScriptEngine().eval(source, bindings);
				} catch (Exception e) {
					log.error("Evaled Script Exception [{}]", engine.getEngineName(), e);
					throw new ScriptException(e);
				}
			}
			// Ignores the function name and just calls the main
			@Override
			public Object invoke(String functionName, Object...args) throws ScriptException {
				try {
					Bindings bindings = engine.getScriptEngine().createBindings();
					bindings.put(ScriptEngine.ARGV, args);
					return engine.getScriptEngine().eval(source, bindings);
				} catch (Exception e) {
					log.error("Evaled Script Exception [{}]", engine.getEngineName(), e);
					throw new ScriptException(e);
				}				
			}
			// Ignores the object name and method name and just calls the main
			@Override
			public Object invoke(String objectName, String methodName, Object...args) throws ScriptException {
				try {
					Bindings bindings = engine.getScriptEngine().createBindings();
					bindings.put(ScriptEngine.ARGV, args);
					return engine.getScriptEngine().eval(source, bindings);
				} catch (Exception e) {
					log.error("Evaled Script Exception [{}]", engine.getEngineName(), e);
					throw new ScriptException(e);
				}				
			}
			@Override
			public Engine getEngine() {
				return engine;
			}
			@Override
			public boolean isCommand() {
				return command;
			}

			@Override
			public boolean isDiscovery() {
				return discovery;
			}
			
			
		};
	}
	
}
