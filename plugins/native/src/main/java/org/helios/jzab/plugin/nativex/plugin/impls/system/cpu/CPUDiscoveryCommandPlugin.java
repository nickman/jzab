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

import org.helios.jzab.plugin.nativex.plugin.CommandHandler;
import org.helios.jzab.plugin.nativex.plugin.generic.AbstractMultiCommandProcessor;
import org.json.JSONObject;

/**
 * <p>Title: CPUDiscoveryCommandPlugin</p>
 * <p>Description: Provides simple discovery for cpu IDs.</p>
 * <p>For example, if the token argument was <b><code>{#CPUNO}</code></b>, and there were 8 cpus, the returned value would be:<br>
 * <b><pre>
 * {"data": [
 *   {"{#CPUNO}": "0"},
 *   {"{#CPUNO}": "1"},
 *   {"{#CPUNO}": "2"},
 *   {"{#CPUNO}": "3"},
 *   {"{#CPUNO}": "4"},
 *   {"{#CPUNO}": "5"},
 *   {"{#CPUNO}": "6"},
 *   {"{#CPUNO}": "7"}
 * ]}
 * </pre></b>
 * </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.plugin.impls.system.cpu.CPUDiscoveryCommandPlugin</code></p>
 */

public class CPUDiscoveryCommandPlugin extends AbstractMultiCommandProcessor {
	/** The CPU count */
	protected final int CPU_COUNT = sigar.getCpuList().length;
	/** The placeholder token */
	protected final String TOKEN_TOKEN = "{#CPUID}";
	
	/** The json template prefilled using the TOKEN_TOKEN */
	protected final String template;
	
	/**
	 * Creates a new CPUDiscoveryCommandPlugin
	 */
	public CPUDiscoveryCommandPlugin() {
		StringBuilder b = new StringBuilder("{\"data\":[");
		for(int i = 0; i < CPU_COUNT; i++) {
			b.append("{\"").append(TOKEN_TOKEN).append("\":\"").append(i).append("\"},");
		}
		if(CPU_COUNT>0) {  // not sure how this could be false.....
			b.deleteCharAt(b.length()-1);
		}
		b.append("]}");
		template = b.toString();
	}
	/**
	 * Returns an array of JSON descriptors providing discovery for CPU IDs.
	 * @param commandName The command name
	 * @param args The command arguments: Single argument which is the substitution pattern for a cpu id
	 * @return an array of JSON descriptors providing discovery for CPU IDs.
	 */
	@Override
	@CommandHandler("cpud")
	protected String doExecute(String commandName, String... args) {
		if(args.length<1) return COMMAND_ERROR;
		return template.replace(TOKEN_TOKEN, args[0]);
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
	 * Basic command line test
	 * @param args No args
	 * @throws Exception Unlikely
	 */
	public static void main(String[] args) throws Exception {
		System.out.println(new JSONObject(new CPUDiscoveryCommandPlugin().doExecute("cpud", "{#CPUNO}")).toString(2));
	}
	
	
}
