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
package org.helios.jzab.agent.net.passive;

import org.helios.jzab.agent.commands.CommandManager;
import org.helios.jzab.agent.commands.ICommandProcessor;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: PassiveRequestInvoker</p>
 * <p>Description: Decoder for passive agent command requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.passive.PassiveRequestInvoker</code></p>
 */
@Sharable	
public class PassiveRequestInvoker extends SimpleChannelUpstreamHandler {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) 	throws Exception {
		Object msg = e.getMessage();
		if(msg instanceof CharSequence) {
			log.debug("Processing Passive Request [{}]", msg);
			String value = CommandManager.getInstance().processCommand(msg.toString());
			if(!ICommandProcessor.COMMAND_NOT_SUPPORTED.equals(value)) {
				log.info("Passive Request Result for [{}] was [{}]", msg, value);
			}
			
			Channel channel = e.getChannel();
			ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), value, channel.getRemoteAddress()));
		}
		super.messageReceived(ctx, e);
	}
	
	
}
