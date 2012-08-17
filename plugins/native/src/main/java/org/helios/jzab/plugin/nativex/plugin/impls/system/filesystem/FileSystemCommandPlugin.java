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
package org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;

import org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction;
import org.helios.jzab.plugin.nativex.plugin.CommandHandler;
import org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor;
import org.helios.jzab.util.JMXHelper;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;

/**
 * <p>Title: FileSystemCommandPlugin</p>
 * <p>Description: Command plugin to collect and report file system metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.FileSystemCommandPlugin</code></p>
 */
public class FileSystemCommandPlugin extends AbstractMultiCommandProcessor implements FileSystemCommandPluginMBean {
	/** A map of requested file system usage instances, keyed by file system name */
	protected final Map<String, FileSystemUsage> usages = new ConcurrentHashMap<String, FileSystemUsage>();
	
	/** The known file system names */
	protected final Set<String> FS_NAMES = new CopyOnWriteArraySet<String>();
	
	/**
	 * Schedules the resource refresh task
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor#init()
	 */
	@Override
	public void init() {
		for(FileSystem fs: sigar.getFileSystemList()) {
			FS_NAMES.add(fs.getDevName().trim());
		}
		if(!inited.get()) {
			scheduleRefresh();
			try {
				JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), JMXHelper.objectName(new StringBuilder(objectName.toString()).append(",helper=DiskStatSummary")), new DiskStatSummary());
			} catch (Exception e) {
				log.warn("Failed to register DiskStats MBean");
			}
			super.init();			
		}
	}
	
	
	/**
	 * The resource refresh task
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		for(Map.Entry<String, FileSystemUsage> fs: usages.entrySet()) {
			try {
				fs.getValue().gather(sigar.getSigar(), fs.getKey());
				FS_NAMES.add(fs.getKey().trim());
				log.trace("Refreshed FileSystemUsage for [{}]", fs.getKey());
			} catch (Exception e) {
				log.warn("Resource refresh exception:[{}]", e.toString());
			}
		}
	}

	/**
	 * Gathers and reports disk activity
	 * @param commandName The command name
	 * @param args Optional positional parameters:<ol>
	 * 	<li><b>device</b>: The file system name. Default is <b><code>all</code></b></li>
	 * 	<li><b>type</b>: The metric type which is any of the enumerated metrics in {@link DiskUsage} plus these zabbix specific supported values:<ul>
	 * 			<li><b>ops</b>: (Default) The number of disk reads per second</li>
	 * 			<li><b>bps</b>: The number of bytes read per second</li>
	 * 		</ul></li>
	 * 	<li><b>mode</b>:The aggregation window name. e.g. <b><code>avg1</code></b>, <b><code>avg5</code></b> and <b><code>avg15</code></b>. Defaults to <b><code>avg1</code></b></li>
	 * </ol>
	 * @return The disk read stats JSON doc
	 */
	@CommandHandler({"vfs.dev.read", "vfs.dev.write"})
	public String getDiskStats(String commandName, String... args) {
		String device = "all";
		String type = "ops";
		String mode = "avg1";
		final String aggrName = "PS_RATE_ALL";
		int range = 1;
		
		if(args.length>0) device = args[0];
		if(args.length>1) type = args[1].toLowerCase();
		if(args.length>2) {
			mode = args[2];
			Matcher m = EXPRESSION.matcher(mode);
			if(m.matches()) {
				range = Integer.parseInt(m.group(2));
			} else {
				log.error("Failed to recognize mode [{}]", mode);
				return COMMAND_NOT_SUPPORTED;
			}
		}
		if(!"all".equals(device) && !FS_NAMES.contains(device)) {
			log.warn("Requested Device [{}] not recognized", device);
			return COMMAND_NOT_SUPPORTED;
		}
		if(!DiskUsage.isValidDiskUsage(type) && !"ops".equals(type) && !"bps".equals(type)) {
			log.warn("Requested Type [{}] not recognized", type);
			return COMMAND_NOT_SUPPORTED;
		}
		if("ops".equals(type) || "bps".equals(type)) {
			boolean reads = commandName.endsWith("read");
			if("ops".equals(type)) type = reads ? DiskUsage.DISKREADS.name() : DiskUsage.DISKWRITES.name();
			else if("bps".equals(type)) type = reads ? DiskUsage.DISKREADBYTES.name() : DiskUsage.DISKWRITEBYTES.name();
			
		}
		type = DiskUsage.forValue(type).getInternal();
		String rollingMetricName = new StringBuilder(commandName).append(".").append(device).append(".").append(type).append(".").toString();
		log.debug("Registering Rolling Metric [{}] with range of [{}]", rollingMetricName, range);
		if(!rollingMetrics.hasDoubleRollingMetric(rollingMetricName, range)) {
			rollingMetrics.registerDoubleRollingMetric(rollingMetricName, range, 12, objectName, 
					"diskReads", commandName, device, type);
			return "" + getFillInDiskStat(device, type);
		} 
		double val = rollingMetrics.getDoubleEvaluation(rollingMetricName, aggrName, range);
		if(val==0) val = getFillInDiskStat(device, type);
		return "" + val;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.FileSystemCommandPluginMBean#getFillInDiskStat(java.lang.String, java.lang.String)
	 */
	@Override
	public double getFillInDiskStat(String device, String metricName) {
		try {
			FileSystemUsage fsu = usages.get(device);
			if(fsu==null) {
				fsu = sigar.getFileSystemUsageOrNull(device);
				usages.put(device, fsu);
			}
			fsu.gather(sigar.getSigar(), device);
			double first = Double.parseDouble(fsu.toMap().get(metricName).toString());
			Thread.currentThread().join(1000);
			fsu.gather(sigar.getSigar(), device);
			double second = Double.parseDouble(fsu.toMap().get(metricName).toString());
			return AggregateFunction.PS_RATE_ALL.aggregate(new double[]{5, first, second});

		} catch (Exception e) {
			log.error("Failed to get fillin disk stat for [{}]:[{}]", device + ":" + metricName, e.toString());
			throw new RuntimeException("Failed to get fill in disk stat", e);
		}
	}
	
	/**
	 * The callback that the rolling metrics service calls to get disk reads to aggregate
	 * @param commandName The command name
	 * @param device The device name
	 * @param type The type of metric to read
	 * @return the read metric
	 */
	@Override
	public double diskStats(String commandName, String device, String type) {
		if(type==null || type.trim().isEmpty()) throw new IllegalArgumentException("Passed metric type was null or empty", new Throwable());
		if(device==null || device.trim().isEmpty()) throw new IllegalArgumentException("Passed device was null or empty", new Throwable());
		type = type.trim().toLowerCase();
		if(!"all".equals(device) && !FS_NAMES.contains(device)) {
			throw new IllegalArgumentException("Requested Device was invalid [" + device + "]", new Throwable());
		}
		if(!"all".equals(device)) {
			FileSystemUsage fsu = usages.get(device);
			if(fsu==null) {
				fsu = sigar.getFileSystemUsageOrNull(device);
				usages.put(device, fsu);
			}
			return Double.parseDouble(fsu.toMap().get(type).toString()); 
		}
		double t = 0;
		for(FileSystemUsage fsu: usages.values()) {
			t += Double.parseDouble(fsu.toMap().get(type).toString());
		}
		return t;
		// return "" + rollingMetrics.getDoubleEvaluation(rollingMetricName, aggrName, range);
	}
	
	/**
	 * Returns a set of the know file systems
	 * @return a set of the know file systems
	 */
	@Override
	public Set<String> getFileSystemNames() {
		return Collections.unmodifiableSet(FS_NAMES);
	}


}
