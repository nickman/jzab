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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: AtomicIntCounter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.rolling.AtomicIntCounter</code></p>
 */
public class AtomicIntCounter {
	/** The maximum value the counter can be incremented to */
	protected final int maxValue;
	/** The underlying atomic */
	protected final AtomicInteger atom = new AtomicInteger(0);

	/**
	 * Creates a new AtomicIntCounter
	 * @param maxValue The maximum value that can be incremented to
	 */
	public AtomicIntCounter(int maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * Returns the max value of this counter
	 * @return the max value of this counter
	 */
	public int getMaxValue() {
		return maxValue;
	}

	public int tick() {
        for (;;) {
            int current = atom.get();
            if(current==maxValue) return maxValue;
            int next = current + 1;
            if (atom.compareAndSet(current, next)) return next;
            break;
        }		
        return tick();
	}
	
	
	public int get() {
		return atom.get();
	}

}
