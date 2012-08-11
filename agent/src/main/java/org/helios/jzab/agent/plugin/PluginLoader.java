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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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
				
				
				
				try {
					
					if(name.isEmpty()) throw new Exception("No name defined in plugin [" + XMLHelper.renderNode(n) + "]");
					if(type.isEmpty()) throw new Exception("No type defined in plugin [" + name + "]");
					if("java-agent".equalsIgnoreCase(type.trim())) {
						String agentArgs = getJavaAgentArgs(n);
						loadJavaAgent(isolated, new URL(url), name, null, agentArgs);
					} else if("jzab-plugin".equalsIgnoreCase(type.trim())) {
						String[] pluginArgs = getPluginArgs(n);
						Properties props = getProperties(n);
						Node pluginConfigNode = XMLHelper.getChildNodeByName(n, "plugin-config", false);
						loadPlugin(isolated, new URL(url), name, props, pluginConfigNode, pluginArgs);
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
	 * Loads the plugin arguments from the configuration node
	 * @param pluginNode The plugin configuration node
	 * @return An array of arguments
	 */
	protected String[] getPluginArgs(Node pluginNode) {
		Set<String> args = new HashSet<String>();
		for(Node argNode: XMLHelper.getChildNodesByName(pluginNode, "plugin-arg", false)) {
			args.add(XMLHelper.getNodeTextValue(argNode));
		}
		return args.toArray(new String[args.size()]);
	}
	
	/**
	 * Reads a plugin's properties from the properties node
	 * @param pluginNode The plugin's config node
	 * @return the [possibly empty] properties
	 */
	protected Properties getProperties(Node pluginNode) {
		Node propsNode = XMLHelper.getChildNodeByName(pluginNode, "properties", false);
		Properties p = new Properties();
		if(propsNode!=null) {
			String text = XMLHelper.getNodeTextValue(propsNode).trim();
			try {
				p.load(new StringReader(text));
			} catch (IOException e) {
				throw new RuntimeException("Failed to read plugin properties from node [" + XMLHelper.getStringFromNode(propsNode) + "]", e);
			}
		}
		return p;
	}
	
	/**
	 * Bootstraps a plugin
	 * @param isolated Indicates if this plugin's classpath should be isolated from the main or extend it.
	 * @param jarUrl The URL of the plugin jar
	 * @param name The name of the plugin that will be used to reference it's services
	 * @param props The plugin properties
	 * @param xmlConfig The plugin configuration
	 * @param pluginArgs The plugin arguments 
	 */
	public void loadPlugin(boolean isolated, URL jarUrl, String name, Properties props, Node xmlConfig, String... pluginArgs) {
		if(jarUrl==null) throw new IllegalArgumentException("Passed Plugin URL was null]", new Throwable());
		if(name==null) throw new IllegalArgumentException("Passed Plugin Name was null]", new Throwable());
		log.debug("Loading Plugin [{}] from URL [{}]. Isolated:" + isolated, name, jarUrl );
		try {
			URL[] urls = getRelatedArchives(jarUrl);
			URLClassLoader pluginClassLoader = isolated ? new IsolatedArchiveLoader(urls) : new URLClassLoader(urls, getClass().getClassLoader());
			log.debug("ClassLoader business for plugin load: \n{}", Arrays.toString(pluginClassLoader.getURLs()));
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(pluginClassLoader);
				String manifestUrl =  "jar:" + jarUrl.toExternalForm() + "!/META-INF/MANIFEST.MF";
				log.debug("Hoping to find the manifest at [{}]", manifestUrl);
				//invokeJar(pluginClassLoader, manifestUrl, "Main-Class", "main", new Class[]{new String[0].getClass()}, new Object[]{pluginArgs});
				String className = getManifestEntry(manifestUrl, "Main-Class");
				Class<?> pluginClazz = Class.forName(className, true, pluginClassLoader);
				IPluginConfiguration config = pluginConfigurator(pluginClazz);
				config.setProperties(props);
				if(xmlConfig!=null) {
					config.setXmlConfiguration(xmlConfig);
				}
				config.boot(pluginArgs);
				log.info("Started Plugin Instance of [{}]", name);
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
	 * Returns an array of jar file URLs including the passed one and any others in the same directory.
	 * Currently only supports finding additional jars when the URL protocol is <b><code>file</code></b>.
	 * @param jarUrl The jar file URL of the main plugin loader
	 * @return an array of jar file URLs
	 */
	protected URL[] getRelatedArchives(URL jarUrl) {
		if(jarUrl==null) throw new IllegalArgumentException("The passed jarUrl was null", new Throwable());
		Set<URL> urls = new HashSet<URL>();
		urls.add(jarUrl);
		if(jarUrl.getProtocol().equalsIgnoreCase("file")) {
			File dir = new File(jarUrl.getFile()).getParentFile().getAbsoluteFile();
			for(File archive: dir.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {					
					return name.toLowerCase().endsWith(".jar");
				}
			})) {
				try { urls.add(archive.toURI().toURL()); } catch (Exception e) {}
			}
		}
		return urls.toArray(new URL[urls.size()]);

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
	 * @return the return value from the target method invocation
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
		}
		Object pluginInstance = pluginClazz.newInstance();
		return targetMethod.invoke(pluginInstance, arguments);		
	}
	
	/**
	 * Retrieves a manifest entry value
	 * @param manifestUrl The string representation of the manifest URL in the JAR
	 * @param manifestEntry The name of the manifest entry (must be in main) 
	 * @return the manifest entry value
	 */
	protected String getManifestEntry(String manifestUrl, String manifestEntry) {
		InputStream is = null;
		try {
			Manifest manifest = null;
			is = new URL(manifestUrl ).openStream();
			manifest = new Manifest(is);
			log.debug("Read manifest [{}]", manifest);
			String value = manifest.getMainAttributes().getValue(manifestEntry);
			log.debug("Manifest value [{}]:[{}]", manifestEntry, value);
			return value;
		} catch (Exception e) {
			throw new RuntimeException("Failed to retrieve a manifest entry value for [" + manifestEntry + "] in manifest URL [" + manifestUrl + "]", e);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception ex) {}
		}
		
	}
	
	/**
	 * Generates a live configuration proxy for the passed class
	 * @param pluginClass The plugin class
	 * @return the configurator
	 */
	protected IPluginConfiguration pluginConfigurator(final Class<?> pluginClass) {
		try {
			final Object plugin = pluginClass.newInstance();
			return (IPluginConfiguration)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{IPluginConfiguration.class}, new InvocationHandler(){
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					try {
						Method actualMethod = null;
						try {
							actualMethod = pluginClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
						} catch (Exception e) {
							actualMethod = pluginClass.getMethod(method.getName(), method.getParameterTypes());
						}
						actualMethod.setAccessible(true);
						return actualMethod.invoke(plugin, args);
						
					} catch (NoSuchMethodException ne) {}
					return null;
				}
			});
		} catch (Exception e) {
			log.error("Failed to generate IPluginConfiguration for plugin class [{}]", pluginClass.getName(), e);
			throw new RuntimeException("Failed to generate IPluginConfiguration for plugin class [" + pluginClass.getName() + "]", e);
		}
	}
	
	
}
