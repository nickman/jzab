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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.jzab.plugin.nativex.HeliosSigar;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;

/**
 * <p>Title: DiskStatSummary</p>
 * <p>Description: A disk stat summary MXBean attribute</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatSummary</code></p>
 */

public class DiskStatSummary {
	/** A map of requested file system usage instances, keyed by file system name */
	protected final Map<String, FileSystemUsage> usages = new ConcurrentHashMap<String, FileSystemUsage>();
	/** Last time updated */
	protected long lastRefresh = 0;
	
	/**
	 * Creates a new DiskStatSummary
	 */
	public DiskStatSummary() {
		for(FileSystem fs: HeliosSigar.getInstance().getFileSystemList()) {
			usages.put(fs.getDevName(), HeliosSigar.getInstance().getFileSystemUsage(fs.getDevName()));
		}
		lastRefresh = System.currentTimeMillis();
	}
	
	public Set<DiskStat> getDiskStats() {
		long now = System.currentTimeMillis();
		if((now-lastRefresh)>5000) {
			
		}
	}
	
	public interface DiskStatMBean {

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystem#getDevName()
		 */
		public abstract String getDevName();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystem#getDirName()
		 */
		public abstract String getDirName();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystem#getFlags()
		 */
		public abstract long getFlags();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystem#getOptions()
		 */
		public abstract String getOptions();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystem#getSysTypeName()
		 */
		public abstract String getSysTypeName();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystem#getTypeName()
		 */
		public abstract String getTypeName();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getAvail()
		 */
		public abstract long getAvail();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getDiskQueue()
		 */
		public abstract double getDiskQueue();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getDiskReadBytes()
		 */
		public abstract long getDiskReadBytes();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getDiskReads()
		 */
		public abstract long getDiskReads();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getDiskServiceTime()
		 */
		public abstract double getDiskServiceTime();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getDiskWriteBytes()
		 */
		public abstract long getDiskWriteBytes();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getDiskWrites()
		 */
		public abstract long getDiskWrites();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getFiles()
		 */
		public abstract long getFiles();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getFree()
		 */
		public abstract long getFree();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getFreeFiles()
		 */
		public abstract long getFreeFiles();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getTotal()
		 */
		public abstract long getTotal();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getUsePercent()
		 */
		public abstract double getUsePercent();

		/**
		 * @return
		 * @see org.hyperic.sigar.FileSystemUsage#getUsed()
		 */
		public abstract long getUsed();

	}
	
	/**
	 * <p>Title: DiskStat</p>
	 * <p>Description: MXBean attribute to expose file system info and stats</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStat</code></p>
	 */
	public class DiskStat implements DiskStatMBean {
		/** File system static info */
		private final FileSystem fs;
		/** File system dynamic stats */		
		private final FileSystemUsage fsu;
		
		/**
		 * Creates a new DiskStat
		 * @param fs
		 * @param fsu
		 */
		public DiskStat(FileSystem fs, FileSystemUsage fsu) {
			super();
			this.fs = fs;
			this.fsu = fsu;
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDevName()
		 */
		@Override
		public String getDevName() {
			return fs.getDevName();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDirName()
		 */
		@Override
		public String getDirName() {
			return fs.getDirName();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getFlags()
		 */
		@Override
		public long getFlags() {
			return fs.getFlags();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getOptions()
		 */
		@Override
		public String getOptions() {
			return fs.getOptions();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getSysTypeName()
		 */
		@Override
		public String getSysTypeName() {
			return fs.getSysTypeName();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getTypeName()
		 */
		@Override
		public String getTypeName() {
			return fs.getTypeName();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getAvail()
		 */
		@Override
		public long getAvail() {
			return fsu.getAvail();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskQueue()
		 */
		@Override
		public double getDiskQueue() {
			return fsu.getDiskQueue();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskReadBytes()
		 */
		@Override
		public long getDiskReadBytes() {
			return fsu.getDiskReadBytes();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskReads()
		 */
		@Override
		public long getDiskReads() {
			return fsu.getDiskReads();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskServiceTime()
		 */
		@Override
		public double getDiskServiceTime() {
			return fsu.getDiskServiceTime();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskWriteBytes()
		 */
		@Override
		public long getDiskWriteBytes() {
			return fsu.getDiskWriteBytes();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskWrites()
		 */
		@Override
		public long getDiskWrites() {
			return fsu.getDiskWrites();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getFiles()
		 */
		@Override
		public long getFiles() {
			return fsu.getFiles();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getFree()
		 */
		@Override
		public long getFree() {
			return fsu.getFree();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getFreeFiles()
		 */
		@Override
		public long getFreeFiles() {
			return fsu.getFreeFiles();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getTotal()
		 */
		@Override
		public long getTotal() {
			return fsu.getTotal();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getUsePercent()
		 */
		@Override
		public double getUsePercent() {
			return fsu.getUsePercent();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getUsed()
		 */
		@Override
		public long getUsed() {
			return fsu.getUsed();
		}
		
		
	}
}
