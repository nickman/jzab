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
package org.helios.jzab.agent.net.active.collection;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.helios.jzab.agent.net.active.ActiveServer;

/**
 * <p>Title: IExecutionPlan</p>
 * <p>Description: Defines an implementation of a CommandThreadPolicy execution plan which manages how check executions are multithreaded</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.collection.IExecutionPlan</code></p>
 */
public interface IExecutionPlan {
	/**
	 * Returns a collection of check execution task callables. Each member of the array will be allocated to a seperate thread.
	 * @param delay The scheduling delay to plan executions for
	 * @param activeServer The active server that manage hosts that have active checks that are scheduled to be executed for the passed delay
	 * @param collectionStream The collection stream that results are written to 
	 * @return an collection of check execution task callables
	 */
	public Collection<? extends Callable<Void>> createPlan(long delay, ActiveServer activeServer, IActiveCollectionStream collectionStream); 
}
