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
import org.hyperic.sigar.Cpu;
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
	protected CpuPerc cpuPerc = sigar.getCpuPerc();
	/** The shared Cpu instance */
	protected final Cpu cpux = sigar.getCpu();

	
	/** The last time the cpu resoures were updated */
	protected long lastTime = System.currentTimeMillis();
	
	/** The default frequency of refresh in ms. The default is 5000 */
	public static final long DEFAULT_REFRESH_WINDOW = 5000;
	/** The configured frequency of the refresh */
	protected long refreshFrequency = DEFAULT_REFRESH_WINDOW;
	
	/**
	 * Retrieves the cached CPUPerc instance
	 * @return the cached CPUPerc instance
	 */
	public CpuPerc getCpuPerc() {
		long since = System.currentTimeMillis()-lastTime;
		if(since>refreshFrequency) {
			cpuPerc = sigar.getCpuPerc();
		}
		return cpuPerc;
	}
	
	/**
	 * Retrieves the cached cpu instance
	 * @return the cached cpu instance
	 */
	public Cpu getCpu() {
		long since = System.currentTimeMillis()-lastTime;
		if(since>refreshFrequency) {
			try {
				cpux.gather(sigar.getSigar());
			} catch (Exception e) {
				throw new RuntimeException("Failed to gather CPU", e);
			}
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
		return formatPerc(getCpuPerc().getIdle());
	}
	
	/**
	 * Returns the system cpu irq percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu irq percentage
	 */
	@CommandHandler("cpu.irq")
	protected String getCpuIrq(String commandName, String... args) {		
		return formatPerc(getCpuPerc().getIrq());
	}
	
	/**
	 * Returns the system cpu nice percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu nice percentage
	 */
	@CommandHandler("cpu.nice")
	protected String getCpuNice(String commandName, String... args) {
		return formatPerc(getCpuPerc().getNice());
	}
	
	/**
	 * Returns the system cpu sys percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu sys percentage
	 */
	@CommandHandler("cpu.sys")
	protected String getCpuSys(String commandName, String... args) {
		return formatPerc(getCpuPerc().getSys());
	}
	
	/**
	 * Returns the system cpu user percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu user percentage
	 */
	@CommandHandler("cpu.user")
	protected String getCpuUser(String commandName, String... args) {
		return formatPerc(getCpuPerc().getUser());
	}

	/**
	 * Returns the system cpu wait percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu wait percentage
	 */
	@CommandHandler("cpu.wait")
	protected String getCpuWait(String commandName, String... args) {		
		return formatPerc(getCpuPerc().getWait());
	}
	
	/**
	 * Returns the system cpu stolen percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu stolen percentage
	 */
	@CommandHandler("cpu.stolen")
	protected String getCpuStolen(String commandName, String... args) {
		return formatPerc(getCpuPerc().getStolen());
	}
	
	/**
	 * Returns the system cpu soft irq percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu soft irq percentage
	 */
	@CommandHandler("cpu.sirq")
	protected String getCpuSoftIrq(String commandName, String... args) {
		return formatPerc(getCpuPerc().getSoftIrq());
	}
	
	/**
	 * Returns the system cpu total percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu total percentage
	 */
	@CommandHandler("cpu.total")
	protected String getCpuTotal(String commandName, String... args) {
		return formatPerc(getCpuPerc().getCombined());
	}
	
	/**
	 * Returns the system cpu idle cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu idle cpu time
	 */
	@CommandHandler("cpu.time.idle")
	protected String getCpuTimeIdle(String commandName, String... args) {
		return format(getCpu().getIdle());
	}
	
	/**
	 * Returns the system cpu irq cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu irq cpu time
	 */
	@CommandHandler("cpu.time.irq")
	protected String getCpuTimeIrq(String commandName, String... args) {		
		return format(getCpu().getIrq());
	}
	
	/**
	 * Returns the system cpu nice cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu nice cpu time
	 */
	@CommandHandler("cpu.time.nice")
	protected String getCpuTimeNice(String commandName, String... args) {
		return format(getCpu().getNice());
	}
	
	/**
	 * Returns the system cpu sys cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu sys cpu time
	 */
	@CommandHandler("cpu.time.sys")
	protected String getCpuTimeSys(String commandName, String... args) {
		return format(getCpu().getSys());
	}
	
	/**
	 * Returns the system cpu user cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu user cpu time
	 */
	@CommandHandler("cpu.time.user")
	protected String getCpuTimeUser(String commandName, String... args) {
		return format(getCpu().getUser());
	}

	/**
	 * Returns the system cpu wait cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu wait cpu time
	 */
	@CommandHandler("cpu.time.wait")
	protected String getCpuTimeWait(String commandName, String... args) {		
		return format(getCpu().getWait());
	}
	
	/**
	 * Returns the system cpu stolen cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu stolen cpu time
	 */
	@CommandHandler("cpu.time.stolen")
	protected String getCpuTimeStolen(String commandName, String... args) {
		return format(getCpu().getStolen());
	}
	
	/**
	 * Returns the system cpu soft irq cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu soft irq cpu time
	 */
	@CommandHandler("cpu.time.sirq")
	protected String getCpuTimeSoftIrq(String commandName, String... args) {
		return format(getCpu().getSoftIrq());
	}
	
	/**
	 * Returns the system cpu total cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu total cpu time
	 */
	@CommandHandler("cpu.time.total")
	protected String getCpuTimeTotal(String commandName, String... args) {
		return format(getCpu().getTotal());
	}	
	
	/**
	 * Formats the percentage return value
	 * @param value The double value to format
	 * @return a formated percentage string
	 */
	private static String formatPerc(double value) {
		if(value==Double.NaN) return COMMAND_NOT_SUPPORTED;
		return "" + Math.round(value*100);
	}
	
	/**
	 * Formats the cpu time value
	 * @param value The long value to format
	 * @return the formated cpu time value
	 */
	private static String format(long value) {		
		return "" + value;
	}
		

	
	
	

}
