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

import javax.script.Compilable;
import javax.script.CompiledScript;

import org.helios.jzab.plugin.scripting.engine.Engine;

/**
 * <p>Title: ScriptInvokerFactory</p>
 * <p>Description: Creates invocation proxies for passed script engines</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.invokers.ScriptInvokerFactory</code></p>
 */
public class ScriptInvokerFactory {
	/**
	 * @param engine
	 * @param scriptName
	 * @return
	 */
	public static IScriptInvoker invokerFor(Engine engine, String source) {
		if(engine==null) throw new IllegalArgumentException("The passed engine was null", new Throwable());
		if(source==null) throw new IllegalArgumentException("The passed script source was null", new Throwable());
		IScriptInvoker invoker = null;
		if(engine.isCompilable()) {
			
		}
		return invoker;
	}
	
	
	protected static IScriptInvoker compiled(Engine engine, String source) {
		final CompiledScript cs = ((Compilable)engine.getScriptEngine()).compile(source);
		return new IScriptInvoker() {
			/**
			 * {@inheritDoc}
			 * @see org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker#invoke(java.lang.Object[])
			 */
			@Override
			public Object invoke(Object... args) {
				cs.
				return null;
			}
			
		};
	}
}
