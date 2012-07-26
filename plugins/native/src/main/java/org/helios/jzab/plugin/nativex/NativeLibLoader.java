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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import org.helios.jzab.agent.util.ReadableWritableByteChannelBuffer;

/**
 * <p>Title: NativeLibLoader</p>
 * <p>Description: Utility class to load a native library acquired as a resource from the classpath.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.NativeLibLoader</code></p>
 */
public class NativeLibLoader {

	
	public static class SnipeFilesRepo  {
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
		
		private SnipeFilesRepo() {
			
		}
		
		private void load() {
			if(serFile.canRead()) {
				ReadableWritableByteChannelBuffer buff = ReadableWritableByteChannelBuffer.newDirect((int)serFile.length());
				try {
					Set<String> set = (Set<String>)new ObjectInputStream(ReadableWritableByteChannelBuffer.newDirect((int)serFile.length()).asInputStream()).readObject();
					for(String s: set) {
						if(s!=null && !s.trim().isEmpty()) {
							filesToSnipe.add(s);
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
	}
}
