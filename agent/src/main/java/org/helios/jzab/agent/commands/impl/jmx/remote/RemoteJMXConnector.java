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
package org.helios.jzab.agent.commands.impl.jmx.remote;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServerErrorException;
import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: RemoteJMXConnector</p>
 * <p>Description: A wrapper for {@link JMXConnector}s that tracks the connected state and enables connectivity based caching </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.jmx.remote.RemoteJMXConnector</code></p>
 */

public class RemoteJMXConnector implements JMXConnector, NotificationListener, Callable<Boolean>, InvocationHandler {
	/** Static class logger */
	protected static final Logger log = LoggerFactory.getLogger(RemoteJMXConnector.class);
	
	/** The delegate connection */
	protected final JMXConnector delegate;
	/** The connected state of the delegate */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** The environment for this connection */
	protected final Map<String,Object> environment = new HashMap<String, Object>();
	/** The reconnect schedule handle */
	protected volatile ScheduledFuture<?> scheudleHandle = null;
	/** The cache map to remove disconnected connectors if reconnect is not enabled */
	protected final Map<String, RemoteJMXConnector> remoteServerCache;
	/** Concurrency control lock */
	protected final ReentrantLock accessLock = new ReentrantLock(true);
	/** The MBeanServerConnection proxy that enforces the lock acquisition and release */
	protected final MBeanServerConnection mbscProxy = (MBeanServerConnection)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{MBeanServerConnection.class}, this);
	/** The actual MBeanServerConnection  */
	protected MBeanServerConnection mbsc;
	
	
	/** The reconnect scheduler */
	protected static volatile ScheduledExecutorService reconnectScheduler = null;
	/** Indicates if reconnect is enabled. Default is false */
	protected static boolean reconnectEnabled = false;
	/** The reconnect period in seconds. Default is 60 */
	protected static long reconnectPeriod = 60;

	/**
	 * Acquires the lock on this connector
	 * @param timeout The timeout in ms.
	 * @return true if the lock was acquired, false if it was not or connector is not connected
	 */
	public boolean lock(long timeout) {		
		try {
			if(!connected.get()) return false;
			log.debug("Not connected when requesting Lock on [{}]", this);
			boolean locked =  accessLock.tryLock(timeout, TimeUnit.MILLISECONDS);
			log.debug("Lock acquired for [{}]:{}", this, locked);
			return locked;
		} catch (InterruptedException ioe) {
			log.debug("Thread interrupted while waiting for Lock on [{}]", this);
			return false;
		}
	}
	
	/**
	 * Releases the lock if it is locked by the calling thread
	 */
	public void unlock() {
		try { accessLock.unlock(); log.debug("Unlocked [{}]", this); } catch (Exception e) {}
	}

	/**
	 * Creates a new RemoteJMXConnector
	 * @param remoteServerCache The cache map to remove disconnected connectors if reconnect is not enabled 
	 * @param delegate The delegate connector
	 */
	public RemoteJMXConnector(Map<String, RemoteJMXConnector> remoteServerCache, JMXConnector delegate) {
		this(remoteServerCache, delegate, null);
	}
	
	/**
	 * Creates a new RemoteJMXConnector
	 * @param remoteServerCache The cache map to remove disconnected connectors if reconnect is not enabled 
	 * @param delegate The delegate connector
	 * @param environment the properties of the connection.
	 */
	public RemoteJMXConnector(Map<String, RemoteJMXConnector> remoteServerCache, JMXConnector delegate, Map<String,?> environment) {
		this.delegate = delegate;
		this.remoteServerCache = remoteServerCache;
		if(environment!=null) { env(environment); }
		this.delegate.addConnectionNotificationListener(this, null, null);
		
		try {
			this.delegate.getMBeanServerConnection().getDefaultDomain();
			setConnected(true);
			log.debug("Created Connected Remote JMXConnector [{}]", this.delegate );
		} catch (Exception e) {
			try {
				this.delegate.connect();
				setConnected(true);
				log.debug("Created Connected Remote JMXConnector [{}]", this.delegate );
			} catch (Exception ex) {
				setConnected(false);
				log.debug("Created Disconnected Remote JMXConnector [{}]", this.delegate );
			}
		}
	}
	

	/**
	 * Sets the connected state of the connector. If it is set to false, and reconnect is enabled, 
	 * schedules a delayed task to reconnect 
	 * @param connected true if the connector is now connected, false otherwise
	 */
	protected void setConnected(boolean connected) {
		this.connected.set(connected);
		log.trace("Connection State for Remote JMXConnector [{}]: {}", this.delegate, connected );
		if(scheudleHandle!=null) {
			try { scheudleHandle.cancel(true); } catch (Exception e) {}
			scheudleHandle = null;
		} 
		try {
			mbsc = delegate.getMBeanServerConnection();
		} catch (Exception e) {
			log.error("Failed to acquire MBeanServerConnection from [{}] on set connection true", delegate);
			log.debug("Failed to acquire MBeanServerConnection from [{}] on set connection true", delegate, e);
			setConnected(false);
		}
		
		if(!connected) {
			mbsc = null;
			if(reconnectEnabled) {
				scheudleHandle = getScheduler().schedule(this, reconnectPeriod, TimeUnit.SECONDS);
				log.debug("Scheduling Reconnect for Remote JMXConnector [{}], Period {} s.", this.delegate, reconnectPeriod);
			} else {
				remoteServerCache.remove(this);
				log.debug("Ejecting Remote JMXConnector [{}] from cache on disconnect", this.delegate);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(!accessLock.isHeldByCurrentThread()) throw new IllegalStateException("The MBeanServerConnection Lock for [" + this.delegate + "] is not held by the current thread", new Throwable());
		try {
			return method.invoke(mbsc, args);
		} finally {
			accessLock.unlock();
		}
	}
	
	
	/**
	 * Lazy accessor for the reconnect scheduler
	 * @return the scheduler
	 */
	protected ScheduledExecutorService getScheduler() {
		if(reconnectScheduler==null) {
			synchronized(getClass()) {
				if(reconnectScheduler==null) {
					reconnectScheduler = Executors.newScheduledThreadPool(2, new ThreadFactory(){
						protected final AtomicInteger serial = new AtomicInteger(0);
						public Thread newThread(Runnable r) {
							Thread t = new Thread(r, "JMXConnectorReconnectThread#" + serial.incrementAndGet());
							t.setDaemon(true);
							return t;
						}
					});
					log.debug("Created JMXConnectorReconnectThread Reconnect Scheduler");
				}
			}
		}
		return reconnectScheduler;
	}

	
	/**
	 * Returns true if this connector is connected, false if it is not.
	 * @return true if this connector is connected, false if it is not.
	 */
	public boolean isConnected() {
		return connected.get();
	}
	
	/**
	 * Returns the connected state of this connector
	 * {@inheritDoc}
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Boolean call() {
		if(connected.get()) {
			return true;
		}
		try {
			connect();
			return true;
		} catch (Exception e) {
			return connected.get();
		}
	}
	
	
	/**
	 * Appends environmental entries to this connector
	 * @param environment environmental entries to add
	 * @return this connector
	 */
	public RemoteJMXConnector env(Map<String,?> environment) {
		if(environment!=null) {
			for(Map.Entry<String,?> env : environment.entrySet()) {
				Object val = env.getValue();
				this.environment.put(env.getKey(), val);
			}
		}
		return this;
	}

    /**
     * <p>Establishes the connection to the connector server.  This
     * method is equivalent to {@link #connect(Map)
     * connect(null)}.</p>
     *
     * @exception IOException if the connection could not be made
     * because of a communication problem.
     *
     * @exception SecurityException if the connection could not be
     * made for security reasons.
     */
	@Override
	public void connect() throws IOException {
		if(environment.isEmpty()) {
			delegate.connect();
		} else {
			delegate.connect(environment);
		}
	}

    /**
     * <p>Establishes the connection to the connector server.</p>
     *
     * <p>If <code>connect</code> has already been called successfully
     * on this object, calling it again has no effect.  If, however,
     * {@link #close} was called after <code>connect</code>, the new
     * <code>connect</code> will throw an <code>IOException</code>.<p>
     *
     * <p>Otherwise, either <code>connect</code> has never been called
     * on this object, or it has been called but produced an
     * exception.  Then calling <code>connect</code> will attempt to
     * establish a connection to the connector server.</p>
     *
     * @param env the properties of the connection.  Properties in
     * this map override properties in the map specified when the
     * <code>JMXConnector</code> was created, if any.  This parameter
     * can be null, which is equivalent to an empty map.
     *
     * @exception IOException if the connection could not be made
     * because of a communication problem.
     *
     * @exception SecurityException if the connection could not be
     * made for security reasons.
     */
	@Override
	public void connect(Map<String, ?> env) throws IOException {
		delegate.connect(env);
	}

    /**
     * <p>Returns an <code>MBeanServerConnection</code> object
     * representing a remote MBean server.  For a given
     * <code>JMXConnector</code>, two successful calls to this method
     * will usually return the same <code>MBeanServerConnection</code>
     * object, though this is not required.</p>
     *
     * <p>For each method in the returned
     * <code>MBeanServerConnection</code>, calling the method causes
     * the corresponding method to be called in the remote MBean
     * server.  The value returned by the MBean server method is the
     * value returned to the client.  If the MBean server method
     * produces an <code>Exception</code>, the same
     * <code>Exception</code> is seen by the client.  If the MBean
     * server method, or the attempt to call it, produces an
     * <code>Error</code>, the <code>Error</code> is wrapped in a
     * {@link JMXServerErrorException}, which is seen by the
     * client.</p>
     *
     * <p>Calling this method is equivalent to calling
     * {@link #getMBeanServerConnection(Subject) getMBeanServerConnection(null)}
     * meaning that no delegation subject is specified and that all the
     * operations called on the <code>MBeanServerConnection</code> must
     * use the authenticated subject, if any.</p>
     *
     * @return an object that implements the
     * <code>MBeanServerConnection</code> interface by forwarding its
     * methods to the remote MBean server.
     *
     * @exception IOException if a valid
     * <code>MBeanServerConnection</code> cannot be created, for
     * instance because the connection to the remote MBean server has
     * not yet been established (with the {@link #connect(Map)
     * connect} method), or it has been closed, or it has broken.
     */
	@Override
	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		return mbscProxy;
	}

    /**
     * <p>Returns an <code>MBeanServerConnection</code> object representing
     * a remote MBean server on which operations are performed on behalf of
     * the supplied delegation subject. For a given <code>JMXConnector</code>
     * and <code>Subject</code>, two successful calls to this method will
     * usually return the same <code>MBeanServerConnection</code> object,
     * though this is not required.</p>
     *
     * <p>For each method in the returned
     * <code>MBeanServerConnection</code>, calling the method causes
     * the corresponding method to be called in the remote MBean
     * server on behalf of the given delegation subject instead of the
     * authenticated subject. The value returned by the MBean server
     * method is the value returned to the client. If the MBean server
     * method produces an <code>Exception</code>, the same
     * <code>Exception</code> is seen by the client. If the MBean
     * server method, or the attempt to call it, produces an
     * <code>Error</code>, the <code>Error</code> is wrapped in a
     * {@link JMXServerErrorException}, which is seen by the
     * client.</p>
     *
     * @param delegationSubject the <code>Subject</code> on behalf of
     * which requests will be performed.  Can be null, in which case
     * requests will be performed on behalf of the authenticated
     * Subject, if any.
     *
     * @return an object that implements the <code>MBeanServerConnection</code>
     * interface by forwarding its methods to the remote MBean server on behalf
     * of a given delegation subject.
     *
     * @exception IOException if a valid <code>MBeanServerConnection</code>
     * cannot be created, for instance because the connection to the remote
     * MBean server has not yet been established (with the {@link #connect(Map)
     * connect} method), or it has been closed, or it has broken.
     */
	@Override
	public MBeanServerConnection getMBeanServerConnection(
			Subject delegationSubject) throws IOException {
		return mbscProxy;
	}

    /**
     * <p>Closes the client connection to its server.  Any ongoing or new
     * request using the MBeanServerConnection returned by {@link
     * #getMBeanServerConnection()} will get an
     * <code>IOException</code>.</p>
     *
     * <p>If <code>close</code> has already been called successfully
     * on this object, calling it again has no effect.  If
     * <code>close</code> has never been called, or if it was called
     * but produced an exception, an attempt will be made to close the
     * connection.  This attempt can succeed, in which case
     * <code>close</code> will return normally, or it can generate an
     * exception.</p>
     *
     * <p>Closing a connection is a potentially slow operation.  For
     * example, if the server has crashed, the close operation might
     * have to wait for a network protocol timeout.  Callers that do
     * not want to block in a close operation should do it in a
     * separate thread.</p>
     *
     * @exception IOException if the connection cannot be closed
     * cleanly.  If this exception is thrown, it is not known whether
     * the server end of the connection has been cleanly closed.
     */
	@Override
	public void close() throws IOException {
		delegate.close();
	}

    /**
     * <p>Adds a listener to be informed of changes in connection
     * status.  The listener will receive notifications of type {@link
     * JMXConnectionNotification}.  An implementation can send other
     * types of notifications too.</p>
     *
     * <p>Any number of listeners can be added with this method.  The
     * same listener can be added more than once with the same or
     * different values for the filter and handback.  There is no
     * special treatment of a duplicate entry.  For example, if a
     * listener is registered twice with no filter, then its
     * <code>handleNotification</code> method will be called twice for
     * each notification.</p>
     *
     * @param listener a listener to receive connection status
     * notifications.
     * @param filter a filter to select which notifications are to be
     * delivered to the listener, or null if all notifications are to
     * be delivered.
     * @param handback an object to be given to the listener along
     * with each notification.  Can be null.
     *
     * @exception NullPointerException if <code>listener</code> is
     * null.
     *
     * @see #removeConnectionNotificationListener
     * @see javax.management.NotificationBroadcaster#addNotificationListener
     */
	@Override
	public void addConnectionNotificationListener(
			NotificationListener listener, NotificationFilter filter,
			Object handback) {
		delegate.addConnectionNotificationListener(listener, filter, handback);
	}

    /**
     * <p>Removes a listener from the list to be informed of changes
     * in status.  The listener must previously have been added.  If
     * there is more than one matching listener, all are removed.</p>
     *
     * @param listener a listener to receive connection status
     * notifications.
     *
     * @exception NullPointerException if <code>listener</code> is
     * null.
     *
     * @exception ListenerNotFoundException if the listener is not
     * registered with this <code>JMXConnector</code>.
     *
     * @see #removeConnectionNotificationListener(NotificationListener,
     * NotificationFilter, Object)
     * @see #addConnectionNotificationListener
     * @see javax.management.NotificationEmitter#removeNotificationListener
     */
	@Override
	public void removeConnectionNotificationListener(
			NotificationListener listener) throws ListenerNotFoundException {
		delegate.removeConnectionNotificationListener(listener);
	}

	   /**
     * <p>Removes a listener from the list to be informed of changes
     * in status.  The listener must previously have been added with
     * the same three parameters.  If there is more than one matching
     * listener, only one is removed.</p>
     *
     * @param l a listener to receive connection status notifications.
     * @param f a filter to select which notifications are to be
     * delivered to the listener.  Can be null.
     * @param handback an object to be given to the listener along
     * with each notification.  Can be null.
     *
     * @exception ListenerNotFoundException if the listener is not
     * registered with this <code>JMXConnector</code>, or is not
     * registered with the given filter and handback.
     *
     * @see #removeConnectionNotificationListener(NotificationListener)
     * @see #addConnectionNotificationListener
     * @see javax.management.NotificationEmitter#removeNotificationListener
     */	@Override
	public void removeConnectionNotificationListener(NotificationListener l,
			NotificationFilter f, Object handback)
			throws ListenerNotFoundException {
		delegate.removeConnectionNotificationListener(l, f, handback);
	}

     /**
      * <p>Gets this connection's ID from the connector server.  For a
      * given connector server, every connection will have a unique id
      * which does not change during the lifetime of the
      * connection.</p>
      *
      * @return the unique ID of this connection.  This is the same as
      * the ID that the connector server includes in its {@link
      * JMXConnectionNotification}s.  The {@link
      * javax.management.remote package description} describes the
      * conventions for connection IDs.
      *
      * @exception IOException if the connection ID cannot be obtained,
      * for instance because the connection is closed or broken.
      */
     @Override
	public String getConnectionId() throws IOException {
		return delegate.getConnectionId();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		if(notification instanceof JMXConnectionNotification) {
			JMXConnectionNotification jcn = (JMXConnectionNotification)notification;
			log.debug("RemoteJMXConnector [{}] Event [{}]", this.delegate, jcn.getType());
			if(JMXConnectionNotification.CLOSED.equalsIgnoreCase(jcn.getType()) || JMXConnectionNotification.FAILED.equalsIgnoreCase(jcn.getType())) {
				setConnected(false);				
			} else if(JMXConnectionNotification.OPENED.equalsIgnoreCase(jcn.getType())) {
				setConnected(true);
			}
		}		
	}

	
}
