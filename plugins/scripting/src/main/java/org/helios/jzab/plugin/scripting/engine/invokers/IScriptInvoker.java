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

/**
 * <p>Title: IScriptInvoker</p>
 * <p>Description: Unified script invoker interface to provide a common invoker interface for evals, invocables and compiled scripts.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.IScriptInvoker</code></p>
 */

public interface IScriptInvoker {

	/**
	 * TODO:
	 * 
	 * Invocation Patterns:
	 * ====================
	 * invoke and get return value
	 * invoke and capture output stream
	 * both
	 * 
	 * Inputs/Outputs:
	 * ===============
	 * Inputs:
	 * 		StdIn
	 * 		Parameters (args bindings)
	 * 		Variable bindings
	 * 
	 * Outputs:
	 * 		StdOut
	 * 		InOut Bindings
	 * 		Return value
	 * 
	 * 
	 * CompiledScript:
	 * ===============
	 * eval()
	 * eval(ScriptContext)
	 * 
	 * Invocable:
	 * ==========
	 * invokeFunction(String name, Object... args) 
	 * invokeMethod(Object thiz, String name, Object... args) 
	 * 
	 * ScriptEngine:
	 * =============
	 * eval(String src) 
	 * eval(String src, ScriptContext s)
	 * 
	 * Useful captures:
	 * ================
	 * Script Name:  Defaults to file name  (jzab.xml defs have mandatory name attribute)
	 * Discovery: Need to know if script executes discovery functions since the response to the server is formatted differently for discovery requests
	 * 			If invocable/field value is not available, then need to use script name  (XXX.DIS ??) 
	 *
	 * SEPERATE CONCERN:
	 * =================
	 * getInterface(Class<T> clasz) 
	 * getInterface(Object thiz, Class<T> clasz) 
	 */
	
	
	public Object invoke(Object...args);
	
	
}
