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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.jzab.plugin.nativex.HeliosSigar;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.SigarException;

/**
 * <p>Title: DiskStatSummary</p>
 * <p>Description: A disk stat summary MXBean attribute</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatSummary</code></p>
 */

public class DiskStatSummary  implements DiskStatSummaryMXBean {
	/** A map of requested file system usage instances, keyed by file system name */
	protected final Map<String, FileSystemUsage> usages = new ConcurrentHashMap<String, FileSystemUsage>();
	/** A map of file systems keyed by the name */
	protected final Map<String, FileSystem> fileSystems = new ConcurrentHashMap<String, FileSystem>();
	/** Last time updated */
	protected AtomicLong lastRefresh = new AtomicLong(0L);
	
	/**
	 * Creates a new DiskStatSummary
	 */
	public DiskStatSummary() {
		for(FileSystem fs: HeliosSigar.getInstance().getFileSystemList()) {
			fileSystems.put(fs.getDirName(), fs);
			usages.put(fs.getDirName(), HeliosSigar.getInstance().getFileSystemUsage(fs.getDirName()));
		}
		lastRefresh.set(System.currentTimeMillis());
	}
	
	/**
	 * Returns a set of DiskStats
	 * @return a set of DiskStats
	 */
	public Set<DiskStat> getDiskStats() {
		long now = System.currentTimeMillis();
		if((now-lastRefresh.get())>5000) {
			lastRefresh.set(now);
			fileSystems.clear();
			for(FileSystem fs: HeliosSigar.getInstance().getFileSystemList()) {
				fileSystems.put(fs.getDirName(), fs);
			}
			Set<String> remove = new HashSet<String>();
			for(String name: fileSystems.keySet()) {
				FileSystemUsage fsu = usages.get(name);
				if(fsu==null) {
					fsu = HeliosSigar.getInstance().getFileSystemUsage(name);
					usages.put(name, fsu);
					try {
						fsu.gather(HeliosSigar.getInstance().getSigar(), name);
					} catch (SigarException e) {
					}
				}
			}
			for(Map.Entry<String, FileSystemUsage> fsu: usages.entrySet()) {
				String name = fsu.getKey();
				if(!fileSystems.containsKey(name)) {
					remove.add(name);
					continue;
				}
			}
			for(String name: remove) {
				usages.remove(name);
			}
		}
		Set<DiskStat> stats = new HashSet<DiskStat>();
		for(FileSystem fs: HeliosSigar.getInstance().getFileSystemList()) {
			stats.add(new DiskStat(fs, usages.get(fs.getDirName())));
		}				
		return stats;
	}
}
