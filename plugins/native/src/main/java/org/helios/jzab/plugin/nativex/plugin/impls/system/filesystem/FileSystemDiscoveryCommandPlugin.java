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

import org.helios.jzab.plugin.nativex.plugin.CommandHandler;
import org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor;
import org.hyperic.sigar.FileSystem;
import org.json.JSONObject;

/**
 * <p>Title: FileSystemDiscoveryCommandPlugin</p>
 * <p>Description: Native file system discovery command processor</p>
 * <p>Accepts positional parameters specifying the macro name for each position. The positions are:<ol>
 * 	<li>The file system name. e.g. <b><code>/dev/sda2</code></b></li>
 * 	<li>The file system type. e.g. <b><code>ext4</code></b></li>
 * 	<li>The file system root directory or mount point name. e.g. <b><code>/sys</code></b></li>
 * 	<li>The file system device type name. e.g. <b><code>local</code></b>. Not very predictable, but sometimes provides some useful data.</li>
 * </ol></p>
 * <p>Sample outputs using the command <b><code>fsd[{#FSNAME},{#FSTYPE},{#FSDNAME},{#FSDTYPE}]</code></b>:<ul>
 * <li><b>Ubuntu/Linux</b>
 * <pre>
{"data":[
	{"{#FSNAME}":"/dev/sda2", "{#FSTYPE}":"ext4", "{#FSDNAME}":"/", "{#FSDTYPE}":"local"},
	{"{#FSNAME}":"proc", "{#FSTYPE}":"proc", "{#FSDNAME}":"/proc", "{#FSDTYPE}":"none"},
	{"{#FSNAME}":"sysfs", "{#FSTYPE}":"sysfs", "{#FSDNAME}":"/sys", "{#FSDTYPE}":"none"},
	{"{#FSNAME}":"binfmt_misc", "{#FSTYPE}":"binfmt_misc", "{#FSDNAME}":"/proc/sys/fs/binfmt_misc", "{#FSDTYPE}":"none"},
	{"{#FSNAME}":"gvfs-fuse-daemon", "{#FSTYPE}":"fuse.gvfs-fuse-daemon", "{#FSDNAME}":"/home/nwhitehead/.gvfs", "{#FSDTYPE}":"none"}
	{"{#FSNAME}":"/dev/sdc1", "{#FSTYPE}":"fuseblk", "{#FSDNAME}":"/media/HELIOS", "{#FSDTYPE}":"none"}
	{"{#FSNAME}":"/dev/sdb1", "{#FSTYPE}":"vfat", "{#FSDNAME}":"/media/PKBACK# 001", "{#FSDTYPE}":"local"}
]}  
 * </pre></li>
 * <li><b>Windows 7</b>
 * <pre>
{"data":[
	{"{#FSNAME}":"C:\","{#FSTYPE}":"NTFS","{#FSDNAME}":"C:\","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"D:\","{#FSTYPE}":"CDFS","{#FSDNAME}":"D:\","{#FSDTYPE}":"cdrom"},
	{"{#FSNAME}":"E:\","{#FSTYPE}":"FAT32","{#FSDNAME}":"E:\","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"F:\","{#FSTYPE}":"cdrom","{#FSDNAME}":"F:\","{#FSDTYPE}":"cdrom"},
	{"{#FSNAME}":"\\localhost\c$\services\jboss\jboss-eap-4.3\jboss-as","{#FSTYPE}":"NTFS","{#FSDNAME}":"J:\","{#FSDTYPE}":"remote"},
	{"{#FSNAME}":"\\myco.com\pusers\nwhitehe","{#FSTYPE}":"NTFS","{#FSDNAME}":"P:\","{#FSDTYPE}":"remote"},
]}
 * </pre></li>
 * <li><b>Solaris X86</b>
 * <pre>
{"data":[
	{"{#FSNAME}":"rpool/ROOT/solaris","{#FSTYPE}":"zfs","{#FSDNAME}":"/","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"/devices","{#FSTYPE}":"devfs","{#FSDNAME}":"/devices","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"/dev","{#FSTYPE}":"dev","{#FSDNAME}":"/dev","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"ctfs","{#FSTYPE}":"ctfs","{#FSDNAME}":"/system/contract","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"proc","{#FSTYPE}":"proc","{#FSDNAME}":"/proc","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"mnttab","{#FSTYPE}":"mntfs","{#FSDNAME}":"/etc/mnttab","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"swap","{#FSTYPE}":"tmpfs","{#FSDNAME}":"/system/volatile","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"objfs","{#FSTYPE}":"objfs","{#FSDNAME}":"/system/object","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"sharefs","{#FSTYPE}":"sharefs","{#FSDNAME}":"/etc/dfs/sharetab","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"/usr/lib/libc/libc_hwcap1.so.1","{#FSTYPE}":"lofs","{#FSDNAME}":"/lib/libc.so.1","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"fd","{#FSTYPE}":"fd","{#FSDNAME}":"/dev/fd","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"rpool/ROOT/solaris/var","{#FSTYPE}":"zfs","{#FSDNAME}":"/var","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"swap","{#FSTYPE}":"tmpfs","{#FSDNAME}":"/tmp","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"rpool/export","{#FSTYPE}":"zfs","{#FSDNAME}":"/export","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"rpool/export/home","{#FSTYPE}":"zfs","{#FSDNAME}":"/export/home","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"rpool/export/home/iceadmin","{#FSTYPE}":"zfs","{#FSDNAME}":"/export/home/iceadmin","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"rpool/export/home/releng","{#FSTYPE}":"zfs","{#FSDNAME}":"/export/home/releng","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"rpool","{#FSTYPE}":"zfs","{#FSDNAME}":"/rpool","{#FSDTYPE}":"local"},
	{"{#FSNAME}":"/dev/dsk/c3t1d0s2","{#FSTYPE}":"hsfs","{#FSDNAME}":"/media/VBOXADDITIONS_4.1.18_78361","{#FSDTYPE}":"none"},
	{"{#FSNAME}":"/export/home/nicholas","{#FSTYPE}":"lofs","{#FSDNAME}":"/home/nicholas","{#FSDTYPE}":"none"}
]}
 * </pre></li>
 * </ul>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.filesystem.FileSystemDiscoveryCommandPlugin</code></p>
 */

public class FileSystemDiscoveryCommandPlugin extends AbstractMultiCommandProcessor {
	/** The placeholder token for the file system device name */
	public static final String TOKEN_FSNAME = "{#FSNAME}";
	/** The placeholder token for the file system device type */
	public static final String TOKEN_FSTYPE = "{#FSTYPE}";
	
	/** The placeholder token for the file system directory name */
	public static final String TOKEN_FSDNAME = "{#FSDNAME}";
	/** The placeholder token for the file system type */
	public static final String TOKEN_FSDTYPE = "{#FSDTYPE}";
	

	/**
	 * Creates a new FileSystemDiscoveryCommandPlugin
	 */
	public FileSystemDiscoveryCommandPlugin() {
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor#isDiscovery()
	 */
	@Override
	public boolean isDiscovery() {
		return true;
	}
	
	/**
	 * Returns an array of JSON descriptors providing discovery for file system names and types.
	 * @param commandName The command name
	 * @param args The command optional arguments: <ol>
	 * 	<li>The substitution pattern for a file system name</li>
	 * 	<li>The substitution pattern for a file system type</li>
	 * 	<li>The substitution pattern for a file system directory or mountpoint name</li>
	 * 	<li>The substitution pattern for a file system device type</li>
	 * </ol>
	 * @return an array of JSON descriptors providing discovery for file system names and types.
	 */
	@Override
	@CommandHandler({"fsd", "vfs.fs.discovery"})
	protected String doExecute(String commandName, String... args) {
		if("vfs.fs.discovery".equals(commandName)) {
			args = new String[]{TOKEN_FSNAME};
		}
		int a = args.length;
		if(a<1) return COMMAND_ERROR;
		String[] tokens = new String[a];
		if(args.length>0) {
			tokens[0] = args[0].trim();
		}
		if(args.length>1) {
			tokens[1] = args[1].trim();
		}
		if(args.length>2) {
			tokens[2] = args[2].trim();
		}
		if(args.length>3) {
			tokens[3] = args[3].trim();
		}

		
		StringBuilder b = new StringBuilder("{\"data\":[");
		for(FileSystem fs: sigar.getFileSystemList()) {
			b.append("\n\t{");
			if(a>0) b.append("\"").append(tokens[0]).append("\":\"").append(fs.getDevName()).append("\",");
			if(a>1) b.append("\"").append(tokens[1]).append("\":\"").append(fs.getSysTypeName()).append("\",");
			if(a>2) b.append("\"").append(tokens[2]).append("\":\"").append(fs.getDirName()).append("\",");
			if(a>3) b.append("\"").append(tokens[3]).append("\":\"").append(fs.getTypeName()).append("\",");
			b.deleteCharAt(b.length()-1);
			b.append("},");
		}
		b.deleteCharAt(b.length()-1);
		b.append("\n]}");
		return b.toString().replace("\\", "\\\\");
	}
	
	/**
	 * Basic command line test to pretty print the output
	 * @param args No args
	 * @throws Exception Unlikely
	 */
	public static void main(String[] args) throws Exception {
		System.out.println(new JSONObject(new FileSystemDiscoveryCommandPlugin().doExecute("fsd", TOKEN_FSNAME, TOKEN_FSTYPE, TOKEN_FSDNAME, TOKEN_FSDTYPE)).toString(2));
		System.out.println(new JSONObject(new FileSystemDiscoveryCommandPlugin().doExecute("vfs.fs.discovery", TOKEN_FSNAME, TOKEN_FSTYPE, TOKEN_FSDNAME, TOKEN_FSDTYPE)).toString(2));
	}
	
	
	

}

/*
{
"data":[

{ "{#FSNAME}":"\/",                           "{#FSTYPE}":"rootfs"   },
{ "{#FSNAME}":"\/sys",                        "{#FSTYPE}":"sysfs"    },
{ "{#FSNAME}":"\/proc",                       "{#FSTYPE}":"proc"     },
{ "{#FSNAME}":"\/dev",                        "{#FSTYPE}":"devtmpfs" },
{ "{#FSNAME}":"\/dev\/pts",                   "{#FSTYPE}":"devpts"   },
{ "{#FSNAME}":"\/",                           "{#FSTYPE}":"ext3"     },
{ "{#FSNAME}":"\/lib\/init\/rw",              "{#FSTYPE}":"tmpfs"    },
{ "{#FSNAME}":"\/dev\/shm",                   "{#FSTYPE}":"tmpfs"    },
{ "{#FSNAME}":"\/home",                       "{#FSTYPE}":"ext3"     },
{ "{#FSNAME}":"\/tmp",                        "{#FSTYPE}":"ext3"     },
{ "{#FSNAME}":"\/usr",                        "{#FSTYPE}":"ext3"     },
{ "{#FSNAME}":"\/var",                        "{#FSTYPE}":"ext3"     },
{ "{#FSNAME}":"\/sys\/fs\/fuse\/connections", "{#FSTYPE}":"fusectl"  }

]
}


 */

