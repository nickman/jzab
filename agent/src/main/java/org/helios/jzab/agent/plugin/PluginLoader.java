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
package org.helios.jzab.agent.plugin;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.jar.Manifest;

import org.helios.jzab.agent.classloader.IsolatedArchiveLoader;
import org.helios.jzab.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: PluginLoader</p>
 * <p>Description: Service to load different types of plugins and fixtures</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.plugin.PluginLoader</code></p>
 */

public class PluginLoader {
	/** Instance logger */
	private final Logger log = LoggerFactory.getLogger(getClass());

	
	/**
	 * Loads plugins and fictures defined in the jzab.xml configuration file
	 * @param configNode The root configuration xml node
	 * @return the number of plugins loaded
	 */
	@SuppressWarnings("unchecked")
	public int loadPlugins(Node configNode) {
		if(configNode==null) throw new IllegalArgumentException("Passed configuration node was null", new Throwable());
		log.info("Loading Plugins");
		int cnt = 0;
		Node cpNode = XMLHelper.getChildNodeByName(configNode, "plugins", false);
		if(cpNode!=null) {
			for(Node n: XMLHelper.getChildNodesByName(cpNode, "plugin", false)) {
				String type = XMLHelper.getAttributeByName(n, "type", "").trim();
				String url = XMLHelper.getAttributeByName(n, "url", "").trim();
				/*
				 * <plugin  name="jmx-native-agent" type="java-agent" url="file://home/nwhitehead/.m2/repository/org/helios/helios-native/helios-native-jmx/1.0-SNAPSHOT/helios-native-jmx-1.0-SNAPSHOT-launcher.jar" />
				 */
				String name = XMLHelper.getAttributeByName(n, "name", "").trim();
				// ========================================
				//	Just doing the basic java-agents for now.
				// ========================================
				String agentArgs = getJavaAgentArgs(n);
				
				
				try {
					
					if(name.isEmpty()) throw new Exception("No name defined in plugin [" + XMLHelper.renderNode(n) + "]");
					if(type.isEmpty()) throw new Exception("No type defined in plugin [" + name + "]");
					if("java-agent".equalsIgnoreCase(type.trim())) {
						loadJavaAgent(new URL(url), name, null, agentArgs);
					}
				} catch (Exception e) {
					log.warn("Failed to load plugin [{}]", name, e);
				}
			}
		}
		return cnt;
	}
	
	/**
	 * Extracts the agent arguments for a java-agent plugin
	 * @param pluginNode The plugin configuration node
	 * @return The agent arguments which may be null
	 */
	protected String getJavaAgentArgs(Node pluginNode) {
		String args = null;
		Node agentArgs = XMLHelper.getChildNodeByName(pluginNode, "java-agent-args", false);
		if(agentArgs!=null) {
			args = XMLHelper.getNodeTextValue(agentArgs);
		}
		return args;
	}
	
	
	/**
	 * Loads a JavaAgent from the passed URL
	 * @param jarUrl The URL of the JavaAgent jar
	 * @param name The name of the JavaAgent that will be used to reference it's services 
	 * @param instr The JavaAgent instrumentation instance. Ignored if null.
	 * @param agentArgs The JavaAgent initialization string. Ignored if null.
	 */
	public void loadJavaAgent(URL jarUrl, String name, Instrumentation instr, String agentArgs) {
		if(jarUrl==null) throw new IllegalArgumentException("Passed JavaAgent URL was null]", new Throwable());
		if(name==null) throw new IllegalArgumentException("Passed JavaAgent Name was null]", new Throwable());
		log.debug("Loading JavaAgent [{}] from URL [{}]", name, jarUrl );
		try {
			IsolatedArchiveLoader isolator = new IsolatedArchiveLoader(jarUrl);
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(isolator);
				String manifestUrl = "jar:" + jarUrl.toExternalForm() + "!/META-INF/MANIFEST.MF";
				log.debug("Loading JavaAgent [{}] manifest from URL [{}]", name, manifestUrl );
				Manifest manifest = null;
				try {
					manifest = new Manifest(new URL(manifestUrl ).openStream());
				} catch (Exception e) {
					log.debug("Failed to Load JavaAgent manifest from URL [{}]", manifestUrl, e );
					throw new Exception("Failed to Load JavaAgent manifest");
				}
				String className = manifest.getMainAttributes().getValue("Agent-Class");
				log.debug("JavaAgent [{}] Agent-Class: [{}]", name, className );
				Class<?> agentClazz = Class.forName(className, true, isolator);
				if(instr!=null) {
					agentClazz.getDeclaredMethod("agentmain", String.class, Instrumentation.class).invoke(null, agentArgs==null ? "" : agentArgs.trim(), instr);
				} else {
					agentClazz.getDeclaredMethod("agentmain", String.class).invoke(null, agentArgs==null ? "" : agentArgs);
				}				
				log.info("Started Plugin [{}]", name);
			} finally {
				Thread.currentThread().setContextClassLoader(cl);
			}			
		} catch (Exception e) {			
			log.debug("Failed to Load JavaAgent [{}]", name, e );
			log.warn("Failed to Load JavaAgent [{}]", name);
			throw new RuntimeException("Failed to Load JavaAgent [" + name + "]", e);			
		}
	}
	
	
	
}
