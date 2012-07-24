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
package org.helios.jzab.agent.net.routing;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * <p>Title: RoutingObjectName</p>
 * <p>Description: Extension of {@link ObjectName} to implement equality when two instances have a pattern match</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.routing.RoutingObjectName</code></p>
 */
public class RoutingObjectName extends ObjectName implements Iterable<JSONResponseHandler> {

	/**  */
	private static final long serialVersionUID = 3608273011749051048L;
	
	/** A set of JSONResponseHandlers that are interested in receiving responses that have routing object names that match this name */
	private final transient Set<JSONResponseHandler> responseHandlers = new CopyOnWriteArraySet<JSONResponseHandler>();

	/**
	 * Creates a new RoutingObjectName
	 * @param name
	 * @throws MalformedObjectNameException
	 * @throws NullPointerException
	 */
	public RoutingObjectName(String name) throws MalformedObjectNameException, NullPointerException {
		super(name);
	}

	/**
	 * Creates a new RoutingObjectName
	 * @param domain
	 * @param table
	 * @throws MalformedObjectNameException
	 * @throws NullPointerException
	 */
	public RoutingObjectName(String domain, Hashtable<String, String> table)
			throws MalformedObjectNameException, NullPointerException {
		super(domain, table);
	}

	/**
	 * Creates a new RoutingObjectName
	 * @param domain
	 * @param key
	 * @param value
	 * @throws MalformedObjectNameException
	 * @throws NullPointerException
	 */
	public RoutingObjectName(String domain, String key, String value)
			throws MalformedObjectNameException, NullPointerException {
		super(domain, key, value);
	}
	
	/**
	 * Tests the passed object for equality with this object. If the passed object 
	 * is not null, is assignable to an {@link ObjectName} and has an {@link ObjectName}
	 * pattern match to this instance, then they are considered equal. However, the 
	 * pattern based match is only applicable if this instance is a pattern.  
	 * {@inheritDoc}
	 * @see javax.management.ObjectName#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if(object==null) return false;
		if(!ObjectName.class.isAssignableFrom(object.getClass())) {
			return false;
		}
		ObjectName on = (ObjectName)object;
		if(!on.isPattern() && isPattern()) {
			return super.equals(object);
		}
		return on.apply(this);
	}
	
	/**
	 * Registers a new response handler with this routing object
	 * @param responseHandler the response handler to register
	 */
	public void addResponseHandler(JSONResponseHandler responseHandler) {
		if(responseHandler==null) throw new IllegalArgumentException("The passed response handler was null", new Throwable());
		responseHandlers.add(responseHandler);
	}
	
	/**
	 * Removes a response handler from this routing object
	 * @param responseHandler the response handler to remove
	 */
	public void removeResponseHandler(JSONResponseHandler responseHandler) {
		if(responseHandler==null) return;
		responseHandlers.remove(responseHandler);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<JSONResponseHandler> iterator() {		
		return Collections.unmodifiableSet(responseHandlers).iterator();
	}
	
	/**
	 * Removes all response handlers
	 */
	public void clear() {
		responseHandlers.clear();
	}
	
	/**
	 * Returns the number of registered response handlers
	 * @return the number of registered response handlers
	 */
	public int size() {
		return responseHandlers.size();
	}
	
	

}
