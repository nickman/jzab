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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

/**
 * <p>Title: RoutingObjectNameFactory</p>
 * <p>Description: Borrowing from {@link ObjectName} to implement a flexible routing key based on session token handlers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.routing.RoutingObjectNameFactory</code></p>
 */
public class RoutingObjectNameFactory {
	/** A map of routing object names keyed by the requesting concatenated strings */
	private final ConcurrentHashMap<String, RoutingObjectName> routingObjects = new ConcurrentHashMap<String, RoutingObjectName>();
	
	/** A cache of mapped properties keyed by all the key-values concatenated  */
	private final ConcurrentHashMap<String, Map<String, String>> routingProperties = new ConcurrentHashMap<String, Map<String, String>>();
	
	/** A cache of register routing objects */
	private final Set<RoutingObjectName> routes = new CopyOnWriteArraySet<RoutingObjectName>();
	
	/** The domain of the object routing object names */
	public static final String DOMAIN = "org.helios.jzab.agent.net.routing";
	
	/** The singleton instance */
	private static volatile RoutingObjectNameFactory instance;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	private RoutingObjectNameFactory() {
		
	}
	
	/**
	 * Registers a {@link JSONResponseHandler} with each of its advertised {@link RoutingObjectName}s.
	 * @param responseHandler the {@link JSONResponseHandler} to register
	 */
	public void registerJSONResponseHandler(JSONResponseHandler responseHandler) {
		if(responseHandler==null) throw new IllegalArgumentException("The passed response handler was null", new Throwable());
		for(RoutingObjectName ron: responseHandler.getRoutingObjectNames()) {
			ron.addResponseHandler(responseHandler);
		}
	}
	
	/**
	 * Unregisters a {@link JSONResponseHandler} from each of its advertised {@link RoutingObjectName}s.
	 * @param responseHandler the {@link JSONResponseHandler} to unregister
	 */
	public void unregisterJSONResponseHandler(JSONResponseHandler responseHandler) {
		if(responseHandler==null) return;
		for(RoutingObjectName ron: responseHandler.getRoutingObjectNames()) {
			ron.removeResponseHandler(responseHandler);
		}
	}
	
	
	/**
	 * Acquires the RoutingObjectNameFactory singleton instance
	 * @return the RoutingObjectNameFactory singleton instance
	 */
	public static RoutingObjectNameFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RoutingObjectNameFactory();
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Returns a collection of matching routing objects
	 * @param ron The routing object to match against
	 * @return a collection of matching routing objects
	 */
	public Collection<RoutingObjectName> lookup(RoutingObjectName ron) {
		if(ron==null || routes.isEmpty() || !routes.contains(ron)) return Collections.emptySet();
		Collection<RoutingObjectName> results = new ArrayList<RoutingObjectName>();
		for(RoutingObjectName r: routes) {
			if(ron.equals(r)) {
				results.add(r);
			}
		}
		return results;		
	}
	
	/**
	 * Returns a collection of matching routing objects
	 * @param props The properties used to build a routing object to match against
	 * @return a collection of matching routing objects
	 */
	public Collection<RoutingObjectName> lookup(Map<String, String> props) {
		if(props==null || props.isEmpty()) throw new IllegalArgumentException("The passed property map was null or empty", new Throwable());
		return lookup(getRoute(props));
	}
	
	
	/**
	 * Returns the routing object for the passed properties
	 * @param appendWildcard true to append a wildcard
	 * @param props A map of properties
	 * @return The routing object
	 */
	public RoutingObjectName getRoute(boolean appendWildcard, Map<String, String> props) {
		if(props==null) throw new IllegalArgumentException("The passed map of properties was null", new Throwable());
		RoutingObjectName on = routingObjectName(appendWildcard, props);
		final String key = on.getCanonicalKeyPropertyListString() + (appendWildcard ? ",*" : "");
		RoutingObjectName realOn = routingObjects.get(key); 
		if(realOn==null) {
			synchronized(routingObjects) {
				realOn = routingObjects.get(key); 
				if(realOn==null) {
					routingObjects.put(key, on);
					routes.add(on);
					realOn = on;
				}
			}
		}
		return realOn;
	}
	
	/**
	 * Returns the routing object for the passed properties and no wildcard
	 * @param props A map of properties
	 * @return The routing object
	 */
	public RoutingObjectName getRoute(Map<String, String> props) {
		return getRoute(false, props);
	}
	
	
	/**
	 * Returns the routing object for the passed properties
	 * @param appendWildcard true to append a wildcard
	 * @param props An array of properties
	 * @return The routing object
	 */
	public RoutingObjectName getRoute(boolean appendWildcard, String...props) {
		return getRoute(appendWildcard, mapProperties(props));
	}
	
	/**
	 * Returns the routing object for the passed properties and no wildcard
	 * @param props An array of properties
	 * @return The routing object
	 */
	public RoutingObjectName getRoute(String...props) {
		return getRoute(false, mapProperties(props));
	}
	
	
	/**
	 * Returns the mapped properties for the passed array of strings 
	 * @param args An array of strings to create a property map for
	 * @return the mapped properties
	 */
	public Map<String, String> mapProperties(String...args) {
		if(args==null || args.length < 2 || args.length%2!=0) throw new IllegalArgumentException("Invalid arguments for route. Args were null, < 2 or odd number [" + args==null ? "[]" : Arrays.toString(args) + "]", new Throwable());
		String key = Arrays.toString(args);
		Map<String, String> map = routingProperties.get(key);
		if(map==null) {
			synchronized(routingProperties) {
				map = routingProperties.get(key);
				if(map==null) {
					map = new Hashtable<String, String>(args.length/2);
					String k=null, v=null;
					for(int i = 0; i < args.length; i++) {
						k = args[i].trim();
						i++;
						v = args[i].trim();
						map.put(k, v);				
					}
					routingProperties.put(key, map);					
				}
			}
		}
		return map;
	}
	
	
	/**
	 * Creates a new RoutingObjectName
	 * @param props
	 * @return
	 */
	protected static RoutingObjectName routingObjectName(boolean appendWildcard, Map<String, String> props) {
		try {
			Hashtable<String, String> ht = new Hashtable<String, String>(props); 			
			RoutingObjectName ron = new RoutingObjectName(DOMAIN, ht);
			if(appendWildcard) {
				ron = new RoutingObjectName(ron.toString() + ",*");
			}
			return ron;
		} catch (Exception e) {
			throw new RuntimeException("Invalid Routing Object Name from properties [" +  props + "]", new Throwable());
		}
	}
	
	public static void main(String[] args) {
		log("Routing ObjectName test");
		Set<RoutingObjectName> set = new CopyOnWriteArraySet<RoutingObjectName>();
		RoutingObjectName ron1 = RoutingObjectNameFactory.getInstance().getRoute(true, "a", "b", "c", "d");
		set.add(ron1);
		log("Added RON [" + ron1 + "]. Set size:" + set.size());
		RoutingObjectName ron2 = RoutingObjectNameFactory.getInstance().getRoute(false, "a", "b", "c", "d");
		log("Does set contain ron2 [" + ron2 + "] ?:" + set.contains(ron2) );
		log("The matching collection size:" + RoutingObjectNameFactory.getInstance().lookup(ron2).size());
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
