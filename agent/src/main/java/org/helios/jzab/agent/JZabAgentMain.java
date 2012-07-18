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
package org.helios.jzab.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.helios.jzab.agent.commands.CommandProcessorLoader;
import org.helios.jzab.agent.internal.jmx.ScheduledThreadPoolFactory;
import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.agent.net.AgentListener;
import org.helios.jzab.agent.plugin.PluginLoader;
import org.helios.jzab.util.XMLHelper;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * <p>Title: JZabAgentMain</p>
 * <p>Description: The jzab agent command line entry point.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.JZabAgentMain</code></p>
 */

public class JZabAgentMain {
	/** The actual jzab home directory */
	protected String jZabHomeDir = null;
	/** The actual located configuration file */
	protected String locatedConfigFile = null;
	/** The parsed configuration node */
	protected Node parsedConfigNode = null;
	
	
	/** The agent listeners keyed by name */
	protected Map<String, AgentListener> listeners = new ConcurrentHashMap<String, AgentListener>();
	
	
	/** Static class logger */
	private static final Logger log = LoggerFactory.getLogger(JZabAgentMain.class);
	
	/** The jZab directory name */
	public static final String JZAB_DIR_NAME = ".jzab";
	/** The jZab config file name */
	public static final String JZAB_CONFIG_NAME = "jzab.xml";
	
	/** The fully qualified default jZab home directory */
	public static final String JZAB_HOME = String.format("%s%s%s", System.getProperty("user.home"), File.separator, JZAB_DIR_NAME);
	/** The fully qualified default jZab config file */
	public static final String DEFAULT_CONF_FILE = String.format("%s%s%s", JZAB_HOME, File.separator, JZAB_CONFIG_NAME);
	
	/** <code>"="</code> Split Regex */
	public static final Pattern EQ_SPLIT = Pattern.compile("=");
	/** <code>","</code> Split Regex */
	public static final Pattern COMMA_SPLIT = Pattern.compile(",");
	
	
	/**
	 * Creates a new JZabAgentMain
	 * @param args Same as {@link JZabAgentMain#main(String[])}
	 */
	private JZabAgentMain(String...args) {
		processCommandLineArgs(args);
		if(locatedConfigFile==null) {			
			if(testConfFile(DEFAULT_CONF_FILE)) {
				locatedConfigFile = DEFAULT_CONF_FILE;
			}			
		}
		if(locatedConfigFile!=null) {
			log.info("Bootstrap Config File [{}] jZab Home Directory [{}]", locatedConfigFile, jZabHomeDir);
		} else {
			log.error("Unable to locate jZab Configuration File. Default location is [{}]", DEFAULT_CONF_FILE);
			System.exit(-1);
		}
	}
	
	
	/**
	 * Boots up the configured components
	 * @throws Exception thrown on any boot error
	 */
	public void boot() throws Exception {
		log.info("Booting Agent from [{}]...", locatedConfigFile);
		// First, find all the thread pools and start them
		try {
			log.info("Booted [{}] Thread Pools", bootThreadPools());
		} catch (Exception e) {
			log.error("Failed to start agent thread pools", e);
			throw new Exception("Failed to start agent thread pools", e);
		}
		// Find all the schedulers and start them
		try {
			log.info("Booted [{}] Schedulers", bootSchedulers());
		} catch (Exception e) {
			log.error("Failed to start agent schedulers", e);
			throw new Exception("Failed to start agent schedulers", e);
		}				
		try {
			log.info("Booted [{}] AgentListeners", bootAgentListeners());			
		} catch (Exception e) {
			log.error("Failed to start agent listeners", e);
			throw new Exception("Failed to start agent listeners", e);
		}
		bootCommandProcessors();
		loadPlugins();
		//loadNativeAgent();   // ONLY LOAD IF SPECIED IN JZAB.XML
	}
	
	public void loadNativeAgent() {
		try {
			log.info("Loading native agent");
			URL url = getClass().getClassLoader().getResource("plugins/native/helios-native-jmx.jar");
			log.info("Native Agent URL [" + url + "]");
			URLClassLoader ucl = new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(ucl);
				Class<?> clazz = Class.forName("org.helios.nativex.jmx.ServiceBootStrap", true, ucl);
				for(Method m : clazz.getDeclaredMethods()) {
					if(m.getName().equals("boot")) {
						m.invoke(null, true, null);
					}
				}
			} finally {
				Thread.currentThread().setContextClassLoader(cl);
			}
		} catch (Exception e) {
			log.warn("Failed to load native agent");
			if(log.isDebugEnabled()) {
				log.debug("Native Agent Load Failure", e);
			}
			
		}
	}
	
	protected void loadPlugins() {
		PluginLoader pl = new PluginLoader();
		pl.loadPlugins(parsedConfigNode);
	}
	
	/**
	 * Stops the agent.
	 */
	public void shutdown() {
		
	}
	
	/**
	 * Loads the agent's configured comand processors
	 * @return the number of command processors loaded
	 */
	protected int bootCommandProcessors() {
		CommandProcessorLoader cpl = new CommandProcessorLoader();
		return cpl.loadCommandProcessors(parsedConfigNode);
	}
	
	/**
	 * Starts all the configured thread pools
	 * @return The number of booted thread pools
	 */
	protected int bootThreadPools() {
		log.debug("Booting Thread Pools");
		int cnt = 0;
		Node tpNodeRoot = XMLHelper.getChildNodeByName(parsedConfigNode, "thread-pools", false);
		if(tpNodeRoot!=null) {
			for(Node tpNode : XMLHelper.getChildNodesByName(tpNodeRoot, "thread-pool", false)) {
				String tpName = ThreadPoolFactory.newCachedThreadPool(tpNode).getName();
				log.info("Started ThreadPool [{}]", tpName);
				cnt++;
			}
		}		
		return cnt;
	}
	
	/**
	 * Starts all the configured schedulers
	 * @return The number of booted schedulers
	 */
	protected int bootSchedulers() {
		log.debug("Booting Schedulers");
		int cnt = 0;
		Node tpNodeRoot = XMLHelper.getChildNodeByName(parsedConfigNode, "thread-pools", false);
		if(tpNodeRoot!=null) {
			for(Node tpNode : XMLHelper.getChildNodesByName(tpNodeRoot, "scheduler", false)) {
				String sName = ScheduledThreadPoolFactory.newScheduler(tpNode).getName();
				log.info("Started Scheduler [{}]", sName);
				cnt++;
			}
		}		
		return cnt;
	}
	
	
	/**
	 * Starts all the configured agent listeners
	 * @return the number of booted listeners
	 * @throws Exception thrown if any error occurs when setting up the listeners
	 */
	protected int bootAgentListeners() throws Exception {		
		log.debug("Booting AgentListeners");
		int cnt = 0;
		
		for(Node listenerNode : XMLHelper.getChildNodesByName(parsedConfigNode, "agent-listener", false)) {
			AgentListener listener = new AgentListener(listenerNode);
			if(listeners.containsKey(listener.getListenerName())) {
				throw new RuntimeException("Duplicate definition of AgentListener [" + listener.getListenerName() + "]");
			}
			listener.start();
			log.info("Started AgentListener [{}]", listener.getListenerName());
			listeners.put(listener.getListenerName(), listener);
			cnt++;
		}
		return cnt;
	}
	
	/**
	 * The jzab agent command line entry point
	 * @param args Optional arguments are as follows:<ul>
	 * 	<li><b>conf=&lt;config&gt;</b>: Overrides the location of the <code>jzab.xml</code> configuration file. If in URL format, will read from that URL. Comma separateds will be processed in sequence until one hits.</li>
	 *  <li><b></b>:</li>
	 *  <li><b></b>:</li>
	 * </ul>
	 */
	public static void main(String[] args) {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
		log.info("jZab Agent");
		JZabAgentMain main = new JZabAgentMain(args);
		try {
			main.boot();
			log.info("\n\t=============================\n\tjZab Agent Successfully Started\n\t=============================\n");
		} catch (Exception e) {
			log.error("Agent Start Failed",e);
			System.exit(-1);
		}
	}
	
	/**
	 * The pre-main entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		main(new String[]{agentArgs});
	}
	
	/**
	 * The pre-main entry point for JVMs not supporting a <b><code>java.lang.instrument.Instrumentation</code></b> implementation.
	 * @param agentArgs The agent bootstrap arguments
	 */	
	public static void premain(String agentArgs) {
		main(new String[]{agentArgs});
	}
	
	/**
	 * The agent attach entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		main(new String[]{agentArgs});
	}
	
	/**
	 * The agent attach entry point for JVMs not supporting a <b><code>java.lang.instrument.Instrumentation</code></b> implementation.
	 * @param agentArgs The agent bootstrap arguments
	 */
	public static void agentmain(String agentArgs) {
		main(new String[]{agentArgs});
	}
		
	
	/**
	 * Parses the command line arguments
	 * @param args the command line arguments
	 */
	void processCommandLineArgs(String...args) {
		if(args==null || args.length < 1) return;
		for(int i = 0; i < args.length; i++) {
			String[] argPair = EQ_SPLIT.split(args[i]);
			if("CONF".equalsIgnoreCase(argPair[0])) {
				String[] locations = COMMA_SPLIT.split(argPair[1]);
				log.debug("Testing config file locations [{}]", Arrays.toString(locations));
				for(String location: locations) {
					location = location.trim();
					try {
						if(testConfFile(location)) {
							locatedConfigFile = location;
							break;
						}
					} catch (Exception e) {
						log.debug("Failed to validate config file location [{}]", location);						
					}
				}
			}
		}
	}
	
	/**
	 * Tests the configuration file or URL  for readability and format
	 * @param fileName The file name or URL to test
	 * @return true if the file looks good, false otherwise
	 */
	boolean testConfFile(String fileName) {
		if(fileName==null || fileName.trim().isEmpty()) {
			log.warn("The supplied configuration file name was null or blank");
			return false;
		}
		URL url = null;
		Document doc = null;
		try {
			url = new URL(fileName);
		} catch (Exception e) {}
		if(url==null) {
			File cf = new File(fileName);
			if(!cf.canRead()) {
				log.warn("The supplied configuration file [{}] could not be read", fileName);
				return false;
			}
			doc = XMLHelper.parseXML(cf);
		} else {
			doc = XMLHelper.parseXML(url);
		}
		try {			
			// jZabHomeDir
			
			Node node = doc.getDocumentElement();
			String rootNode = node.getNodeName();
			if("jzab".equalsIgnoreCase(rootNode)) {
				log.debug("The configuration file [{}] looks good.", fileName);		
				parsedConfigNode = node;
				jZabHomeDir = XMLHelper.getAttributeByName(node, "homeDir", "");
				File f = new File(jZabHomeDir);
				if(!f.exists() || !f.isDirectory() ) {
					jZabHomeDir = JZAB_HOME;
				} else {
					jZabHomeDir = f.getAbsolutePath();
				}
				System.setProperty("jzab.home", jZabHomeDir);
				return true;
			}
			log.error("The supplied configuration file [{}] failed validation because the root node was not \"jzab\" but {}", fileName, rootNode);
			return false;
		} catch (Exception e) {
			log.error("The supplied configuration file [{}] failed validation", fileName, e);
			return false;
		}
	}

}
