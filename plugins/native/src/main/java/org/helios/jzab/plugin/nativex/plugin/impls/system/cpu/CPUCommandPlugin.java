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

public class CPUCommandPlugin extends AbstractMultiCommandProcessor implements CPUCommandPluginMBean {
	
	/** The shared CpuPerc instance */
	protected CpuPerc cpuPerc = sigar.getCpuPerc();
	/** The shared per cpu CpuPerc instances */
	protected CpuPerc[] cpuInstancePercs = sigar.getCpuPercList();	
	/** The shared Cpu instance */
	protected final Cpu cpux = sigar.getCpu();
	/** The shared per cpu Cpu instances */
	protected final Cpu[] cpuxs = sigar.getCpuList();
	
	/** An array of cpu infos */
	protected final CpuInfo[] cpuInfos = sigar.getCpuInfoList();
	
	/** A map of process cpu trackers keyed by pid */
	protected final Map<Long, ProcCpu> procCpus = new ConcurrentHashMap<Long, ProcCpu>();
	
	/** The CPU utilization unit for cummulative CPU time */
	public static final String UNIT_TIME = "time";
	/** The CPU utilization unit for percentage utilization */
	public static final String UNIT_PERCENT = "percent";
	
	
	/**
	 * Schedules the resource refresh task
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor#init()
	 */
	@Override
	public void init() {
		if(!inited.get()) {
			scheduleRefresh();
			rollingMetrics.registerDoubleRollingMetric("CPU Load", 15, 4, objectName, "cpuLoad", "system.cpu.load", "TOTAL", "percent");
			for(int i = 0; i < sigar.cpuCount; i++) {
				rollingMetrics.registerDoubleRollingMetric("CPU Load #" + i, 15, 4, objectName, "cpuLoad", "system.cpu.load", "TOTAL", "percent", "" + i);
			}
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
			for(Cpu cpuxx: cpuxs) {
				cpuxx.gather(sigar.getSigar());
			}
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
	 * Returns CPU usage for all or a specified cpu. 
	 * More zabbix compatible with <a href="http://www.zabbix.com/documentation/1.8/manual/config/items">system.cpu.util[&lt;cpu&gt;,&lt;type&gt;,&lt;mode&gt;]</a>. 
	 * @param commandName The command name
	 * @param args:<ol>
	 * 	<li><b>cpu</b>: The CPU ID identified as an int from <code>0</code> to <code>[number of processors -1]</code>. Default is a composite of all CPUs combined. <code>-1</code> means all.</li>
	 * 	<li><b>type</b>: The usage type as defined in {@link CPUUtilizationType}, defaults to {@link CPUUtilizationType#USER}</li>
	 *  <li><b>mode</b>: The aggregation window name. e.g. <b><code>avg1</code></b>, <b><code>avg5</code></b> and <b><code>avg15</code></b>. Defaults to <b><code>avg1</code></b></li> 
	 * </ol>
	 * @return
	 */
	@CommandHandler({"system.cpu.util"})
	public String getCpuUtil(String commandName, String... args) {
		int cpuId = -1;
		CPUUtilizationType type = CPUUtilizationType.USER;
		String mode = "avg1";
		
		return null;
	}
	
	
	/**
	 * Returns CPU usage for all or a specified cpu
	 * @param commandName The command name
	 * @param args The arguments: <ol>
	 * 	<li>The usage type as defined in {@link CPUUtilizationType}, defaults to {@link CPUUtilizationType#TOTAL}</li>
	 *  <li>Specifies the utilization unit. One of <code>time</code> or <code>percent</code>. Default is <code>percent</code>.</li> 
	 *  <li>The CPU ID identified as an int from <code>0</code> to <code>[number of processors -1]</code>. Default is a composite of all CPUs combined. <code>-1</code> means all.</li>
	 * </ol>
	 * @return The specified CPU utilization
	 */
	@CommandHandler({"cpu.util"})
	public String getCpuUsage(String commandName, String... args) {
		CPUUtilizationType utilType = CPUUtilizationType.TOTAL;
		String unit = UNIT_PERCENT;
		int id = -1;
		if(args.length>0) {
			try { utilType = CPUUtilizationType.forName(args[0]); } catch (Exception e) { log.error("Invalid CPUUtilizationType [{}]", args[0]); return COMMAND_ERROR; } 
		}
		if(args.length>1) {
			unit = args[1].toLowerCase();
			if(!UNIT_TIME.equals(unit) && !UNIT_PERCENT.equals(unit)) {
				log.error("Invalid CPU Utilization Unit [{}]", unit); return COMMAND_ERROR;
			}
		}
		if(args.length>2) {
			try {
				id = Integer.parseInt(args[2]);
				if(id>cpuInstancePercs.length-1) {
					log.warn("Non existent CPU ID Specified [{}]", id); return COMMAND_NOT_SUPPORTED;
				}
			} catch (Exception e) {
				log.error("Invalid CPU ID Specified [{}]", args[2]); return COMMAND_ERROR;
			}
		}
		if(UNIT_TIME.equals(unit)) {
			if(id==-1) {
				return utilType.getTime(cpux);
			}
			return utilType.getTime(cpuxs[id]);
		}
		if(id==-1) {
			return utilType.getTime(cpuPerc);
		}
		return utilType.getTime(cpuInstancePercs[id]);		
	}
	
	/**
	 * Alias for {@link CPUCommandPlugin#getCpuUsage(String, String...) but returns a double in decimal percentage 
	 * @param commandName The command name
	 * @param args The arguments
	 * @return a processor load
	 */
	public double cpuLoad(String commandName, String... args) {
		return Double.parseDouble(getCpuUsage(commandName, args))/100;
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
	
//	/**
//	 * Returns cpu utilization type for a specific process
//	 * @param commandName The command name
//	 * @param args The optional arguments:<ol>
//	 *  <li>The pid of the process to get cpu utilization for. Defaults to the agent process.</li>
//	 * </ol>
//	 * @return the requested cpu utilization data
//	 */
//	@CommandHandler("cpu.proc")
//	protected String getProcessCpu(String commandName, String... args) {
//		long pid = HeliosSigar.getInstance().pid;
//		if(args.length>1) {
//			pid = Long.parseLong(args[1].trim());
//		}
//		ProcCpu pc = procCpus.get(pid);
//		if(pc==null) {
//			synchronized(procCpus) {
//				pc = procCpus.get(pid);
//				if(pc==null) {
//					pc = sigar.getProcCpu(pid);
//					procCpus.put(pid, pc);
//					try {
//						pc.gather(sigar.getSigar(), pid);
//					} catch (Exception e) {
//						log.debug("Failed to gather proc cpu for PID [{}]", pid, e );
//					}
//				}
//			}
//		}
//		
//		return formatPerc(pc.getPercent());
//		// **
//		// {User=1591, LastTime=1344271384848, Percent=0.0, StartTime=1344271313939, Total=1809, Sys=218}
//	}
		
	/**
	 * Returns formatted information about cpus
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
				return cpuInfoFormat(cpuInfos[id]);
			} catch (Exception e) {
				return COMMAND_ERROR;
			}
		}
		return cpuInfosFormat(cpuInfos);
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
	public static String cpuInfosFormat(CpuInfo... cpuInfos) {
		StringBuilder b = new StringBuilder();
		for(CpuInfo c: cpuInfos) {
			b.append(cpuInfoFormat(c));
		}
		return b.toString();
	}
	
	/**
	 * Generates formatted text for the passed CPU info
	 * @param cpuInfo The cpu info to generate json for
	 * @return the generated text
	 */
	public static String cpuInfoFormat(CpuInfo cpuInfo) {
		try {
			StringBuilder b = new StringBuilder("====CPU====");
			b.append("\n\tVendor:").append(cpuInfo.getVendor())
			.append("\n\tModel:").append(cpuInfo.getModel())
			.append("\n\tCache-size:").append(cpuInfo.getCacheSize())
			.append("\n\tCores-per-socket:").append(cpuInfo.getCoresPerSocket())
			.append("\n\tSpeed:").append(cpuInfo.getMhz())
			.append("\n\tTotal-cores:").append(cpuInfo.getTotalCores())
			.append("\n\tTotal-sockets:").append(cpuInfo.getTotalSockets());			
			return b.toString();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate for cpuinfo [" + cpuInfo + "]", e);
		}
	}
	
	

}
