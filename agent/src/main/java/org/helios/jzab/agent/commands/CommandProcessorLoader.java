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
package org.helios.jzab.agent.commands;

import java.util.Properties;

import org.helios.jzab.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * <p>Title: CommandProcessorLoader</p>
 * <p>Description: Service to load and initialize command processors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.CommandProcessorLoader</code></p>
 */

public class CommandProcessorLoader {
	/** Instance logger */
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	
	/**
	 * Loads command processors defined in the jzab.xml configuration file
	 * @param configNode The root configuration xml node
	 * @return the number of processors loaded
	 */
	@SuppressWarnings("unchecked")
	public int loadCommandProcessors(Node configNode) {
		CommandManager cm = CommandManager.getInstance();
		if(configNode==null) throw new IllegalArgumentException("Passed configuration node was null", new Throwable());
		log.info("Loading CommandProcessors");
		int cnt = 0;
		Node cpNode = XMLHelper.getChildNodeByName(configNode, "command-processors", false);
		if(cpNode!=null) {
			for(Node n: XMLHelper.getChildNodesByName(cpNode, "command-processor", false)) {
				String className = "none-defined";
				String key = null;
				try {
					className = XMLHelper.getAttributeByName(n, "class", "none-defined").trim();
					key = XMLHelper.getAttributeByName(n, "key", (String)null);
					Class<ICommandProcessor> clazz = (Class<ICommandProcessor>) Class.forName(className);
					ICommandProcessor processor = clazz.newInstance();
					Properties p = new Properties();
					for(Node pNode: XMLHelper.getChildNodesByName(n, "property", false)) {						
						String pKey = XMLHelper.getAttributeByName(pNode, "name", null);
						String pValue = XMLHelper.getAttributeByName(pNode, "value", null);
						if(pKey!=null && !pKey.trim().isEmpty()  && pValue!=null && !pValue.trim().isEmpty()) {
							p.setProperty(pKey.trim(), pValue.trim());
						}						
					}
					processor.setProperties(p);
					processor.init();
					cm.registerCommandProcessor(processor, key);
					log.debug("Loaded CommandProcessor [{}]", processor.getLocatorKey() + (key==null ? "" : "," + key));
				} catch (Exception e) {
					log.warn("Failed to load command processor [{}]", className);
				}
			}
		}
		return cnt;
	}
	
}
