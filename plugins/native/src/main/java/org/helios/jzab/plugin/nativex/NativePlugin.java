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

import java.lang.reflect.Method;
import java.util.Properties;

import org.helios.jzab.plugin.nativex.plugin.RegistrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: NativePlugin</p>
 * <p>Description: The native OS plugin bootstrap</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.NativePlugin</code></p>
 */
public class NativePlugin {
	/** The configured properties */
	private final Properties nativePluginProps = new Properties();
	/** The configured Xml configuration */
	private Node nativePluginXmlNode = null;
	
	
	/**
	 * Main boot loading entry point
	 * @param args : <ol>
	 * 	<li>The optional name of this script plugin instance. If not supplied, defaults to <b><code>ScriptEnginePlugin</code></b>.</li>
	 * </ol>
	 */
	public void boot(String[] args) {
		if(System.getProperty("org.helios.jzab.agent.version") !=null) {
			HeliosSigar.getInstance();
			Logger log = LoggerFactory.getLogger(HeliosSigar.class);
			log.info("{}", HeliosSigar.getInstance());
			RegistrationType regType = null;
			if(args.length>0) {
				try { 
					regType = RegistrationType.forName(args[0]);
				} catch (Exception ex) {
					regType = RegistrationType.GENERIC;
				}
			}
			try {
				if(regType.equals(RegistrationType.IPLUGIN)) {
					bootPlugin("org.helios.jzab.plugin.nativex.JZabAgentBoot", nativePluginProps);
				} else {
					bootPlugin("org.helios.jzab.plugin.nativex.GenericAgentBoot", nativePluginProps);
				}
			} catch (Throwable t) {
				t.printStackTrace(System.err);
			}
			
		} else {
			HeliosSigar.main(args);
		}
	}
	
	/**
	 * Booots the native plugins subcomponents
	 * @param className The boot class name
	 * @param props The jzab.xml provided properties
	 */
	private void bootPlugin(String className, Properties props) {
		try {
			Class<?> clazz = Class.forName(className);
			Method bootMethod = clazz.getDeclaredMethod("bootPlugin", Properties.class); 
			bootMethod.invoke(null, new Object[]{props});
		} catch (Throwable e) {
			throw new RuntimeException("Failed to boot plugin", e);
		}
	}
	
	
	/**
	 * Handles properties set by the core plugin loader
	 * @param props The properties passed by the core plugin loader
	 */
	public void setProperties(Properties props) {
		if(props!=null) {
			nativePluginProps.putAll(props);
		}
	}
	
	/**
	 * Handles XML configuration set by the core plugin loader
	 * @param configNode The XML node passed by the core plugin loader
	 */
	public void setXmlConfiguration(Node configNode) {
		if(configNode!=null) {
			nativePluginXmlNode = configNode;
		}
	}
	
	
	

}
