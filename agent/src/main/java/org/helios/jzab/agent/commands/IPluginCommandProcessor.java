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
package org.helios.jzab.agent.commands;

/**
 * <p>Title: IPluginCommandProcessor</p>
 * <p>Description: An extension of {@link ICommandProcessor} that returns the instance of the processor itself.
 * This is so the command executor can execute directly against the plugged in processor rather than invoking 
 * through JMX.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.IPluginCommandProcessor</code></p>
 */
public interface IPluginCommandProcessor extends ICommandProcessor {
    /**
     * Returns the actual instance of the processor
     * @return the actual instance of the processor
     */
    public ICommandProcessor getInstance();
    
    /**
     * Returns additional aliases for this plugin
     * @return additional aliases for this plugin
     */
    public String[] getAliases();

    
}
