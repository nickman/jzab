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
 * <p>Title: IsolatedArchiveLoader</p>
 * <p>Description: Isolating classloader that restricts the classes it loads to those available from the passed URLs and the root system classloader.</p>  
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.classloader.IsolatedArchiveLoader</code></p>
 */
public class IsolatedArchiveLoader extends URLClassLoader {
	/** This classloader's parent which is delegated system class loads */
	protected static SystemOnlyClassLoader ncl;

	/**
	 * Creates a new IsolatedArchiveLoader that restricts its classloading to the passed URLs. 
	 * @param urls The URLs this classloader will load from
	 * @throws Exception
	 */
	public IsolatedArchiveLoader(URL...urls)  {
		super(urls, getNullClassLoader());		
		StringBuilder b = new StringBuilder();
		if(urls!=null) {
			for(URL url: urls) {
				b.append("\n\t").append(url);
			}
		}
		System.out.println("Isolated Class Loader for URLs: [" + b + "]");
	}
	
	/**
	 * Returns the SystemOnlyClassLoader that will be an IsolatedClassLoader's parent.
	 * @return the SystemOnlyClassLoader
	 */
	private static ClassLoader getNullClassLoader()  {
		ncl = new SystemOnlyClassLoader();
		return ncl;
	}
	
	
	/**
	 * Attempts to load the named the class from the configured URLs and if not found, delegates to the parent.
	 * @param name The class name
	 * @return the loaded class
	 * @throws ClassNotFoundException
	 */
	private Class<?> loadSystemClass(String name) throws ClassNotFoundException {
		try {
			Class<?> clazz = super.findClass(name);
			return clazz;
		} catch (ClassNotFoundException cle) {
			return ncl.forReal(name);
		}
	}
	

	public URL getResource(String name) {
		URL url = super.getResource(name);
		if(url==null) {
			url = ncl.getRealResource(name);
		} else {
		}
		return url;
	}
	
	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL> en = super.getResources(name);
		if(en==null) {
			en = ncl.getRealResources(name);
		} else {
		}
		return en;
	}
	
	public InputStream getResourceAsStream(String name) {
		InputStream is = super.getResourceAsStream(name);
		if(is==null) {
			is = ncl.getRealResourceAsStream(name);
		} else {
		}
		return is;
	}

	/**
	 * Attempts to load the named the class from the configured URLs and if not found, delegates to the parent.
	 * @param name The class name
	 * @param resolve true to resolve the class.
	 * @return the loaded class
	 * @throws ClassNotFoundException
	 */	
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadSystemClass(name);
	}
	
	/**
	 * Attempts to load the named the class from the configured URLs and if not found, delegates to the parent.
	 * @param name The class name
	 * @return the loaded class
	 * @throws ClassNotFoundException
	 */
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loadSystemClass(name);
	}	
	
	/**
	 * Attempts to load the named the class from the configured URLs and if not found, delegates to the parent.
	 * @param name The class name
	 * @return the loaded class
	 * @throws ClassNotFoundException
	 */	
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return loadSystemClass(name);
	}

	


}
