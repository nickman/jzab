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
package org.helios.jzab.agent.net.active.schedule;

import org.helios.jzab.agent.net.active.collection.CommandThreadPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ScheduledCollection</p>
 * <p>Description: The scheduled runnable to collect the response fragments from each active check for a specific delay window.
 * Depending on the {@link CommandThreadPolicy}, this task may break up the collection tasks across additional concurrent threads.
 * The collected response fragments will be written to an output stream which may be in memory, or configured to buffer to disk temp space
 * if the number of collections is very large and might create a memory usage and/or garbage collection explosion. However, each collection
 * executing thread will have its own dedicated output stream to prevent undesired interlacing of results.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.ScheduledCollection</code></p>
 */
public class ScheduledCollection implements Runnable {
	/** The threading policy for this collection */
	protected final CommandThreadPolicy threadPolicy;
	/** The delay for which this execution is being targetted */
	protected final long delayCollection;
	
	/** Instance logger */
	protected final Logger log; 
	
	
	
	
	/**
	 * Creates a new ScheduledCollection
	 * @param threadPolicy The threading policy for this collection 
	 * @param delayCollection The delay for which this execution is being targetted
	 */
	public ScheduledCollection(CommandThreadPolicy threadPolicy, long delayCollection) {
		this.threadPolicy = threadPolicy;
		this.delayCollection = delayCollection;
		log = LoggerFactory.getLogger(getClass().getName() + "-" + delayCollection);
		log.info("Created Scheduled Collection for delay [{}]", delayCollection);
	}



	/**
	 * Executes the configured collection
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
	}
	
}
