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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction;
import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: JMXOperationCommandProcessor</p>
 * <p>Description:The core JMX command processor for standard operation invocations. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.jmx.JMXOperationCommandProcessor</code></p>
 */

public class JMXOperationCommandProcessor extends BaseJMXCommandProcessor {
	/** This processors command keys */
	public static final String COMMAND_KEY  = "jmxop"; 
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.ICommandProcessor#getLocatorKey()
	 */
	@Override
	public String getLocatorKey() {
		return COMMAND_KEY;
	}

	/**
	 * Since operation arguments are in string form, a medium effort is made to match the correct operation, depending mostly on parameter counts.
	 *  If the target has multiple overloaded operations with the same name and number of parameters, this will most likely not work. Additionally, registered
	 *  property editors will be used to attempt correct typing. So yes, this needs some work.
	 * Var parameters:<ol>
	 *  <li><b>JMX Object Name</b>: (Mandatory) The target MBean's ObjectName. </li>
	 *  <li><b>Operation Name</b>: (Mandatory) The name of the target operation</li>
	 *  <li><b>Aggregate Function</b>: (Mandatory if a domain or arguments are specified, but can be blank) The name of an aggregate function in {@link AggregateFunction}</li>
	 *  <li><b>Domain</b>: (Mandatory if arguments are specified, but can be blank) Defines the MBeanServer domain in which the target MBeans are registered. Can also be interpreted as a {@link JMXServiceURL} in which case a remote connection will be used to retrieve the attribute values.</li>
	 *  <li><b>Arguments</b>: (Optional) The arguments define the arguments that will be passed to the operation invocation. </li>
	 * </ol>
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.AbstractCommandProcessor#doExecute(java.lang.String[])
	 */
	@Override
	protected Object doExecute(String... args) throws Exception {
		if(args==null || args.length < 2) throw new IllegalArgumentException("Invalid argument count for command [" + (args==null ? 0 : args.length) + "]", new Throwable());
		ObjectName on = JMXHelper.objectName(args[0].trim());
		String opName = args[1].trim();
		int argStartingIndex = 2;
		String aggregate = null;
		String domain = null;
		if(args.length>2) {
			aggregate = args[2].trim();
			argStartingIndex++;
		}
		if(args.length>3) {
			domain = args[3].trim();
			argStartingIndex++;
		}
		
		int argCount = args.length-argStartingIndex;
		String[] opArgs = new String[argCount];
		if(argCount > 0) {
			for(int i = argStartingIndex; i < args.length; i++) {
				opArgs[i-argStartingIndex] = args[i].trim();
			}
		}				
		try {
			String[] signature = null;
			Object[] arguments = null;
			if(argCount>0) {
				signature = getSignature(on, domain, opName, argCount);
				arguments = getParameters(signature, opArgs);
			} else {
				signature = new String[0];
				arguments = new Object[0];
			}
			Object result =  getServerForDomain(domain).invoke(on, opName, arguments, signature);			
			if(!aggregate.trim().isEmpty()) {
				AggregateFunction aggrFunc = AggregateFunction.getAggregateFunction(aggregate);
				if(aggrFunc==null) {
					log.error("Invalid aggregate name [{}]", aggrFunc);
					return COMMAND_ERROR;
				} 
				return AggregateFunction.aggregate(aggrFunc.name(), result);
			}
			if(result==null) return "";
			return result;
		} catch (Exception e) {
			log.debug("Failed to get MBeanServerConnection for domain [{}]", domain, e);
			log.error("Failed to get MBeanServerConnection for domain [{}]", domain);
			return COMMAND_NOT_SUPPORTED;			
		}		
	}
	
	/**
	 * Sniffs out the signature of the inteded op.
	 * @param on The object name
	 * @param domain The server pointer
	 * @param opName The operation name
	 * @param argCount The argument count
	 * @return A string array of types or null if no match was found.
	 * @throws Exception thrown if MBeanInfo retrieval fails
	 */
	protected String[] getSignature(ObjectName on, String domain, String opName, int argCount) throws Exception {		
		try {
			MBeanServerConnection conn = domain==null ? JMXHelper.getHeliosMBeanServer() : getServerForDomain(domain);
			MBeanInfo info = conn.getMBeanInfo(on);
			for(MBeanOperationInfo opInfo: info.getOperations()) {
				if(opInfo.getName().equals(opName) && opInfo.getSignature().length==argCount) {
					String[] sig = new String[argCount];
					MBeanParameterInfo[] pInfo = opInfo.getSignature();
					for(int i = 0; i <argCount; i++) {
						sig[i] = pInfo[i].getType();
					}
					return sig;
				}
			}
			return null;
		} catch (Exception e) {
			log.error("Failed to get MBeanInfo from server [{}] for op name [{}]", domain, opName);
			throw e;
		}
	}
	
	/**
	 * Creates an array of objects from the passed types and string values
	 * @param types The types
	 * @param values The string values
	 * @return An array of objects 
	 * @throws Exception thrown on any error
	 */
	protected Object[] getParameters(String[] types, String[] values) throws Exception {
		Object[] objects = new Object[types.length];
		for(int i = 0; i < types.length; i++) {
			if(values[i]==null || values[i].isEmpty()) {
				objects[i] = null;
				continue;
			}
			PropertyEditor pe = PropertyEditorManager.findEditor(Class.forName(types[i]));
			pe.setAsText(values[i]);
			objects[i] = pe.getValue();
		}
		return objects;
	}

}
