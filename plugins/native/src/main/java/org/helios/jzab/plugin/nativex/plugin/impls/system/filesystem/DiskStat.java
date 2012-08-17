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

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;

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
	
	public String getDevName() {
		return fs.getDevName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDirName()
	 */
	
	public String getDirName() {
		return fs.getDirName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getFlags()
	 */
	
	public long getFlags() {
		return fs.getFlags();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getOptions()
	 */
	
	public String getOptions() {
		return fs.getOptions();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getSysTypeName()
	 */
	
	public String getSysTypeName() {
		return fs.getSysTypeName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getTypeName()
	 */
	
	public String getTypeName() {
		return fs.getTypeName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getAvail()
	 */
	
	public long getAvail() {
		return fsu.getAvail();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskQueue()
	 */
	
	public double getDiskQueue() {
		return fsu.getDiskQueue();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskReadBytes()
	 */
	
	public long getDiskReadBytes() {
		return fsu.getDiskReadBytes();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskReads()
	 */
	
	public long getDiskReads() {
		return fsu.getDiskReads();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskServiceTime()
	 */
	
	public double getDiskServiceTime() {
		return fsu.getDiskServiceTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskWriteBytes()
	 */
	
	public long getDiskWriteBytes() {
		return fsu.getDiskWriteBytes();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getDiskWrites()
	 */
	
	public long getDiskWrites() {
		return fsu.getDiskWrites();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getFiles()
	 */
	
	public long getFiles() {
		return fsu.getFiles();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getFree()
	 */
	
	public long getFree() {
		return fsu.getFree();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getFreeFiles()
	 */
	
	public long getFreeFiles() {
		return fsu.getFreeFiles();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getTotal()
	 */
	
	public long getTotal() {
		return fsu.getTotal();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getUsePercent()
	 */
	
	public double getUsePercent() {
		return fsu.getUsePercent();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.DiskStatMBean#getUsed()
	 */
	
	public long getUsed() {
		return fsu.getUsed();
	}
	
	
}