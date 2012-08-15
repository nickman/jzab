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

import java.util.Arrays;

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
 * <p>Sample output (from an Ubuntu/Linux host) using the command <b><code>fsd[{#FSNAME},{#FSTYPE},{#FSDNAME},{#FSDTYPE}]</code></b>:<pre>
{"data":[
	{"{#FSNAME}":"/dev/sda2", "{#FSTYPE}":"ext4", "{#FSDNAME}":"/", "{#FSDTYPE}":"local"},
	{"{#FSNAME}":"proc", "{#FSTYPE}":"proc", "{#FSDNAME}":"/proc", "{#FSDTYPE}":"none"},
	{"{#FSNAME}":"sysfs", "{#FSTYPE}":"sysfs", "{#FSDNAME}":"/sys", "{#FSDTYPE}":"none"},
	{"{#FSNAME}":"binfmt_misc", "{#FSTYPE}":"binfmt_misc", "{#FSDNAME}":"/proc/sys/fs/binfmt_misc", "{#FSDTYPE}":"none"},
	{"{#FSNAME}":"gvfs-fuse-daemon", "{#FSTYPE}":"fuse.gvfs-fuse-daemon", "{#FSDNAME}":"/home/nwhitehead/.gvfs", "{#FSDTYPE}":"none"}
	{"{#FSNAME}":"/dev/sdc1", "{#FSTYPE}":"fuseblk", "{#FSDNAME}":"/media/HELIOS", "{#FSDTYPE}":"none"}
	{"{#FSNAME}":"/dev/sdb1", "{#FSTYPE}":"vfat", "{#FSDNAME}":"/media/PKBACK# 001", "{#FSDTYPE}":"local"}
]}  
 * </pre></p>
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
	@CommandHandler("fsd")
	protected String doExecute(String commandName, String... args) {
		// if(args.length<2) return COMMAND_ERROR;  // we're going to let this one go, for testabilty purposes, we'll default the tokens
		//return template.replace(TOKEN_TOKEN, args[0]);
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
			b.append("{");
			if(a>0) b.append("\"").append(tokens[0]).append("\":\"").append(fs.getDevName()).append("\",");
			if(a>1) b.append("\"").append(tokens[1]).append("\":\"").append(fs.getSysTypeName()).append("\",");
			if(a>2) b.append("\"").append(tokens[2]).append("\":\"").append(fs.getDirName()).append("\",");
			if(a>3) b.append("\"").append(tokens[3]).append("\":\"").append(fs.getTypeName()).append("\",");
			b.deleteCharAt(b.length()-1);
			b.append("},");
		}
		b.deleteCharAt(b.length()-1);
		b.append("]}");
		return b.toString();
	}
	
	/**
	 * Basic command line test to pretty print the output
	 * @param args No args
	 * @throws Exception Unlikely
	 */
	public static void main(String[] args) throws Exception {
		System.out.println(new JSONObject(new FileSystemDiscoveryCommandPlugin().doExecute("fsd", TOKEN_FSNAME, TOKEN_FSTYPE, TOKEN_FSDNAME, TOKEN_FSDTYPE)).toString(2));
		//System.out.println(Arrays.toString(new String[]{TOKEN_FSNAME, TOKEN_FSTYPE, TOKEN_FSDNAME, TOKEN_FSDTYPE}));
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

