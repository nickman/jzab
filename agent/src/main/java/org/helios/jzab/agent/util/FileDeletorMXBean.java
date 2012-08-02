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
package org.helios.jzab.agent.util;

/**
 * <p>Title: FileDeletorMXBean</p>
 * <p>Description: JMX interface for {@link FileDeletor}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.util.FileDeletorMXBean</code></p>
 */
public interface FileDeletorMXBean {
	/**
	 * Returns the number of pending Closeables
	 * @return the number of pending Closeables
	 */
	public int getPendingCloseables();
	/**
	 * Returns the number of pending file deletions
	 * @return the number of pending file deletions
	 */	
	public int getFileDeletes();
	
	/**
	 * Runs a file deletion
	 */
	public void run();
	
	/**
	 * Returns the cummulative number of successful file deletions
	 * @return the cummulative number of successful file deletions
	 */
	public long getDeletionCount();
	
	/**
	 * Returns the number of times the reaper thread has run
	 * @return the number of times the reaper thread has run
	 */
	public long getReaperCount();
}
