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
package jzab;

import java.io.IOException;
import java.util.Arrays;

import au.com.bytecode.opencsv.CSVParser;

/**
 * <p>Title: CSVTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>jzab.CSVTest</code></p>
 * TODO: Convert this into a unit test for command parsing.
 */

public class CSVTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("CSVTest");
		String TEST_STRING = "jmx[\"java.lang:type=Compilation\",TotalCompilationTime]";
		int i = TEST_STRING.indexOf('[');
		if(i>-1 && TEST_STRING.charAt(TEST_STRING.length()-1)==']') {
			log("Processing");
			String command = TEST_STRING.substring(0, i);
			log("Command:[" + command.trim().toLowerCase() + "]");
			String argString = TEST_STRING.substring(i+1, TEST_STRING.length()-1).trim();
			CSVParser parser = new CSVParser(',', '"');
			try {
				String[] commandArgs = parser.parseLine(argString);
				log("Arguments:\n" + Arrays.toString(commandArgs));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			log("Arg String:[" + argString + "]");
		} else {
			log("No Match");
		}
		

	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
