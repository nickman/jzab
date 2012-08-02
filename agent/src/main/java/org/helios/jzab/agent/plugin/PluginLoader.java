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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
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
	public int loadPlugins(Node configNode) {
		if(configNode==null) throw new IllegalArgumentException("Passed configuration node was null", new Throwable());
		log.info("Loading Plugins");
		int cnt = 0;
		Node cpNode = XMLHelper.getChildNodeByName(configNode, "plugins", false);
		if(cpNode!=null) {
			for(Node n: XMLHelper.getChildNodesByName(cpNode, "plugin", false)) {
				String type = XMLHelper.getAttributeByName(n, "type", "").trim();
				String url = XMLHelper.getAttributeByName(n, "url", "").trim();
				boolean isolated = XMLHelper.getAttributeByName(n, "isolated", false);
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
						loadJavaAgent(isolated, new URL(url), name, null, agentArgs);
					} else if("jzab-plugin".equalsIgnoreCase(type.trim())) {
						loadPlugin(isolated, new URL(url), name);
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
	 * Bootstraps a plugin
	 * @param isolated Indicates if this plugin's classpath should be isolated from the main or extend it.
	 * @param jarUrl The URL of the plugin jar
	 * @param name The name of the plugin that will be used to reference it's services 
	 */
	public void loadPlugin(boolean isolated, URL jarUrl, String name) {
		if(jarUrl==null) throw new IllegalArgumentException("Passed Plugin URL was null]", new Throwable());
		if(name==null) throw new IllegalArgumentException("Passed Plugin Name was null]", new Throwable());
		log.debug("Loading Plugin [{}] from URL [{}]. Isolated:" + isolated, name, jarUrl );
		try {
			URLClassLoader pluginClassLoader = isolated ? new IsolatedArchiveLoader(jarUrl) : new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
			log.debug("ClassLoader business for plugin load: \n{}", Arrays.toString(pluginClassLoader.getURLs()));
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(pluginClassLoader);
				String manifestUrl =  "jar:" + jarUrl.toExternalForm() + "!/META-INF/MANIFEST.MF";
				log.debug("Hoping to find the manifest at [{}]", manifestUrl);
				invokeJar(pluginClassLoader, manifestUrl, "Main-Class", "main", new Class[]{String.class, String.class});
				log.info("Started Plugin [{}]", name);
			} finally {
				Thread.currentThread().setContextClassLoader(cl);
			}			
		} catch (Exception e) {			
			log.debug("Failed to Load Plugin [{}]", name, e );
			log.warn("Failed to Load Plugin [{}]", name);
			throw new RuntimeException("Failed to Load Plugin [" + name + "]", e);			
		}
		
	}
	
	
	/**
	 * Loads a JavaAgent from the passed URL
	 * @param isolated Indicates if this plugin's classpath should be isolated from the main or extend it.
	 * @param jarUrl The URL of the JavaAgent jar
	 * @param name The name of the JavaAgent that will be used to reference it's services 
	 * @param instr The JavaAgent instrumentation instance. Ignored if null.
	 * @param agentArgs The JavaAgent initialization string. Ignored if null.
	 */
	public void loadJavaAgent(boolean isolated, URL jarUrl, String name, Instrumentation instr, String agentArgs) {
		if(jarUrl==null) throw new IllegalArgumentException("Passed JavaAgent URL was null]", new Throwable());
		if(name==null) throw new IllegalArgumentException("Passed JavaAgent Name was null]", new Throwable());
		log.debug("Loading JavaAgent [{}] from URL [{}]. Isolated:" + isolated, name, jarUrl );
		try {
			URLClassLoader pluginClassLoader = isolated ? new IsolatedArchiveLoader(jarUrl) : new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
			log.debug("ClassLoader business for plugin load: \n{}", Arrays.toString(pluginClassLoader.getURLs()));
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			
			try {
				Thread.currentThread().setContextClassLoader(pluginClassLoader);
				String manifestUrl =  "jar:" + jarUrl.toExternalForm() + "!/META-INF/MANIFEST.MF";
				log.debug("Hoping to find the manifest at [{}]", manifestUrl);
				try {
					invokeJar(pluginClassLoader, manifestUrl, "Agent-Class", "agentmain", new Class[]{String.class, Instrumentation.class}, agentArgs==null ? "" : agentArgs.trim(), instr);
				} catch (Exception e) {
					log.debug("===  Ooops. Failed to load that way [{}]", e.toString());
					invokeJar(pluginClassLoader, manifestUrl, "Agent-Class", "agentmain", new Class[]{String.class}, agentArgs==null ? "" : agentArgs.trim());
				}
				log.info("Started Java-Agent [{}]", name);
			} finally {
				Thread.currentThread().setContextClassLoader(cl);
			}			
		} catch (Exception e) {			
			log.debug("Failed to Load JavaAgent [{}]", name, e );
			log.warn("Failed to Load JavaAgent [{}]", name);
			throw new RuntimeException("Failed to Load JavaAgent [" + name + "]", e);			
		}
	}
	
	/**
	 * Locates the manifest for a jar, finds the class specified by the passed manifest entry and invokes the named method with the specified signature
	 * @param classLoader The classloader to load the manifest from
	 * @param manifestUrl The string representation of the manifest URL in the JAR
	 * @param manifestEntry The name of the manifest entry (must be in main) that identifies the target class
	 * @param methodName The name of the method in the identified class to invoke
	 * @param signature The class signature of the method
	 * @param arguments The arguments to pass to the invocation
	 * @throws Exception There's a bunch of places where an exception might be thrown so this is very generic
	 */
	protected Object invokeJar(ClassLoader classLoader, String manifestUrl, String manifestEntry, String methodName, Class<?>[] signature, Object...arguments) throws Exception {
		Manifest manifest = null;
		manifest = new Manifest(new URL(manifestUrl ).openStream());
		log.debug("Read manifest [{}]", manifest);
		String className = manifest.getMainAttributes().getValue(manifestEntry);
		log.debug("Plugin Class [{}] Found", className );
		Class<?> pluginClazz = Class.forName(className, true, classLoader);
		log.debug("Plugin Class [{}] Loaded", className );
		Method targetMethod = null;
		try {
			targetMethod = pluginClazz.getDeclaredMethod(methodName, signature);
		} catch (Exception e) {
			targetMethod = pluginClazz.getMethod(methodName, signature);
		}
		log.debug("Found target method [{}]", targetMethod);
		targetMethod.setAccessible(true);
		if(Modifier.isStatic(targetMethod.getModifiers())) {
			return targetMethod.invoke(null, arguments);
		} else {
			return targetMethod.invoke(pluginClazz.newInstance(), arguments);
		}
	}
	
	
}
