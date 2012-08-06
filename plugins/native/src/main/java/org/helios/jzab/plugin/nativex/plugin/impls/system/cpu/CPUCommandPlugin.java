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
package org.helios.jzab.plugin.nativex.plugin.impls.system.cpu;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.jzab.plugin.nativex.HeliosSigar;
import org.helios.jzab.plugin.nativex.plugin.CommandHandler;
import org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.ProcCpu;

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
	/** The shared per cpu CpuPerc instances */
	protected CpuPerc[] cpuInstancePercs = sigar.getCpuPercList();	
	/** The shared Cpu instance */
	protected final Cpu cpux = sigar.getCpu();	
	/** An array of cpu infos */
	protected final CpuInfo[] cpuInfos = sigar.getCpuInfoList();
	
	/** A map of process cpu trackers keyed by pid */
	protected final Map<Long, ProcCpu> procCpus = new ConcurrentHashMap<Long, ProcCpu>();
	
	/**
	 * Schedules the resource refresh task
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor#init()
	 */
	@Override
	public void init() {
		if(!inited.get()) {
			scheduleRefresh();
		}
		super.init();
	}

	/**
	 * The resource refresh task
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			cpuPerc = sigar.getCpuPerc();
			cpuInstancePercs = sigar.getCpuPercList();
			cpux.gather(sigar.getSigar());
			for(Map.Entry<Long, ProcCpu> pc: procCpus.entrySet()) {
				try { pc.getValue().gather(sigar.getSigar(), pc.getKey()); } catch (Exception e) {
					log.debug("Failed to gather proc cpu for PID [{}]", pc.getKey(), e );
				}
			}
		} catch (Exception e) {
			log.warn("Resource refresh exception:[{}]", e.toString());
		}
	}
	
	
	/**
	 * Retrieves the cached CPUPerc instance
	 * @return the cached CPUPerc instance
	 */
	public CpuPerc getCpuPerc() {
		return cpuPerc;
	}
	
	/**
	 * Retrieves the cached per cpu CPUPerc instances
	 * @return the cached per cpu CPUPerc instances
	 */
	public CpuPerc[] getCpuPercList() {
		return cpuInstancePercs;
	}	
	
	/**
	 * Retrieves the cached cpu instance
	 * @return the cached cpu instance
	 */
	public Cpu getCpu() {
		return cpux;
	}	
	
	/**
	 * Returns the system cpu idle percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu idle percentage
	 */
	@CommandHandler("cpu.idle")
	public String getCpuIdle(String commandName, String... args) {
		return formatPerc(getCpuPerc().getIdle());
	}
	
	/**
	 * Returns the system cpu irq percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu irq percentage
	 */
	@CommandHandler("cpu.irq")
	public String getCpuIrq(String commandName, String... args) {		
		return formatPerc(getCpuPerc().getIrq());
	}
	
	/**
	 * Returns the system cpu nice percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu nice percentage
	 */
	@CommandHandler("cpu.nice")
	public String getCpuNice(String commandName, String... args) {
		return formatPerc(getCpuPerc().getNice());
	}
	
	/**
	 * Returns the system cpu sys percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu sys percentage
	 */
	@CommandHandler("cpu.sys")
	public String getCpuSys(String commandName, String... args) {
		return formatPerc(getCpuPerc().getSys());
	}
	
	/**
	 * Returns the system cpu user percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu user percentage
	 */
	@CommandHandler("cpu.user")
	public String getCpuUser(String commandName, String... args) {
		return formatPerc(getCpuPerc().getUser());
	}

	/**
	 * Returns the system cpu wait percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu wait percentage
	 */
	@CommandHandler("cpu.wait")
	public String getCpuWait(String commandName, String... args) {		
		return formatPerc(getCpuPerc().getWait());
	}
	
	/**
	 * Returns the system cpu stolen percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu stolen percentage
	 */
	@CommandHandler("cpu.stolen")
	public String getCpuStolen(String commandName, String... args) {
		return formatPerc(getCpuPerc().getStolen());
	}
	
	/**
	 * Returns the system cpu soft irq percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu soft irq percentage
	 */
	@CommandHandler("cpu.sirq")
	public String getCpuSoftIrq(String commandName, String... args) {
		return formatPerc(getCpuPerc().getSoftIrq());
	}
	
	/**
	 * Returns the system cpu total percentage
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu total percentage
	 */
	@CommandHandler("cpu.total")
	public String getCpuTotal(String commandName, String... args) {
		return formatPerc(getCpuPerc().getCombined());
	}
	
	/**
	 * Returns the system cpu idle cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu idle cpu time
	 */
	@CommandHandler("cpu.time.idle")
	public String getCpuTimeIdle(String commandName, String... args) {
		return format(getCpu().getIdle());
	}
	
	/**
	 * Returns the system cpu irq cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu irq cpu time
	 */
	@CommandHandler("cpu.time.irq")
	public String getCpuTimeIrq(String commandName, String... args) {		
		return format(getCpu().getIrq());
	}
	
	/**
	 * Returns the system cpu nice cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu nice cpu time
	 */
	@CommandHandler("cpu.time.nice")
	public String getCpuTimeNice(String commandName, String... args) {
		return format(getCpu().getNice());
	}
	
	/**
	 * Returns the system cpu sys cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu sys cpu time
	 */
	@CommandHandler("cpu.time.sys")
	public String getCpuTimeSys(String commandName, String... args) {
		return format(getCpu().getSys());
	}
	
	/**
	 * Returns the system cpu user cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu user cpu time
	 */
	@CommandHandler("cpu.time.user")
	public String getCpuTimeUser(String commandName, String... args) {
		return format(getCpu().getUser());
	}

	/**
	 * Returns the system cpu wait cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu wait cpu time
	 */
	@CommandHandler("cpu.time.wait")
	public String getCpuTimeWait(String commandName, String... args) {		
		return format(getCpu().getWait());
	}
	
	/**
	 * Returns the system cpu stolen cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu stolen cpu time
	 */
	@CommandHandler("cpu.time.stolen")
	public String getCpuTimeStolen(String commandName, String... args) {
		return format(getCpu().getStolen());
	}
	
	/**
	 * Returns the system cpu soft irq cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu soft irq cpu time
	 */
	@CommandHandler("cpu.time.sirq")
	public String getCpuTimeSoftIrq(String commandName, String... args) {
		return format(getCpu().getSoftIrq());
	}
	
	/**
	 * Returns the system cpu total cpu time
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the system cpu total cpu time
	 */
	@CommandHandler("cpu.time.total")
	public String getCpuTimeTotal(String commandName, String... args) {
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
	
	/**
	 * Returns the number of cpus
	 * @param commandName The command name
	 * @param args The arguments
	 * @return the number of cpus
	 */
	@CommandHandler("cpu.count")
	public String getCpuCount(String commandName, String... args) {
		return "" + cpuInfos.length;
	}
	
	/**
	 * Returns cpu utilization type for a specific process
	 * @param commandName The command name
	 * @param args The optional arguments:<ol>
	 *  <li>The pid of the process to get cpu utilization for. Defaults to the agent process.</li>
	 * </ol>
	 * @return the requested cpu utilization data
	 */
	@CommandHandler("cpu.proc")
	protected String getProcessCpu(String commandName, String... args) {
		long pid = HeliosSigar.getInstance().pid;
		if(args.length>1) {
			pid = Long.parseLong(args[1].trim());
		}
		ProcCpu pc = procCpus.get(pid);
		if(pc==null) {
			synchronized(procCpus) {
				pc = procCpus.get(pid);
				if(pc==null) {
					pc = sigar.getProcCpu(pid);
					procCpus.put(pid, pc);
					try {
						pc.gather(sigar.getSigar(), pid);
					} catch (Exception e) {
						log.debug("Failed to gather proc cpu for PID [{}]", pid, e );
					}
				}
			}
		}
		
		return formatPerc(pc.getPercent());
		// **
		// {User=1591, LastTime=1344271384848, Percent=0.0, StartTime=1344271313939, Total=1809, Sys=218}
	}
		
	/**
	 * Returns json formatted information about cpus
	 * @param commandName The command name
	 * @param args Optional argument is the id of the cpu (from 0 to n). 
	 * Zero arguments will return one json document outlinin all cpus.
	 * @return the system cpu data
	 */
	@CommandHandler("cpu.info")
	public String getCpuInfo(String commandName, String... args) {
		if(args != null && args.length>0) {
			try {
				int id = getIntArg(args);
				if(id<0 || id>cpuInfos.length-1) {
					return COMMAND_NOT_SUPPORTED;
				}
				return cpuInfoToJSON(cpuInfos[id]);
			} catch (Exception e) {
				return COMMAND_ERROR;
			}
		}
		return cpuInfosToJSON(cpuInfos);
	}	
	
	/**
	 * Extracts the int from the first arg
	 * @param args The command args
	 * @return the first arg as an int
	 */
	private static int getIntArg(String... args) {
		return Integer.parseInt(args[0].trim());
	}
	
	
	/**
	 * Returns a json formatted description of all the passed cpu infos
	 * @param cpuInfos The cpu infos to generate json for
	 * @return a json string
	 */
	public static String cpuInfosToJSON(CpuInfo... cpuInfos) {
		StringBuilder b = new StringBuilder("{ \"cpus\":[");
		for(CpuInfo c: cpuInfos) {
			b.append(cpuInfoToJSON(c));
		}
		if(cpuInfos.length>0) {
			b.deleteCharAt(b.length()-1);
		}
		b.append("]}");
		return b.toString();
	}
	
	/**
	 * Generates JSON for the passed CPU info
	 * @param cpuInfo The cpu info to generate json for
	 * @return the generated JSON
	 */
	public static String cpuInfoToJSON(CpuInfo cpuInfo) {
		try {
			StringBuilder b = new StringBuilder("{ \"cpu\": {");
			b.append("\"vendor\":\"").append(cpuInfo.getVendor()).append("\",")
			.append("\"model\":\"").append(cpuInfo.getModel()).append("\",")
			.append("\"cache-size\":").append(cpuInfo.getCacheSize()).append(",")
			.append("\"cores-per-socket\":").append(cpuInfo.getCoresPerSocket()).append(",")
			.append("\"speed\":").append(cpuInfo.getMhz()).append(",")
			.append("\"total-cores\":").append(cpuInfo.getTotalCores()).append(",")
			.append("\"total-sockets\":").append(cpuInfo.getTotalSockets()).append("}}");			
			return b.toString();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate JSON for cpuinfo [" + cpuInfo + "]", e);
		}
	}
	
	

}
