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

import org.helios.jzab.plugin.scripting.engine.Engine;

/**
 * <p>Title: IScriptUpdateListener</p>
 * <p>Description: Defines a listener that is to be notified of script source changes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.engine.script.IScriptUpdateListener</code></p>
 */

public interface IScriptUpdateListener {
	/**
	 * Callback from the script instance when the source changes (and is possibly recompiled)
	 * @param engine The script engine that runs the script
	 * @param instance The script instance that was updated
	 * @param statusOk true if the source change was clean (e.g. source recompiled ok), false if an error occured on source update
	 */
	public void onScriptSourceChange(Engine engine, ScriptInstance instance, boolean statusOk);
}
