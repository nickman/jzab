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
package org.helios.jzab.rolling;

import javax.management.ObjectName;

import org.helios.jzab.util.JMXHelper;

/**
 * <p>Title: RollingMetricService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.rolling.RollingMetricService</code></p>
 */

public class RollingMetricService {
	
	/** The singleton instance */
	private static volatile RollingMetricService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The service's JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.jzab.rolling:service=WeAreRolling");
	/**
	 * Returns the RollingMetricService singleton instance
	 * @return the RollingMetricService singleton instance
	 */
	public static RollingMetricService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RollingMetricService();
				}
			}
		}
		return instance;
		
	}
	
	private RollingMetricService() {
		
	}
}
