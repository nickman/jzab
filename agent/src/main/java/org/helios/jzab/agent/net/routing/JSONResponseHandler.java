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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: JSONResponseHandler</p>
 * <p>Description: Defines a class that can be registered as a listener for JSON responses returned from a zabbix server.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.routing.JSONResponseHandler</code></p>
 */
public interface JSONResponseHandler {
	/** The JSON Response Key for the request type */
	public static final String KEY_REQUEST = "request";
	/** The JSON Response Key for the requesting host */
	public static final String KEY_HOST = "host";
	/** The JSON Response Value for an active check request */
	public static final String VALUE_ACTIVE_CHECK_REQUEST = "active checks";
	/** The JSON Response Value for an active check submission */
	public static final String VALUE_ACTIVE_CHECK_SUBMISSION = "agent data";
	
	/** The JSON Response Key for the response data array */
	public static final String KEY_DATA = "data";
	
	/**
	 * Callback from the {@link ResponseRoutingHandler} when a response is routed to this instance
	 * @param routing The routing instance that located this handler
	 * @param response The JSONObject representing the zabbix server response
	 * @throws JSONException thrown on any exceptions parsing the JSON response
	 */
	public void jsonResponse(RoutingObjectName routing, JSONObject response) throws JSONException;
	
	/**
	 * Returns this response handler's {@link RoutingObjectName}s.
	 * @return this response handler's {@link RoutingObjectName}s.
	 */
	public RoutingObjectName[] getRoutingObjectNames();
}
