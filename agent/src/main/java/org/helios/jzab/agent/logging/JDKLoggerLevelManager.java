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
package org.helios.jzab.agent.logging;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.LogManager;

import org.helios.jzab.util.JMXHelper;


/**
 * <p>Title: JDKLoggerLevelManager</p>
 * <p>Description: Logger level manager for JDK logging</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.logging.JDKLoggerLevelManager</code></p>
 */

public class JDKLoggerLevelManager extends SimpleJMXLoggerLevelManager {

	/**
	 * Creates a new JDKLoggerLevelManager
	 */
	public JDKLoggerLevelManager() {
		super("setLoggerLevel", "getLoggerLevel", "", LoggerManager.JDK_OBJECT_NAME, LoggerManager.JDK_LEVEL_NAMES);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.logging.ILoggerLevelManager#reloadConfiguration(java.lang.String)
	 */
	@Override
	public void reloadConfiguration(String location) {
		if(location==null) throw new IllegalArgumentException("The passed location was null", new Throwable());
		URL url = null;
		try {
			url = new URL(location.trim());			
		} catch (Exception e) {
			File f = new File(location.trim());
			if(f.canRead() && f.isFile()) {
				try {
					url = f.toURI().toURL();
				} catch (Exception e2) {}
			}
		}
		if(url!=null) {
			InputStream is = null;
			try {
				is = url.openStream();
				LogManager.getLogManager().readConfiguration(is);
				return;				
			} catch (Exception ei) {
				throw new RuntimeException("Failed to read configuration from [" + location + "], ei");
			} finally {
				if(is!=null) {
					try { is.close(); } catch (Exception ex) {}
				}
			}
		} 
		throw new RuntimeException("Failed to read configuration from [" + location + "]");
	}

}
