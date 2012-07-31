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
package org.helios.jzab.agent.net;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.agent.net.codecs.ResponseRoutingHandler;
import org.helios.jzab.agent.net.codecs.ZabbixRequestEncoder;
import org.helios.jzab.agent.net.codecs.ZabbixResponseDecoder;
import org.helios.jzab.agent.net.passive.PassiveRequestInvoker;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: SharableHandlers</p>
 * <p>Description: </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.SharableHandlers</code></p>
 */
public class SharableHandlers {
    /** Instance logger */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    /** A map of sharable handlers keyed by handler name */
    protected final Map<String, ChannelHandler> sharedHandlers = new ConcurrentHashMap<String, ChannelHandler>();
    /** The singleton instance */
    protected static volatile SharableHandlers instance = null;
    /** The singleton instance ctor lock */
    protected static final Object lock = new Object();
   
   
    /**
     * Acquires the SharableHandlers singleton instance
     * @return the SharableHandlers singleton instance
     */
    public static SharableHandlers getInstance() {
        if(instance==null) {
            synchronized(lock) {
                if(instance==null) {
                    instance = new SharableHandlers();
                }
            }
        }
        return instance;
    }
   
    /**
     * Creates a new SharableHandlers
     */
    protected SharableHandlers() {
        addChannelHandler("stringDecoder", new StringDecoder());        
        addChannelHandler("responseEncoder", new ZabbixRequestEncoder((byte)1));
        addChannelHandler("stringEncoder", new StringEncoder());        
        addChannelHandler("responseDecoder", new ZabbixResponseDecoder());
        addChannelHandler("responseRoutingHandler", new ResponseRoutingHandler(ThreadPoolFactory.getInstance("TaskExecutor"), "host", "request"));
        addChannelHandler("channelCloser", new ChannelCloser());
    }
   
    /**
     * Adds a new shared handler
     * @param name The name assigned to this handler
     * @param handler The handler to share
     * @throws IllegalStateException if the named handler has already been registered
     */
    protected void addChannelHandler(String name, ChannelHandler handler) {
        if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed handler name was null", new Throwable());
        if(handler==null) throw new IllegalArgumentException("The passed handler named [" + name + "] was null", new Throwable());
        String key = name.trim().toUpperCase();
        synchronized(sharedHandlers) {
            if(sharedHandlers.containsKey(key)) throw new IllegalStateException("The handler named [" + name + "] has already been registered", new Throwable());
            sharedHandlers.put(key, handler);
        }
    }
   
    /**
     * Returns the named handler
     * @param name The name of the handler to retrieve
     * @return the named handler
     * @throws IllegalStateException if the named handler has not been registered
     */
    public ChannelHandler getHandler(String name) {
        if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed handler name was null", new Throwable());
        ChannelHandler handler = sharedHandlers.get(name.trim().toUpperCase());
        if(handler==null) throw new IllegalStateException("The handler named [" + name + "] has not been registered", new Throwable());
        return handler;
    }
    
    protected static class ChannelCloser extends SimpleChannelUpstreamHandler {
    	/** Instance logger */
    	protected final Logger log = LoggerFactory.getLogger(getClass());

    	/**
    	 * {@inheritDoc}
    	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
    	 */
    	@Override
    	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			e.getChannel().close().addListener(new ChannelFutureListener() {				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()) {
						log.debug("======>  CLOSED AFTER RESPONSE");
					} else {
						log.debug("======>  CLOSED AFTER RESPONSE FAILED");
					}					
				}
			});
    	}
    }
}



