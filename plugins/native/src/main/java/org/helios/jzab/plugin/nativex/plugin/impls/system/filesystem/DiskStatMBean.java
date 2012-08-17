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

public interface DiskStatMBean {

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystem#getDevName()
	 */
	public String getDevName();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystem#getDirName()
	 */
	public String getDirName();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystem#getFlags()
	 */
	public long getFlags();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystem#getOptions()
	 */
	public String getOptions();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystem#getSysTypeName()
	 */
	public String getSysTypeName();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystem#getTypeName()
	 */
	public String getTypeName();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getAvail()
	 */
	public long getAvail();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getDiskQueue()
	 */
	public double getDiskQueue();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getDiskReadBytes()
	 */
	public long getDiskReadBytes();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getDiskReads()
	 */
	public long getDiskReads();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getDiskServiceTime()
	 */
	public double getDiskServiceTime();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getDiskWriteBytes()
	 */
	public long getDiskWriteBytes();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getDiskWrites()
	 */
	public long getDiskWrites();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getFiles()
	 */
	public long getFiles();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getFree()
	 */
	public long getFree();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getFreeFiles()
	 */
	public long getFreeFiles();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getTotal()
	 */
	public long getTotal();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getUsePercent()
	 */
	public double getUsePercent();

	/**
	 * @return
	 * @see org.hyperic.sigar.FileSystemUsage#getUsed()
	 */
	public long getUsed();

}