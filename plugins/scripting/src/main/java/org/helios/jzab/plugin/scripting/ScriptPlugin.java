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
package org.helios.jzab.plugin.scripting;

import java.util.Properties;

import org.helios.jzab.plugin.scripting.engine.PluginScriptEngine;
import org.helios.jzab.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: ScriptPlugin</p>
 * <p>Description: Bootstrap for scripting plugin.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.scripting.ScriptPlugin</code></p>
 */
public class ScriptPlugin {
	/** The configured properties */
	protected final Properties scriptPluginProps = new Properties();
	/** The configured Xml configuration */
	protected Node scriptPluginXmlNode = null;
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	
	
	/**
	 * Main boot loading entry point
	 * @param args : <ol>
	 * 	<li>The optional name of this script plugin instance. If not supplied, defaults to <b><code>ScriptEnginePlugin</code></b>.</li>
	 * </ol>
	 */
	public void boot(String[] args) {
		PluginScriptEngine pse = PluginScriptEngine.get(args.length==0 ? PluginScriptEngine.DEFAULT_PLUGIN_NAME : args[0]);
		if(scriptPluginXmlNode!=null) {
			for(Node scriptNode: XMLHelper.getChildNodesByName(scriptPluginXmlNode, "script", false)) {
				String name = XMLHelper.getAttributeByName(scriptNode, "name", null);
				String ext = XMLHelper.getAttributeByName(scriptNode, "ext", null);				
				if(name==null || name.trim().isEmpty() || ext==null || ext.trim().isEmpty()) {
					log.warn("Failed to deploy configured script. Name or ext was null. [{}]", XMLHelper.getNodeTextValue(scriptNode));
					continue;
				}
				Node srcNode = XMLHelper.getChildNodeByName(scriptNode, "source", false);
				if(srcNode==null) {
					log.warn("Failed to deploy configured script [{}]:[{}]. No source defined.", name, ext);
					continue;					
				}
				String source = XMLHelper.getNodeTextValue(srcNode);
				pse.addScript(source, name, ext);
				log.debug("Added Script [{}]:[{}]", name, ext);
			}
		}
	}
	
	/**
 	 	<plugin-config>
 	 		<script name="TotalBlockCounts" ext="groovy">
 	 			<source><![CDATA[
					import java.lang.management.*;
					long totalBlocks = 0;
					ManagementFactory.getThreadMXBean().dumpAllThreads(false, false).each() {
					    totalBlocks += it.getBlockedCount();
					}
					return totalBlocks;
 	 			]]></source>
 	 		</script>
 	 	</plugin-config>

	 */
	
	/**
	 * Handles properties set by the core plugin loader
	 * @param props The properties passed by the core plugin loader
	 */
	public void setProperties(Properties props) {
		
	}
	
	/**
	 * Handles XML configuration set by the core plugin loader
	 * @param configNode The XML node passed by the core plugin loader
	 */
	public void setXmlConfiguration(Node configNode) {
		this.scriptPluginXmlNode = configNode;
	}
	
	
	@SuppressWarnings("javadoc")
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
