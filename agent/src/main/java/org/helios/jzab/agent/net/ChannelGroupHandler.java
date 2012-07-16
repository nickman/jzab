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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;

import javax.management.Notification;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;

/**
 * <p>Title: ChannelGroupHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.ChannelGroupHandler</code></p>
 */
public class ChannelGroupHandler extends SimpleChannelUpstreamHandler implements ChannelGroup {
	

		/**
		 * Invoked when a child Channel was open.
		 * @param ctx The channel handler context
		 * @param e The child channel state event
		 */
		@Override
		public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
			Channel channel = e.getChannel();
			add(channel);
//			sendNotification(new Notification("child.channel.open", objectName, notificationSequence.incrementAndGet(), new StringBuilder("Channel Opened [")
//				.append(" ChannelID:").append(channel.getId())
//				.append(" Remote:").append(channel.getRemoteAddress())
//				.append("]")
//				.toString()
//			));
			//log.debug("Child channel created on [{}] from [{}]", channel.getLocalAddress(), channel.getRemoteAddress());
		}
		
		/**
		 * Invoked when a child Channel was closed.
		 * @param ctx The channel handler context
		 * @param e The child channel state event
		 */
		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
			Channel channel = e.getChannel();
//			sendNotification(new Notification("child.channel.closed", objectName, notificationSequence.incrementAndGet(), new StringBuilder("Channel Closed [")
//				.append(" ChannelID:").append(channel.getId())
//				.append(" Remote:").append(channel.getRemoteAddress())
//				.append("]")
//				.toString()
//			));
			//log.debug("Child channel closed on [{}] from [{}]", channel.getLocalAddress(), channel.getRemoteAddress());
		}
		
		
	
	

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#size()
	 */
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#iterator()
	 */
	@Override
	public Iterator<Channel> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#toArray()
	 */
	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#toArray(T[])
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#add(java.lang.Object)
	 */
	@Override
	public boolean add(Channel e) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends Channel> c) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#clear()
	 */
	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ChannelGroup o) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#find(java.lang.Integer)
	 */
	@Override
	public Channel find(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#setInterestOps(int)
	 */
	@Override
	public ChannelGroupFuture setInterestOps(int interestOps) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#setReadable(boolean)
	 */
	@Override
	public ChannelGroupFuture setReadable(boolean readable) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object)
	 */
	@Override
	public ChannelGroupFuture write(Object message) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object, java.net.SocketAddress)
	 */
	@Override
	public ChannelGroupFuture write(Object message, SocketAddress remoteAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#disconnect()
	 */
	@Override
	public ChannelGroupFuture disconnect() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#unbind()
	 */
	@Override
	public ChannelGroupFuture unbind() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#close()
	 */
	@Override
	public ChannelGroupFuture close() {
		// TODO Auto-generated method stub
		return null;
	}

}
