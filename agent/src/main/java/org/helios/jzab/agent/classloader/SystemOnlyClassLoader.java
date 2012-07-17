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
package org.helios.jzab.agent.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * <p>Title: SystemOnlyClassLoader</p>
 * <p>Description: Classloader that only loads from the root classloader. 
 * It serves as a parent classloader and by default its <b><code>findClass</code></b> and <b><code>loadClass</code></b>
 * methods throw a <b><code>ClassNotFoundException</code></b> which causes the class lookup to be delegated to its child
 * classloader[s]. Each child classloader (ideally a {@link IsolatedArchiveLoader} should then attempt to load the specified
 * class and if it fails to find the class (as it might for a system class), it can delegate back up to this classloader using the <b><code>forReal</code></b>
 * method which will then attempt to load and return the class.</p>
 * <p>Essentially, this classloader supports a fully isolated set of classes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.classloader.SystemOnlyClassLoader</code></p>
 */
public class SystemOnlyClassLoader extends URLClassLoader {
	/** The root classloader (where java.lang.Object was loaded from) */
	protected final ClassLoader ROOT = ClassLoader.getSystemClassLoader();
	/**
	 * Creates a new SystemOnlyClassLoader 
	 */
	public SystemOnlyClassLoader()  {
		super(new URL[]{});
		
	}
	
	/**
	 * Attempts to locate the named class that is already loaded and if not found, attempts to load it.
	 * @param name The name of the class to load
	 * @return the loaded class
	 * @throws ClassNotFoundException
	 */
	public Class<?> forReal(String name) throws ClassNotFoundException {
			return findSystemClass(name);
	}
	

	public URL getResource(String name) {
		return null;
	}
	
	public Enumeration<URL> getResources(String name) {
		return null;
	}
	
	public InputStream getResourceAsStream(String name) {
		return null;
	}
	
	public URL getRealResource(String name) {
		return ROOT.getResource(name);
	}
	
	public Enumeration<URL> getRealResources(String name) throws IOException {
		return ROOT.getResources(name);
	}
	
	public InputStream getRealResourceAsStream(String name) {
		return ROOT.getResourceAsStream(name);
	}
	

	/**
	 * Always throws ClassNotFoundException
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Class<?> loadSystemClass(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException(name);
	}
	
	/**
	 * Always throws ClassNotFoundException
	 * @param name
	 * @param resolve
	 * @return
	 * @throws ClassNotFoundException
	 */
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadSystemClass(name);
	}
	
	/**
	 * Always throws ClassNotFoundException
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 */
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loadSystemClass(name);
	}	
	
	/**
	 * Always throws ClassNotFoundException
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 */
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return loadSystemClass(name);
	}
	

}
