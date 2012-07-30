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

public enum CommandThreadPolicy implements IExecutionPlan{
	/** One thread is allocated to execute checks for each configured zabbix server */
	SERVER(new ServerExecutionPlan()),
	/** One thread is allocated to execute checks for each configured active host. The default.  */
	HOST(new HostExecutionPlan()),
	/** One thread is allocated to execute checks for each configured active check */
	CHECK(new CheckExecutionPlan());
	
	/**
	 * Creates a new CommandThreadPolicy
	 * @param plan policy's execution plan 
	 */
	private CommandThreadPolicy(IExecutionPlan plan) {
		this.plan = plan;
	}
	
	/** This policy's execution plan  */
	private final IExecutionPlan plan;
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.net.active.collection.IExecutionPlan#createPlan(long, org.helios.jzab.agent.net.active.ActiveServer, org.helios.jzab.agent.net.active.collection.IActiveCollectionStream)
	 */
	@Override
	public Collection<? extends Callable<Void>> createPlan(long delay, ActiveServer activeServer, IActiveCollectionStream collectionStream) {
		return plan.createPlan(delay, activeServer, collectionStream);
	}
	
	
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
		public Collection<? extends Callable<Void>> createPlan(final long delay, final ActiveServer activeServer, final IActiveCollectionStream collectionStream) {
			return Collections.singleton(
					new Callable<Void>(){
						public Void call() throws Exception {
								for(ActiveHost host: activeServer.getHostsForDelay(delay)) {
									for(ActiveHostCheck check: host.getChecksForDelay(delay)) {
										check.execute(collectionStream);										
									}
								}
								return null;
						}
					}
			);
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
		public Collection<? extends Callable<Void>> createPlan(final long delay, final ActiveServer activeServer, final IActiveCollectionStream collectionStream) {
			Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>(); 
				for(final ActiveHost host: activeServer.getHostsForDelay(delay)) {
					tasks.add(new Callable<Void>(){
						public Void call() throws Exception {
							for(ActiveHostCheck check: host.getChecksForDelay(delay)) {
								check.execute(collectionStream);
							}							
							return null;
						}
					});
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
		public Collection<? extends Callable<Void>> createPlan(final long delay, final ActiveServer activeServer, final IActiveCollectionStream collectionStream) {
			Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>(); 
			for(final ActiveHost host: activeServer.getHostsForDelay(delay)) {
				for(final ActiveHostCheck check: host.getChecksForDelay(delay)) {
					tasks.add(new Callable<Void>(){
						public Void call() throws Exception {
							check.execute(collectionStream);
							return null;
						}
					});
				}
			}
			return tasks;
		}
	}

	
}
