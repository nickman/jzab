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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.jzab.agent.commands.AbstractCommandProcessor;
import org.helios.jzab.agent.commands.impl.jmx.remote.RemoteJMXConnector;
import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: BaseJMXCommandProcessor</p>
 * <p>Description: Base class for JMX command processors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.jmx.BaseJMXCommandProcessor</code></p>
 */
public abstract class BaseJMXCommandProcessor extends AbstractCommandProcessor {
	/** A cache of remote JMX connectors */
	protected static final Map<String, RemoteJMXConnector> remoteServerCache = new ConcurrentHashMap<String, RemoteJMXConnector>();
	/** A cache of local MBeanServers */
	protected static final Map<String, MBeanServer> localServers = new ConcurrentHashMap<String, MBeanServer>();
	
	/** The remote timeout in seconds */
	protected long remoteTimeout = 5;
	
	/** The property name for the {@link #compoundDelimiter}  */
	public static final String DELIMITER_KEY  = "compound-delimiter";
	/** The property name for the {@link #remoteTimeout}  */
	public static final String RTIMEOUT_KEY  = "remote-timeout";
	
	
	/** The mandatory prefix for strings representing a {@link JMXServiceURL} */
	public static final String JMX_SVC_URL_PREFIX = "service:jmx:";
	
	/** The delimiter between an MBean's attribute name and the subkeys of opentypes. Set by processor properties. */
	protected String compoundDelimiter = null;

	/**
	 * Initializes the {@link #compoundDelimiter}
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.AbstractCommandProcessor#init()
	 */
	@Override
	public void init() {
		compoundDelimiter = processorProperties.getProperty(DELIMITER_KEY, "/");
		super.init();
	}

	
	/**
	 * Returns an MBeanServerConnection for the passed domain
	 * @param domain The domain which might be a JMXServiceURL
	 * @return an MBeanServerConnection
	 */
	protected MBeanServerConnection getServerForDomain(String domain) {
		MBeanServerConnection mbc = null;
		if(domain!=null && !domain.trim().isEmpty()) {
			domain = domain.trim();
			if(domain.indexOf(JMX_SVC_URL_PREFIX)!=-1) {
				try {
					mbc = getConnection(domain);
				} catch (Exception e) {
					log.debug("Failed to make JMX connection to [{}]", domain, e);
					log.error("Failed to make JMX connection to [{}]", domain);
					throw new RuntimeException(e);
				}
			} else {
				mbc = localServers.get(domain);
				if(mbc == null) {
					mbc = JMXHelper.getLocalMBeanServer(domain, true);
					localServers.put(domain, (MBeanServer)mbc);
				}
			}
		} else {
			mbc = JMXHelper.getHeliosMBeanServer();
		}
		return mbc;
	}
	
	/**
	 * Attempts to retrieve an MBeanServerConnection from a JMXConnector and lock.
	 * @param serviceUrl The JMX Service URL for the connection
	 * @return a MBeanServerConnection for the specified JMX Service URL  
	 */
	protected MBeanServerConnection getConnection(String serviceUrl) {
		if(serviceUrl==null) throw new IllegalArgumentException("The passed JMXServiceURL was null", new Throwable());
		try {
			RemoteJMXConnector connector = remoteServerCache.get(serviceUrl);
			if(connector==null) {
				synchronized(remoteServerCache) {
					connector = remoteServerCache.get(serviceUrl);
					if(connector==null) {
						connector = new RemoteJMXConnector(remoteServerCache, JMXConnectorFactory.newJMXConnector(new JMXServiceURL(serviceUrl), null));
					}
				}
			}
			if(connector.lock(TimeUnit.MILLISECONDS.convert(remoteTimeout, TimeUnit.MILLISECONDS))) {
				return connector.getMBeanServerConnection();
			}
			throw new Exception("Failed to get lock for MBeanServerConnection on [" + serviceUrl + "]");
		} catch (Exception e) {
			throw new RuntimeException("Failed to acquire MBeanServerConnection for [" + serviceUrl + "]", e);
		}
	}
	
	
	/**
	 * Executes a remote operation against the MBeanServerConnection for the passed JMX Service URL
	 * @param serviceUrl the JMX Service URL
	 * @param objectName The JMX ObjectName
	 * @param operationName The operation name
	 * @param parameters The operation parameters
	 * @param signature The operation signature
	 * @return The return value from the operation invocation
	 */
	public Object remoteInvoke(String serviceUrl, ObjectName objectName, String operationName, Object[] parameters, String...signature) {
		MBeanServerConnection server = getConnection(serviceUrl);
		try {
			return server.invoke(objectName, operationName, parameters, signature);
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute operation [" + operationName + "] against remote MBeanServer [" + serviceUrl + "]", e);
		}
	}
	
	/**
	 * Executes a remote attribute retrieval against the MBeanServerConnection for the passed JMX Service URL
	 * @param serviceUrl the JMX Service URL
	 * @param objectName The JMX ObjectName
	 * @param attributeName The attribute name
	 * @return the value of the attribute
	 */
	public Object remoteGetAttribute(String serviceUrl, ObjectName objectName, String attributeName) {
		MBeanServerConnection server = getConnection(serviceUrl);
		try {
			return server.getAttribute(objectName, attributeName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get attribute [" + attributeName + "] against remote MBeanServer [" + serviceUrl + "]", e);
		}
	}
	
	/**
	 * Executes a remote batch attribute retrieval against the MBeanServerConnection for the passed JMX Service URL
	 * @param serviceUrl the JMX Service URL
	 * @param objectName The JMX ObjectName
	 * @param attributeNames The attribute names to retrieve
	 * @return the attribute list
	 */
	public AttributeList remoteGetAttributes(String serviceUrl, ObjectName objectName, String...attributeNames) {
		MBeanServerConnection server = getConnection(serviceUrl);
		try {
			return server.getAttributes(objectName, attributeNames);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get attributes " + Arrays.toString(attributeNames) + " against remote MBeanServer [" + serviceUrl + "]", e);
		}
	}
	
	

//	/**
//	 * Var parameters:<ol>
//	 *  <li><b>JMX Object Name</b>: (Mandatory) The target MBean's ObjectName. Can be a pattern in which case, arg# 3 will be used to determine an aggregate function, defaulting to sum.</li>
//	 *  <li><b>Attribute Name</b>: (Mandatory) The name of the target attribute</li>
//	 *  <li><b>Aggregate function name</b>: (Optional) The aggregation function name used to aggregate multiple values returned. Function names are defined in {@link AggregateFunction}</li>
//	 *  <li><b>Domain</b>: (Optional) Defines the MBeanServer domain in which the target MBeans are registered. Can also be interpreted as a {@link JMXServiceURL} in which case a remote connection will be used to retrieve the attribute values.</li>
//	 * </ol> 
//	 * {@inheritDoc}
//	 * @see org.helios.jzab.agent.commands.AbstractCommandProcessor#doExecute(java.lang.String, java.lang.String[])
//	 */
//	@Override
//	protected Object doExecute(String commandName, String... args) throws Exception {
//		if(args==null || args.length < 2) throw new IllegalArgumentException("Invalid argument count for command [" + commandName + "] with args [" + (args==null ? 0 : args.length) + "]", new Throwable());
//		ObjectName on = JMXHelper.objectName(args[0]);
//		String attrName = args[1];
//		String domain = null;
//		if(args.length>3) {
//			domain = args[3];
//		}
//		JMXConnector connector = null;
//		MBeanServerConnection server = null;
//		try {
//			if(domain!=null && !domain.trim().isEmpty()) {
//				domain = domain.trim();
//				if(domain.indexOf(JMX_SVC_URL_PREFIX)!=-1) {
//					try {
//						connector = JMXHelper.getJMXConnection(domain, true, null);
//						server = connector.getMBeanServerConnection();
//					} catch (Exception e) {
//						log.debug("Failed to make JMX connection to [{}]", domain, e);
//						log.error("Failed to make JMX connection to [{}]", domain);
//						return COMMAND_ERROR;
//					}
//				} else {
//					server = JMXHelper.getLocalMBeanServer(domain, true);
//				}
//			} else {
//				server = JMXHelper.getHeliosMBeanServer();
//			}
//			if(server==null) return COMMAND_NOT_SUPPORTED;
//			return JMXHelper.getAttribute(server, compoundDelimiter, on, attrName);
//		} finally {
//			if(connector!=null) try { connector.close(); } catch (Exception e) {}
//		}
//	}


}
