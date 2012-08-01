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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: FileDeletor</p>
 * <p>Description: Slightly more efficient file delete on exit.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.util.FileDeletor</code></p>
 */
public class FileDeletor extends Thread {
	/** A set of files to be deleted */
	private static final Set<File> toBeDeleted = new CopyOnWriteArraySet<File>();
	/** A set of file channels to be closed */
	private static final Set<Closeable> toBeClosed = new CopyOnWriteArraySet<Closeable>();
	/** Static class logger */
	protected final static Logger log = LoggerFactory.getLogger(FileDeletor.class);
	
	
	
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
				try { Thread.currentThread().join(60000); } catch (Exception e) {}
				FD.run();
			}
		};
		t.setDaemon(true);		
		t.start();
	}
	
	/**
	 * Adds an array of files to be deleted on JVM shutdown
	 * @param files The files to be deleted
	 */
	public static void deleteOnExit(File...files) {
		if(files!=null) {
			for(File file: files) {
				if(file!=null && file.exists()) {
					toBeDeleted.add(file);
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
					toBeClosed.add(closeable);
			}
		}
	}
	
	
	/**
	 * Executes the file deletion
	 * {@inheritDoc}
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try { System.gc(); } catch (Throwable t) {}
		
		for(Iterator<Closeable> iter = toBeClosed.iterator(); iter.hasNext();) {
			Closeable closeable = iter.next();
			try { closeable.close(); } catch (Exception e) {}			
		}
		toBeClosed.clear();
		if(toBeDeleted.isEmpty()) return;
		Set<File> deletedOk = new HashSet<File>();
		for(Iterator<File> iter = toBeDeleted.iterator(); iter.hasNext();) {
			File file = iter.next();
			if(!file.exists() || file.delete()) {
				log.debug("Successfully deleted file [{}]", file);
				deletedOk.add(file);
			}			
		}
		toBeDeleted.removeAll(deletedOk);
	}
}
