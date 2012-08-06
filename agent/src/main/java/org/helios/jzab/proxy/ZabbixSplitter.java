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
package org.helios.jzab.proxy;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.helios.jzab.agent.internal.jmx.ThreadPoolFactory;
import org.helios.jzab.agent.net.active.ActiveClient;
import org.helios.jzab.agent.net.codecs.ZabbixConstants;
import org.helios.jzab.agent.net.codecs.ZabbixResponseDecoder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: ZabbixSplitter</p>
 * <p>Description: Splits the request and sends to the agent</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.codecs.ZabbixSplitter</code></p>
 */

public class ZabbixSplitter extends SimpleChannelHandler implements ChannelFutureListener {
	private final Executor executor;
	protected final Logger log = LoggerFactory.getLogger(getClass());
	public ZabbixSplitter() {
		executor = ThreadPoolFactory.getInstance("TaskExecutor");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		final Channel serverChannel = e.getChannel();
		final ChannelHandlerContext serverChannelCtx = ctx;
		if(e.getMessage() instanceof ChannelBuffer) {
			final ChannelBuffer COPY = ChannelBuffers.copiedBuffer((ChannelBuffer)e.getMessage());
			executor.execute(new Runnable(){ public void run() {
			Channel channel = ActiveClient.getInstance().newChannel("localhost", 10051);
			channel.getPipeline().remove("routingHandler1");
			channel.getPipeline().remove("routingHandler2");
			channel.getPipeline().remove("responseEncoder");
			channel.getPipeline().remove("responseDecoder");
			channel.getPipeline().remove("channelCloser");
			channel.getPipeline().addLast("receiver", new SimpleChannelUpstreamHandler(){
				/**
				 * {@inheritDoc}
				 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
				 */
				@Override
				public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
					log.info("Received Response from Local Agent [{}]", e.getMessage());
					e.getChannel().close();
					ChannelBuffer event = (ChannelBuffer)e.getMessage();
					
//					String s = event.slice(ZabbixConstants.BASELINE_SIZE, event.readableBytes()).toString(Charset.forName("UTF-8"));
//					log.info("Actual Message:[{}]", s);
					ChannelFuture cf = Channels.future(serverChannel);
					cf.addListener(new ChannelFutureListener() {
						public void operationComplete(ChannelFuture future) throws Exception {
							if(future.isSuccess()) {
								log.info("Forwarded Proxy Response To Server [{}]", e.getMessage());
							} else {
								log.error("Proxy Response Forward To Server Failed", future.getCause());
							}
						}
					});
					serverChannelCtx.sendDownstream(new DownstreamMessageEvent(serverChannel, cf, e.getMessage(), serverChannel.getRemoteAddress()));
					//super.messageReceived(ctx, e);
				}
			});
			channel.write(COPY);
			}});
			super.messageReceived(ctx, e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		// TODO Auto-generated method stub
		
	}
}

