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

import org.helios.jzab.plugin.scripting.engine.invokers.IScriptInvoker;

/**
 * <p>Title: ScriptInstance</p>
 * <p>Description: A wrapper for an individual script</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.script.ScriptInstance</code></p>
 */
public class ScriptInstance {
	/** The script source */
	protected String source;
	/** The script name */
	protected String name;
	
	/** The timestamp of the script source */
	protected long sourceTime;
	/** The generic script invoker */
	protected IScriptInvoker invoker;
	
	protected ScriptInstance(CharSequence source) {
		this.source = source.toString();
	}
}
