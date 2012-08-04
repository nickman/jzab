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

/**
 * <p>Title: CPUTimeCommandPlugin</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.CPUTimeCommandPlugin</code></p>
 */

public class CPUTimeCommandPlugin extends AbstractMultiCommandProcessor {
	
	/** The shared Cpu instance */
	protected final Cpu cpux = sigar.getCpu();
	/** The last time the Cpu was updated */
	protected long lastTime = System.currentTimeMillis();
	
	/** The frequency of refresh */
	public static final long REFRESH_WINDOW = 1000;
	
	/**
	 * Retrieves the cached cpu instance
	 * @return the cached cpu instance
	 */
	public Cpu getCpu() {
		long since = System.currentTimeMillis()-lastTime;
		if(since>REFRESH_WINDOW) {
			try {
				cpux.gather(sigar.getSigar());
			} catch (Exception e) {
				throw new RuntimeException("Failed to gather CPU", e);
			}
		}
		return cpux;
	}
	
	/**
	 * Returns the system cpu idle cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu idle cpu time
	 */
	@CommandHandler("cpu.time.idle")
	protected String getCpuIdle(String commandName, String... args) {
		return format(getCpu().getIdle());
	}
	
	/**
	 * Returns the system cpu irq cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu irq cpu time
	 */
	@CommandHandler("cpu.time.irq")
	protected String getCpuIrq(String commandName, String... args) {		
		return format(getCpu().getIrq());
	}
	
	/**
	 * Returns the system cpu nice cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu nice cpu time
	 */
	@CommandHandler("cpu.time.nice")
	protected String getCpuNice(String commandName, String... args) {
		return format(getCpu().getNice());
	}
	
	/**
	 * Returns the system cpu sys cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu sys cpu time
	 */
	@CommandHandler("cpu.time.sys")
	protected String getCpuSys(String commandName, String... args) {
		return format(getCpu().getSys());
	}
	
	/**
	 * Returns the system cpu user cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu user cpu time
	 */
	@CommandHandler("cpu.time.user")
	protected String getCpuUser(String commandName, String... args) {
		return format(getCpu().getUser());
	}

	/**
	 * Returns the system cpu wait cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu wait cpu time
	 */
	@CommandHandler("cpu.time.wait")
	protected String getCpuWait(String commandName, String... args) {		
		return format(getCpu().getWait());
	}
	
	/**
	 * Returns the system cpu stolen cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu stolen cpu time
	 */
	@CommandHandler("cpu.time.stolen")
	protected String getCpuStolen(String commandName, String... args) {
		return format(getCpu().getStolen());
	}
	
	/**
	 * Returns the system cpu soft irq cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu soft irq cpu time
	 */
	@CommandHandler("cpu.time.sirq")
	protected String getCpuSoftIrq(String commandName, String... args) {
		return format(getCpu().getSoftIrq());
	}
	
	/**
	 * Returns the system cpu total cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu total cpu time
	 */
	@CommandHandler("cpu.time.total")
	protected String getCpuTotal(String commandName, String... args) {
		return format(getCpu().getTotal());
	}
	
	private static String format(long value) {		
		return "" + value;
	}
	

}
