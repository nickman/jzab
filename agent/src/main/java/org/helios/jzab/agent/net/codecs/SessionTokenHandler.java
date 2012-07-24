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
package org.helios.jzab.agent.net.codecs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: SessionTokenHandler</p>
 * <p>Description: Handler to detect a session key in the scope of an outgoing JSON request and if found, injects it back into the response JSON payload</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.codecs.SessionTokenHandler</code></p>
 */

public class SessionTokenHandler extends SimpleChannelHandler {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());	
	/** A map of key/value pairs keyed by channel ID */
	protected final Map<Integer, Map<String, String>> sessionKeys = new ConcurrentHashMap<Integer, Map<String, String>>();
	/** A set of keys that qualify for session tokens */
	protected final Set<String> keysToLookFor = new HashSet<String>();
	
	/**
	 * Creates a new SessionTokenHandler
	 * @param keys An aray of case sensitive keys
	 */
	public SessionTokenHandler(String...keys) {
		if(keys!=null) {
			for(String key: keys) {
				if(key!=null) {
					keysToLookFor.add(key.trim());
				}
			}
		}
		log.debug("Created SessionTokenHandler with keys {}", keysToLookFor);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#writeRequested(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if(msg instanceof JSONObject) {
			JSONObject request = (JSONObject)msg;
			boolean keysFound = false;
			Channel channel = e.getChannel();
			for(String key: keysToLookFor) {
				if(request.has(key)) {
					addPair(channel.getId(), key, request.getString(key));
					log.debug("Capturing key [{}] for channel [{}]", key, channel);
					keysFound = true;
				}
			}
			if(keysFound) {
				channel.getCloseFuture().addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future) throws Exception {
						if(future.isDone()) {
							sessionKeys.remove(future.getChannel().getId());
						}
					}
				});
			}
		}
		super.writeRequested(ctx, e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();		
		if(msg instanceof JSONObject) {
			Map<String, String> map = sessionKeys.get(e.getChannel().getId());
			if(map!=null) {
				JSONObject response = (JSONObject)msg;
				for(Map.Entry<String, String> entry: map.entrySet()) {
					if(!response.has(entry.getKey())) {
						response.put(entry.getKey(), entry.getValue());
						log.debug("Injected key [{}] for channel [{}]", entry.getKey() + "/" + entry.getValue(), e.getChannel().getId());
					}
				}
			}
			sessionKeys.remove(e.getChannel().getId());
		}
		super.messageReceived(ctx, e);
	}
	
	/**
	 * Adds a name value pair to the sessionKeys state map
	 * @param channelId The ID of the channel
	 * @param name The pair key
	 * @param value The pair value
	 */
	protected void addPair(int channelId, String name, String value) {
		Map<String, String> map = sessionKeys.get(channelId);
		if(map == null) {
			map = new HashMap<String, String>();
			sessionKeys.put(channelId, map);			
		}
		map.put(name, value);
	}
}
