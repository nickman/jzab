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
package org.helios.jzab.plugin.nativex.plugin.impls.system;

import org.helios.jzab.plugin.nativex.plugin.CommandHandler;
import org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor;
import org.hyperic.sigar.CpuPerc;

/**
 * <p>Title: CPUCommandPlugin</p>
 * <p>Description: Plugin command processor to collect cpu stats</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.CPUCommandPlugin</code></p>
 */

public class CPUCommandPlugin extends AbstractMultiCommandProcessor {
	
	/** The shared CpuPerc instance */
	protected CpuPerc cpux = sigar.getCpuPerc();
	/** The last time the CPUPerc was updated */
	protected long lastTime = System.currentTimeMillis();
	
	/** The frequency of refresh */
	public static final long REFRESH_WINDOW = 5000;
	
	/**
	 * Retrieves the cached CPUPerc instance
	 * @return the cached CPUPerc instance
	 */
	public CpuPerc getCpuPerc() {
		long since = System.currentTimeMillis()-lastTime;
		if(since>REFRESH_WINDOW) {
			cpux = sigar.getCpuPerc();
		}
		return cpux;
	}
	
	/**
	 * Returns the system cpu idle percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu idle percentage
	 */
	@CommandHandler("cpu.idle")
	protected String getCpuIdle(String commandName, String... args) {
		return format(getCpuPerc().getIdle());
	}
	
	/**
	 * Returns the system cpu irq percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu irq percentage
	 */
	@CommandHandler("cpu.irq")
	protected String getCpuIrq(String commandName, String... args) {		
		return format(getCpuPerc().getIrq());
	}
	
	/**
	 * Returns the system cpu nice percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu nice percentage
	 */
	@CommandHandler("cpu.nice")
	protected String getCpuNice(String commandName, String... args) {
		return format(getCpuPerc().getNice());
	}
	
	/**
	 * Returns the system cpu sys percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu sys percentage
	 */
	@CommandHandler("cpu.sys")
	protected String getCpuSys(String commandName, String... args) {
		return format(getCpuPerc().getSys());
	}
	
	/**
	 * Returns the system cpu user percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu user percentage
	 */
	@CommandHandler("cpu.user")
	protected String getCpuUser(String commandName, String... args) {
		return format(getCpuPerc().getUser());
	}

	/**
	 * Returns the system cpu wait percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu wait percentage
	 */
	@CommandHandler("cpu.wait")
	protected String getCpuWait(String commandName, String... args) {		
		return format(getCpuPerc().getWait());
	}
	
	/**
	 * Returns the system cpu stolen percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu stolen percentage
	 */
	@CommandHandler("cpu.stolen")
	protected String getCpuStolen(String commandName, String... args) {
		return format(getCpuPerc().getStolen());
	}
	
	/**
	 * Returns the system cpu soft irq percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu soft irq percentage
	 */
	@CommandHandler("cpu.sirq")
	protected String getCpuSoftIrq(String commandName, String... args) {
		return format(getCpuPerc().getSoftIrq());
	}
	
	/**
	 * Returns the system cpu total percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu total percentage
	 */
	@CommandHandler("cpu.total")
	protected String getCpuTotal(String commandName, String... args) {
		return format(getCpuPerc().getCombined());
	}
	
	private static String format(double value) {
		if(value==Double.NaN) return COMMAND_NOT_SUPPORTED;
		return "" + Math.round(value*100);
	}
	

	
	
	

}
