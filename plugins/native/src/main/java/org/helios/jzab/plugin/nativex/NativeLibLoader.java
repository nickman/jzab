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
package org.helios.jzab.plugin.nativex;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.helios.jzab.agent.util.ReadableWritableByteChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: NativeLibLoader</p>
 * <p>Description: Utility class to load a native library acquired as a resource from the classpath.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.NativeLibLoader</code></p>
 */
public class NativeLibLoader {
	/** Static class logger */
	protected static final Logger  LOG = LoggerFactory.getLogger(NativeLibLoader.class);

	
	
	/**
	 * Adds a file to be sniped
	 * @param name The name of the file
	 */
	public void deleteFile(CharSequence name) {
		SnipeFilesRepo.getInstance().addFile(name);
	
	}
	
	/**
	 * Adds a file to be sniped
	 * @param file The file
	 */
	public void deleteFile(File file) {
		SnipeFilesRepo.getInstance().addFile(file);
	}
	
	public static void main(String[] args) {
		LOG.info("Snipe Test");
		try {
			for(int i = 0; i < 5; i++) {
				File f = File.createTempFile("Foo", "ffo.tmp");
				SnipeFilesRepo.getInstance().bypass(f);
			}
		} catch (Exception e) {
			LOG.error("Failed", e);
		}
		
	}
	
	
	/**
	 * <p>Title: SnipeFilesRepo</p>
	 * <p>Description: Maintains a persistent repository of files that failed to be deleted during that last shutdown and will be deleted on the next startup</p>
	 * <p>The serialized list of files is saved to <b><code>${user.home}/.heliosJzabSnipeFiles.ser</code></b>. 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.plugin.nativex.NativeLibLoader.SnipeFilesRepo</code></p>
	 */
		static class SnipeFilesRepo  extends Thread {
			/** The names of the files that need to be snipped */
			private final Set<String> filesToSnipe = new HashSet<String>();
			/** The singleton instance */		
			private static volatile SnipeFilesRepo instance = null;
			/** The singleton instance ctor lock */
			private static final Object lock = new Object();
			/** The name of the file where this file will be saved and loaded from */
			public static final File serFile = new File(System.getProperty("user.home") + File.separator + ".heliosJzabSnipeFiles.ser");

			/**
			 * Acquires the singleton instance
			 * @return the singleton instance
			 */
			public static SnipeFilesRepo getInstance() {
				if(instance==null) {
					synchronized(lock) {
						if(instance==null) {
							instance = new SnipeFilesRepo();
						}
					}
				}
				return instance;
			}
			
			/**
			 * {@inheritDoc}
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run() {
				save();
			}

			private SnipeFilesRepo() {
				Runtime.getRuntime().addShutdownHook(this);
				load();
				onStart();
			}

			/**
			 * Deletes sniped files on startup
			 */
			private void onStart() {
				for(Iterator<String> iter = filesToSnipe.iterator(); iter.hasNext();) {
					File f = new File(iter.next());
					if(f.exists()) {
						if(!f.delete()) {
							continue;
						} 
						LOG.debug("Startup deleted [{}]", f);
					} 					
					iter.remove();					
				}					
			}

			/**
			 * Adds a file to be sniped
			 * @param name The name of the file
			 */
			public void addFile(CharSequence name) {
				if(name!=null) {
					String nm = name.toString();
					File f = new File(nm);
					if(f.exists()) {
						if(!f.delete()) {
							filesToSnipe.add(f.getAbsolutePath());
							save();
						}
					}
				}
			}
			
			/**
			 * Testing hook to add a file without attempting to delete
			 * @param f The file to add
			 */
			public void bypass(File f) {
				filesToSnipe.add(f.getAbsolutePath());
				save();
			}

			/**
			 * Adds a file to be sniped
			 * @param file The file
			 */
			public void addFile(File file) {
				if(file!=null) {
					if(file.exists()) {
						if(!file.delete()) {
							filesToSnipe.add(file.getAbsolutePath());
							save();
						}
					}
				}
			}


			/**
			 * Attempts to read the snipe file
			 */
			@SuppressWarnings("unchecked")
			private void load() {
				if(serFile.canRead()) {
					FileChannel fc = null;
					try {
						ReadableWritableByteChannelBuffer buff = ReadableWritableByteChannelBuffer.newDirectDynamic((int)serFile.length());
						fc = new RandomAccessFile(serFile, "rw").getChannel();
						fc.transferFrom(buff, 0, serFile.length());
						
						Set<String> set = (Set<String>)new ObjectInputStream(buff.asInputStream()).readObject();
						for(String s: set) {
							if(s!=null && !s.trim().isEmpty()) {
								filesToSnipe.add(s);
							}
						}
						LOG.debug("Loaded [{}] Files", set.size());
					} catch (Exception e) {
						LOG.debug("Failed load Snipe File [{}]", serFile, e);
						//serFile.delete();
					}  finally {
						try { if(fc.isOpen()) fc.close(); } catch (Exception e) {}
					}
				}			
			}
		
		
		/**
		 * Saves the snipe file. Yeah, it's not an XA transaction so if the write fails after the delete, then KAPUT.
		 */
		private synchronized void save() {
			if(filesToSnipe.isEmpty()) return;
			FileChannel fc = null;
			int size = 0;
			for(String s: filesToSnipe ) { size += s.getBytes().length; } 
			try {
				RandomAccessFile raf = new RandomAccessFile(serFile, "rw");
				fc = raf.getChannel();
				if(serFile.exists()) {
					fc.truncate(0);
				}
				ReadableWritableByteChannelBuffer buff = ReadableWritableByteChannelBuffer.newDirectDynamic(size);
				ObjectOutputStream oos = new ObjectOutputStream(buff.asOutputStream());
				oos.writeObject(filesToSnipe);
				oos.flush();
				buff.asOutputStream().flush();
				LOG.info("Out: [{}]", buff);
				long bt = fc.transferTo(0, buff.writerIndex(), buff);
				fc.force(true);
				fc.close();
				LOG.debug("Saved [{}] bytes for [{}] file names", bt, filesToSnipe.size());
			} catch (Exception e) {
				LOG.error("Failed to save snipe file", e);
			} finally {
				try { if(fc.isOpen()) fc.close(); } catch (Exception e) {}
			}
		}
	
	}
		
}
	

