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

import java.io.Closeable;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jzab.util.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: FileDeletor</p>
 * <p>Description: Slightly more efficient file delete on exit.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.util.FileDeletor</code></p>
 */
public class FileDeletor extends Thread implements FileDeletorMXBean {
	/** A set of files to be deleted */
	private static final Set<File> toBeDeleted = new CopyOnWriteArraySet<File>();
	/** A set of file channels to be closed */
	private static final Set<Closeable> toBeClosed = new CopyOnWriteArraySet<Closeable>();
	/** Static class logger */
	protected final static Logger log = LoggerFactory.getLogger(FileDeletor.class);
	
	/** A counter for successful deletes  */
	private static final AtomicLong delCounter = new AtomicLong(0L);
	/** A counter for reaper runs  */
	private static final AtomicLong reaperRun = new AtomicLong(0L);
	
	
	
	static {
		Runtime.getRuntime().addShutdownHook(new FileDeletor());
	}
	
	/**
	 * Creates a new FileDeletor and starts the reaper thread
	 */
	private FileDeletor() {
		final FileDeletor FD = this;
		Thread t = new Thread("FileDelReaperThread"){
			public void run() {
				while(true) {
					try { Thread.currentThread().join(60000); } catch (Exception e) {}
					try { FD.run();	} catch (Exception e) {}
				}
			}
		};
		t.setDaemon(true);		
		t.start();
		JMXHelper.registerMBean(
				JMXHelper.getHeliosMBeanServer(), 
				JMXHelper.objectName("org.helios.jzab.agent.util:service=FileDeletor"), FD);
	}
	
	/**
	 * Adds an array of files to be deleted on JVM shutdown
	 * @param files The files to be deleted
	 */
	public static void deleteOnExit(File...files) {
		if(files!=null) {
			for(File file: files) {
				if(file!=null && file.exists()) {
					try {
						if(!file.delete()) {
							toBeDeleted.add(file);
						} else {
							delCounter.incrementAndGet();
						}
					} catch (Exception e) {
						toBeDeleted.add(file);
					}				
				}
			}
		}
	}
	
	/**
	 * Adds an array of closeables to be closed on JVM shutdown
	 * @param closeables The closeables to be closed
	 */
	public static void closeOnExit(Closeable...closeables) {
		if(closeables!=null) {
			for(Closeable closeable: closeables) {
				if(closeable==null) continue;
				try {
					closeable.close();
				} catch (Exception e) {
					toBeClosed.add(closeable);
				}				
			}
		}
	}
	
	
	/**
	 * Executes the file deletion
	 * {@inheritDoc}
	 * @see java.lang.Thread#run()
	 */
	public void run() {		
		reaperRun.incrementAndGet();
		for(Iterator<Closeable> iter = toBeClosed.iterator(); iter.hasNext();) {
			Closeable closeable = iter.next();
			try { closeable.close(); } catch (Exception e) {}			
		}
		toBeClosed.clear();
		if(toBeDeleted.isEmpty()) return;
		Set<File> deletedOk = new HashSet<File>();
		for(Iterator<File> iter = toBeDeleted.iterator(); iter.hasNext();) {
			File file = iter.next();
			if(file.exists()) {
				if(file.delete()) {
					deletedOk.add(file);
					delCounter.incrementAndGet();
					log.debug("Successfully deleted file [{}]", file);
				}
			} else {
				deletedOk.add(file);
			}
		}
		toBeDeleted.removeAll(deletedOk);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.util.FileDeletorMXBean#getPendingCloseables()
	 */
	@Override
	public int getPendingCloseables() {
		return toBeClosed.size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.util.FileDeletorMXBean#getFileDeletes()
	 */
	@Override
	public int getPendingDeletes() {
		return toBeDeleted.size();
	}

	/**
	 * Returns the cummulative number of successful file deletions
	 * @return the cummulative number of successful file deletions
	 */
	public long getDeletionCount() {
		return delCounter.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.util.FileDeletorMXBean#getReaperCount()
	 */
	public long getReaperCount() {
		return reaperRun.get();
	}
	
	/**
	 * @return the delcounter
	 */
	public static AtomicLong getDelcounter() {
		return delCounter;
	}
}
