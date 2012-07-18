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

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.helios.jzab.agent.commands.AbstractCommandProcessor;
import org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction;
import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: JMXAttributeCommandProcessor</p>
 * <p>Description: The core JMX command processor for standard attribute getters.</p> 
 * <p>Simple test examples (adjust host and port if necessary):<ul>
 * 	<li><code>echo "jmxattr[\"java.lang:type=Compilation\",TotalCompilationTime]" | nc localhost 10050</code></li>
 * 	<li><code>echo "jmxattr[\"java.lang:type=Memory\",HeapMemoryUsage.used]" | nc localhost 10050</code></li>
 *  <li><code>echo "jmxattr[\"java.lang:type=Memory\",HeapMemoryUsage.used,,service:jmx:iiop://localhost:7002/jndi/rmi://localhost:8005/jmxiiop]" | nc localhost 10050</code></li>
 *  <li><code>echo "jmxattr[\"java.lang:type=Memory\",HeapMemoryUsage.used,,service:jmx:rmi://localhost:8002/jndi/rmi://localhost:8005/jmxrmi]" | nc localhost 10050</code></li>
 * </ul>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.jmx.JMXAttributeCommandProcessor</code></p>
 * TODO: <ol>
 * <li>Open and closing JMXConnectors each time is fairly inefficient. Should we pool connections ?</li>
 * </ol>
 */

public class JMXAttributeCommandProcessor extends AbstractCommandProcessor {
	/** This processors command keys */
	public static final String COMMAND_KEY  = "jmxattr"; 
	
	/** The property name for the {@link #compoundDelimiter}  */
	public static final String DELIMITER_KEY  = "compound-delimiter";
	
	/** The mandatory prefix for strings representing a {@link JMXServiceURL} */
	public static final String JMX_SVC_URL_PREFIX = "service:jmx:";
	
	/** The delimiter between an MBean's attribute name and the subkeys of opentypes. Set by processor properties. */
	protected String compoundDelimiter = null;
	
	/**
	 * Initializes the {@link #compoundDelimiter}
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.AbstractCommandProcessor#init()
	 */
	public void init() {
		compoundDelimiter = processorProperties.getProperty(DELIMITER_KEY, "/");
		super.init();
	}
	
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
	 *  <li><b>JMX Object Name</b>: (Mandatory) The target MBean's ObjectName. Can be a pattern in which case, arg# 3 will be used to determine an aggregate function, defaulting to sum.</li>
	 *  <li><b>Attribute Name</b>: (Mandatory) The name of the target attribute</li>
	 *  <li><b>Aggregate function name</b>: (Optional) The aggregation function name used to aggregate multiple values returned. Function names are defined in {@link AggregateFunction}</li>
	 *  <li><b>Domain</b>: (Optional) Defines the MBeanServer domain in which the target MBeans are registered. Can also be interpreted as a {@link JMXServiceURL} in which case a remote connection will be used to retrieve the attribute values.</li>
	 * </ol>
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.AbstractCommandProcessor#doExecute(java.lang.String[])
	 */
	@Override
	protected Object doExecute(String... args) throws Exception {
		if(args==null || args.length < 2) throw new IllegalArgumentException("Invalid argument count for command [" + (args==null ? 0 : args.length) + "]", new Throwable());
		ObjectName on = JMXHelper.objectName(args[0]);
		String attrName = args[1];
		String aggrFuncName = null;
		String domain = null;
		if(args.length>2) {
			aggrFuncName = args[2];
		}
		if(args.length>3) {
			domain = args[3];
		}
		JMXConnector connector = null;
		MBeanServerConnection server = null;
		try {
			if(domain!=null && !domain.trim().isEmpty()) {
				domain = domain.trim();
				if(domain.indexOf(JMX_SVC_URL_PREFIX)!=-1) {
					try {
						connector = JMXHelper.getJMXConnection(domain, true, null);
						server = connector.getMBeanServerConnection();
					} catch (Exception e) {
						log.debug("Failed to make JMX connection to [{}]", domain, e);
						log.error("Failed to make JMX connection to [{}]", domain);
						return COMMAND_ERROR;
					}
				} else {
					server = JMXHelper.getLocalMBeanServer(domain, true);
				}
			} else {
				server = JMXHelper.getHeliosMBeanServer();
			}
			if(server==null) return COMMAND_NOT_SUPPORTED;
			return JMXHelper.getAttribute(server, compoundDelimiter, on, attrName);
		} finally {
			if(connector!=null) try { connector.close(); } catch (Exception e) {}
		}
	}

}
