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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: JMXDiscoveryCommandProcessor</p>
 * <p>Description: JMX discovery command processor. Receives requests with ObjectName patterns, locates all matching instances, and returns the extracted values.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.jmx.JMXDiscoveryCommandProcessor</code></p>
 */
public class JMXDiscoveryCommandProcessor extends BaseJMXCommandProcessor {
	/** This processors command keys */
	public static final String COMMAND_KEY  = "jmxd"; 
	
	/** The regex pattern to extract tokens */
	public static final Pattern TOKEN = Pattern.compile("(\\{#(.*?)\\})");
	
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
	protected Object doExecute(String... args) throws Exception {
		if(args==null || args.length < 2) throw new IllegalArgumentException("Invalid argument count for command [" + (args==null ? 0 : args.length) + "]", new Throwable());
		ObjectName on = JMXHelper.objectName(args[0]);
		Map<String, String> tokens = extractTokens(on);
		String domain = null;
		if(args.length>3) {
			domain = args[3];
		}
		MBeanServerConnection server = null;
		try {
			server = getServerForDomain(domain);
			//return JMXHelper.getAttribute(server, compoundDelimiter, on, attrName);
			for(ObjectName objectName: server.queryNames(on, null)) {
				
			}
		} catch (Exception e) {
			log.debug("Failed to get MBeanServerConnection for domain [{}]", domain, e);
			log.error("Failed to get MBeanServerConnection for domain [{}]", domain);
			return COMMAND_NOT_SUPPORTED;			
		}		
		
		
		return null;
	}
	
	/**
	 * Extracts the discovery tokens into a map
	 * @param on The ObjectName to extract from
	 * @return a map of tokens
	 */
	protected Map<String, String> extractTokens(ObjectName on) {
		Map<String, String> map = new HashMap<String, String>();
		Matcher m = TOKEN.matcher(on.toString());
		while(m.find()) {
			map.put(m.group(1), m.group(2));
		}
		return map;
	}

}
