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
package org.helios.jzab.agent.net.active.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import org.helios.jzab.agent.net.active.ActiveHost;
import org.helios.jzab.agent.net.active.ActiveHost.ActiveHostCheck;
import org.helios.jzab.agent.net.active.ActiveServer;

/**
 * <p>Title: CommandThreadPolicy</p>
 * <p>Description: Enumerates the policy that specifies how chunks of command executions are apportioned across executing threads</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.net.active.CommandThreadPolicy</code></p>
 */

public enum CommandThreadPolicy {
	/** One thread is allocated to execute all checks */
	ALL,
	/** One thread is allocated to execute checks for each configured zabbix server */
	SERVER,
	/** One thread is allocated to execute checks for each configured active host. The default.  */
	HOST,
	/** One thread is allocated to execute checks for each configured active check */
	CHECK;
	
	/**
	 * Decodes the passed string into a CommandThreadPolicy, applying trim and uppercase to the passed value
	 * @param name The name to decode
	 * @return the decoded CommandThreadPolicy
	 */
	public static CommandThreadPolicy forName(CharSequence name) {
		if(name==null || name.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		try {
			return CommandThreadPolicy.valueOf(name.toString().trim().toUpperCase());
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed name [" + name + "] was not a valid CommandThreadPolicy", new Throwable());
		}
	}
	
	/**
	 * <p>Title: IExecutionPlan</p>
	 * <p>Description: Defines an implementation of a CommandThreadPolicy execution plan which manages how check executions are multithreaded</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.IExecutionPlan</code></p>
	 */
	public static interface IExecutionPlan {
		/**
		 * Returns a collection of check execution task callables. Each member of the array will be allocated to a seperate thread.
		 * @param delay The scheduling delay to plan executions for
		 * @param activeServers A set of active servers that manage hosts that have active checks that are scheduled to be executed for the passed delay
		 * @param collectionStream The collection stream that results are written to 
		 * @return an collection of check execution task callables
		 */
		public Collection<? extends Callable<Void>> createPlan(long delay, Set<ActiveServer> activeServers, IActiveCollectionStream collectionStream); 
	}
	
	/**
	 * <p>Title: ServerExecutionPlan</p>
	 * <p>Description: An execution planner that executes all checks for each server in a seperate thread</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.ServerExecutionPlan</code></p>
	 */
	public static class ServerExecutionPlan implements IExecutionPlan {

		/**
		 * Creates a plan that executes all checks for each server in a seperate thread
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.IExecutionPlan#createPlan(long, java.util.Set, org.helios.jzab.agent.net.active.collection.IActiveCollectionStream)
		 */
		@Override
		public Collection<? extends Callable<Void>> createPlan(final long delay, Set<ActiveServer> activeServers, final IActiveCollectionStream collectionStream) {
			Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>(activeServers.size()); 
			for(final ActiveServer server: activeServers) {
				tasks.add(new Callable<Void>() {
					public Void call() throws Exception {
						for(ActiveHost host: server.getHostsForDelay(delay)) {
							for(ActiveHostCheck check: host.getChecksForDelay(delay)) {
								check.execute(collectionStream);
							}
						}
						return null;
					}
				});
			}
			return tasks;
		}
	}
	
	/**
	 * <p>Title: OneExecutionPlan</p>
	 * <p>Description: An execution planner that executes all checks in a single thread</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.OneExecutionPlan.CheckExecutionPlan</code></p>
	 */
	public static class OneExecutionPlan implements IExecutionPlan {

		/**
		 * Creates a plan that executes all checks in one thread
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.IExecutionPlan#createPlan(long, java.util.Set, org.helios.jzab.agent.net.active.collection.IActiveCollectionStream)
		 */
		@Override
		public Collection<? extends Callable<Void>> createPlan(final long delay, final Set<ActiveServer> activeServers, final IActiveCollectionStream collectionStream) {
			Callable<Void> callable = new Callable<Void>(){
				public Void call() throws Exception {
					for(ActiveServer server: activeServers) {
						for(ActiveHost host: server.getHostsForDelay(delay)) {
							for(ActiveHostCheck check: host.getChecksForDelay(delay)) {
								check.execute(collectionStream);
								return null;
							}
						}
					}
					return null;
				}
			};
			return Collections.singleton(callable);
		}
	}
	
	
	/**
	 * <p>Title: HostExecutionPlan</p>
	 * <p>Description: An execution planner that executes all checks for each host  in a seperate thread</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.HostExecutionPlan</code></p>
	 */
	public static class HostExecutionPlan implements IExecutionPlan {

		/**
		 * Creates a plan that executes all checks for each host in a seperate thread
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.IExecutionPlan#createPlan(long, java.util.Set, org.helios.jzab.agent.net.active.collection.IActiveCollectionStream)
		 */
		@Override
		public Collection<? extends Callable<Void>> createPlan(final long delay, Set<ActiveServer> activeServers, final IActiveCollectionStream collectionStream) {
			Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>(activeServers.size()); 
			for(ActiveServer server: activeServers) {
				for(final ActiveHost host: server.getHostsForDelay(delay)) {
					tasks.add(new Callable<Void>(){
						public Void call() throws Exception {
							for(ActiveHostCheck check: host.getChecksForDelay(delay)) {
								check.execute(collectionStream);
							}							
							return null;
						}
					});
				}
			}
			return tasks;
		}
	}
	
	/**
	 * <p>Title: CheckExecutionPlan</p>
	 * <p>Description: An execution planner that executes all checks in a seperate thread</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.CheckExecutionPlan</code></p>
	 */
	public static class CheckExecutionPlan implements IExecutionPlan {

		/**
		 * Creates a plan that executes all checks in a seperate thread
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.net.active.collection.CommandThreadPolicy.IExecutionPlan#createPlan(long, java.util.Set, org.helios.jzab.agent.net.active.collection.IActiveCollectionStream)
		 */
		@Override
		public Collection<? extends Callable<Void>> createPlan(final long delay, Set<ActiveServer> activeServers, final IActiveCollectionStream collectionStream) {
			Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>(activeServers.size()); 
			for(ActiveServer server: activeServers) {
				for(final ActiveHost host: server.getHostsForDelay(delay)) {
					for(final ActiveHostCheck check: host.getChecksForDelay(delay)) {
						tasks.add(new Callable<Void>(){
							public Void call() throws Exception {
								check.execute(collectionStream);
								return null;
							}
						});
					}
				}
			}
			return tasks;
		}
	}
	
}
