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
package org.helios.jzab.agent.commands.impl.jmx;

import javax.management.remote.JMXServiceURL;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * <p>Title: JMXPassiveDiscoveryCommandProcessor</p>
 * <p>Description:  Passive JMX discovery command processor. Receives requests with ObjectName patterns, locates all matching instances, and returns the extracted values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.jmx.JMXPassiveDiscoveryCommandProcessor</code></p>
 */
public class JMXPassiveDiscoveryCommandProcessor extends JMXDiscoveryCommandProcessor {
	/** This processors command keys */
	public static final String COMMAND_KEY  = "jmxdp"; 
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#getLocatorKey()
	 */
	@Override
	public String getLocatorKey() {
		return COMMAND_KEY;
	}

	/**
	 * Var parameters:<ol>
	 *  <li><b>JMX Object Name</b>: The ObjectName pattern query.</li>
	 *  <li><b>Domain</b>: (Optional) Defines the MBeanServer domain in which the target MBeans are registered. Can also be interpreted as a {@link JMXServiceURL} in which case a remote connection will be used to retrieve the attribute values.</li>
	 * </ol>
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.AbstractCommandProcessor#doExecute(java.lang.String[])
	 */	
	@Override
	protected Object doExecute(String... args) throws Exception {
		JSONObject[] parentResults = (JSONObject[])super.doExecute(args);
		JSONObject result = new JSONObject();
		JSONArray array = new JSONArray();
		
		for(JSONObject jo: parentResults) {
			try {
				array.put(jo);
			}	catch (Exception e) {
				log.debug("Failed to add parent result [{}]", jo, e);
			}
		}
		try {
			result.put("data", array);
		} catch (Exception e) {
			log.warn("Error adding final [{}]", array, e);
		}
		log.debug("Returning Discovery Result:\n[{}]", result);
		return result;
	}

}
