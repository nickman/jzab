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

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.ProcCpu;

/**
 * <p>Title: CPUUtilizationType</p>
 * <p>Description: Enumerates the per CPU utilization percentage types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.cpu.CPUUtilizationType</code></p>
 */

public enum CPUUtilizationType implements ICPUDataExtractor {
	/** The combined CPU utilization */
	TOTAL(new ICPUDataExtractor(){
		@Override
		public String getTime(Cpu cpu) {
			return format(cpu.getTotal());
		}
		@Override
		public String getTime(CpuPerc cpu) {
			return formatPerc(cpu.getCombined());
		}
		@Override
		public String getProcCpuTime(ProcCpu procCpu) {
			return format(procCpu.getTotal());
		}
		@Override
		public String getProcCpuPerc(ProcCpu procCpu) {
			return formatPerc(procCpu.getPercent());
		}
	}),
	/** The idle CPU utilization */
	IDLE(new ICPUDataExtractor(){
		@Override
		public String getTime(Cpu cpu) {
			return format(cpu.getIdle());
		}
		@Override
		public String getTime(CpuPerc cpu) {
			return formatPerc(cpu.getIdle());
		}
		@Override
		public String getProcCpuTime(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}
		@Override
		public String getProcCpuPerc(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}		
	}),
	/** The IRQ handling CPU utilization */
	IRQ(new ICPUDataExtractor(){
		@Override
		public String getTime(Cpu cpu) {
			return format(cpu.getIrq());
		}
		@Override
		public String getTime(CpuPerc cpu) {
			return formatPerc(cpu.getIrq());
		}
		@Override
		public String getProcCpuTime(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}
		@Override
		public String getProcCpuPerc(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}				
	}),
	/** The niced CPU utilization */
	NICE(new ICPUDataExtractor(){
		@Override
		public String getTime(Cpu cpu) {
			return format(cpu.getNice());
		}
		@Override
		public String getTime(CpuPerc cpu) {
			return formatPerc(cpu.getNice());
		}		
		@Override
		public String getProcCpuTime(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}
		@Override
		public String getProcCpuPerc(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}				
	}),
	/** The soft IRQ handling CPU utilization */
	SOFTIRQ(new ICPUDataExtractor(){
		@Override
		public String getTime(Cpu cpu) {
			return format(cpu.getSoftIrq());
		}
		@Override
		public String getTime(CpuPerc cpu) {
			return formatPerc(cpu.getSoftIrq());
		}		
		@Override
		public String getProcCpuTime(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}
		@Override
		public String getProcCpuPerc(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}				
	}),
	/** The stolen CPU utilization */
	STOLEN(new ICPUDataExtractor(){
		@Override
		public String getTime(Cpu cpu) {
			return format(cpu.getStolen());
		}
		@Override
		public String getTime(CpuPerc cpu) {
			return formatPerc(cpu.getStolen());
		}		
		@Override
		public String getProcCpuTime(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}
		@Override
		public String getProcCpuPerc(ProcCpu procCpu) {
			return COMMAND_NOT_SUPPORTED;
		}				
	}),
	/** The system, or kernel CPU utilization */
	SYSTEM(new ICPUDataExtractor(){
		@Override
		public String getTime(Cpu cpu) {
			return format(cpu.getSys());
		}
		@Override
		public String getTime(CpuPerc cpu) {
			return formatPerc(cpu.getSys());
		}		
		@Override
		public String getProcCpuTime(ProcCpu procCpu) {
			return format(procCpu.getSys());
		}
		@Override
		public String getProcCpuPerc(ProcCpu procCpu) {
			return formatPerc(procCpu.getSys(), procCpu.getTotal());
		}				
	}),
	/** The user CPU utilization */
	USER(new ICPUDataExtractor(){
		@Override
		public String getTime(Cpu cpu) {
			return format(cpu.getUser());
		}
		@Override
		public String getTime(CpuPerc cpu) {
			return formatPerc(cpu.getUser());
		}		
		@Override
		public String getProcCpuTime(ProcCpu procCpu) {
			return format(procCpu.getUser());
		}
		@Override
		public String getProcCpuPerc(ProcCpu procCpu) {
			return formatPerc(procCpu.getUser(), procCpu.getTotal());
		}				
	});
	
	private CPUUtilizationType(ICPUDataExtractor ext) {
		this.ext = ext;
	}
	
	private final ICPUDataExtractor ext;
	
	/** Return result for a command when the command is not supported or cannot be executed */
	public static final String COMMAND_NOT_SUPPORTED = "ZBX_NOTSUPPORTED";
	
	/**
	 * Returns the CPUUtilizationType for the passed name. Applies trim and toUpper to the name first.
	 * @param name The name of the type
	 * @return the named CPUUtilizationType 
	 */
	public static CPUUtilizationType forName(CharSequence name) {
		if(name==null) throw new IllegalArgumentException("The passed CPUUtilizationType name was null", new Throwable());
		try {
			return CPUUtilizationType.valueOf(name.toString().trim().toUpperCase());
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed CPUUtilizationType name [" + name + "] is not a valid type name", new Throwable());
		}
	}
		
	
	/**
	 * Formats the cpu time value
	 * @param value The long value to format
	 * @return the formated cpu time value
	 */
	public static String format(long value) {		
		return "" + value;
	}
	
	
	/**
	 * Formats the percentage return value
	 * @param value The double value to format
	 * @return a formated percentage string
	 */
	public static String formatPerc(double value) {
		if(value==Double.NaN) return COMMAND_NOT_SUPPORTED;
		return "" + Math.round(value*100);
	}
	
	/**
	 * Calculates and returns a formatted percentage
	 * @param part The part of the whole
	 * @param total The whole value
	 * @return the percentage
	 */
	public static String formatPerc(double part, double total) {
		if(part<=0 || total<=0) return "0";
		double perc = part/total;
		if(perc==Double.NaN) return COMMAND_NOT_SUPPORTED;
		return "" + Math.round(perc*100);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.cpu.ICPUDataExtractor#getTime(org.hyperic.sigar.Cpu)
	 */
	@Override
	public String getTime(Cpu cpu) {		
		return ext.getTime(cpu);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.cpu.ICPUDataExtractor#getTime(org.hyperic.sigar.CpuPerc)
	 */
	@Override
	public String getTime(CpuPerc cpu) {
		return ext.getTime(cpu);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.cpu.ICPUDataExtractor#getProcCpuTime(org.hyperic.sigar.ProcCpu)
	 */
	@Override
	public String getProcCpuTime(ProcCpu procCpu) {
		return ext.getProcCpuTime(procCpu);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.cpu.ICPUDataExtractor#getProcCpuPerc(org.hyperic.sigar.ProcCpu)
	 */
	@Override
	public String getProcCpuPerc(ProcCpu procCpu) {
		return ext.getProcCpuPerc(procCpu);
	}	
	
	
	
}
