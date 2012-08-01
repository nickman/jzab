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
package org.helios.jzab.agent.commands.impl.aggregate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.JSONObject;

/**
 * <p>Title: AggregateFunction</p>
 * <p>Description: Defines aggregate functions for aggregating the values of multiple attributes into one return value</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction</code></p>
 */

public enum AggregateFunction implements IAggregator {
	/** Calculates the sum of the returned values */
	SUM(new SumAggregator(false)),
	/** Calculates the sum of the returned values (strict) */
	STRSUM(new SumAggregator(true)),
	/** Returns the number of items in the result set */
	COUNT(new CountAggregator(false)),
	/** Returns the number of items in the result set (strict) */
	STRCOUNT(new CountAggregator(true)),
	
	/** Returns the average of the returned values */
	AVG(new AverageAggregator(false)),
	/** Returns the average of the returned values (strict) */
	STRAVG(new AverageAggregator(true)),
	/** Returns the minimum value */
	MIN(new MinAggregator(false)),
	/** Returns the minimum value (strict)*/
	STRMIN(new MinAggregator(true)),
	
	/** Returns the maximum value */
	MAX(new MaxAggregator(false)),
	/** Returns the maximum value (strict)*/
	STRMAX(new MaxAggregator(true)),
	
	/** Returns the number of distinct items based on {@link Object#equals(Object)}  */
	DISTINCT(new DistinctAggregator()),
	/** Returns a json group with each unique item and a count of the occurences */
	GROUP(new GroupAggregator()),
	/** Returns a JSON composite of Min, Max, Average and Count */
	MMAC(new MinMaxAvgCntAggregator(false)),
	/** Returns a JSON composite of Min, Max, Average and Count (strict) */
	STRMMAC(new MinMaxAvgCntAggregator(true));
	

	/**
	 * Creates a new AggregateFunction
	 * @param aggr The entry's aggregator
	 */
	private AggregateFunction(IAggregator aggr) {
		this.aggr = aggr;
	}
	
	/** The enum entry's aggregator implementation */
	private final IAggregator aggr;
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
	 */
	@Override
	public Object aggregate(List<Object> items) {
		return aggr.aggregate(items);
	}
	
	
	/**
	 * Returns the AggregateFunction for the passed name. Applies trim and toUpper to the name first.
	 * @param name The name of the function
	 * @return the named AggregateFunction 
	 */
	public static AggregateFunction forName(CharSequence name) {
		if(name==null) throw new IllegalArgumentException("The passed AggregateFunction name was null", new Throwable());
		try {
			return AggregateFunction.valueOf(name.toString().trim().toUpperCase());
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed AggregateFunction name [" + name + "] is not a valid function name", new Throwable());
		}
	}
	
	/**
	 * Retrieves an AggregateFunction by name, returning null for no match
	 * @param name The name of the function to apply
	 * @return the named AggregateFunction or null if one was not found
	 */
	public static AggregateFunction getAggregateFunction(CharSequence name) {
		try {
			return forName(name);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Computes and returns the aggregate for the named aggregator and list of input items
	 * @param name The name of the aggregator function
	 * @param items The list of items to aggregate
	 * @return the aggregate value
	 */
	public static Object aggregate(CharSequence name, List<Object> items) {
		return AggregateFunction.forName(name).aggr.aggregate(items);
	}
	
	/**
	 * Computes and returns the aggregate for the named aggregator and object of nput items.
	 * The object is introspected to determine if it is:<ul>
	 *  <li>Null</li>
	 * 	<li>{@link java.util.Map}</li>
	 * 	<li>{@link java.util.Collection}</li>
	 * 	<li>An array</li>
	 * </ul>.
	 * If it is none of the above, a runtime exception is thrown.
	 * If it is a map or an array, it is converted to a list for aggregate computation.
	 * @param name The name of the aggregator function
	 * @param item The object of items to aggregate
	 * @return the aggregate value
	 * TODO:  Do we need to support multi dimmensional arrays ?
	 */
	@SuppressWarnings("unchecked")
	public static Object aggregate(CharSequence name, Object item) {				
		final List<Object> items;		
		final AggregateFunction function = AggregateFunction.forName(name);
		if(item==null) {
			items = Collections.EMPTY_LIST;
		} else if(item instanceof Map) {
			Map<Object, Object> map = (Map<Object, Object>)item;
			items = new ArrayList<Object>(map.keySet());
		} else if(item instanceof Collection) {
			items = new ArrayList<Object>((Collection<Object>)item);
		} else if(item.getClass().isArray())  {			
			int length = Array.getLength(item);
			items = new ArrayList<Object>(length);
			for(int i = 0; i < length; i++) {
				items.add(i, Array.get(item, i));
			}
		} else {
			throw new IllegalArgumentException("Aggregate object of type [" + item.getClass().getName() + "] was not a Map, Collection or Array", new Throwable());
		}
		return function.aggregate(items);					
	}
	
	
	
	/**
	 * <p>Title: NumericAggregator</p>
	 * <p>Description: Aggregates a numeric computation of all the values that are assumed to be numbers or implement {@link INumberProvider}</p> 
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.SumAggregator</code></p>
	 */
	public static abstract class NumericAggregator implements IAggregator {
		/** If true, throws an error if any item is null or not a number */
		protected final boolean strict;
		/**
		 * Creates a new SumAggregator
		 * @param strict If true, throws an error if any item is null or not a number. Otherwise ignores the non number items
		 */
		public NumericAggregator(boolean strict) {
			this.strict = strict;
		}
		/**
		 * Sifts through the passed items, looking for exceptions and if non-strict, returns a list of compliant items.
		 * @param items The list of items to sift
		 * @return a list of valid items and instances of {@link INumberProvider}s wrapped as Numbers.
		 */
		protected List<Number> sift(List<Object> items) {
			if(items==null) {
				if(strict) throw new RuntimeException("List of items was null and aggregator was strict", new Throwable());
			}			
			List<Number> numbers = new ArrayList<Number>(items.size());
			for(Object o: items) {
				if(o==null) {
					if(strict) throw new RuntimeException("List of items had a null and aggregator was strict", new Throwable());
					continue;
				}
				if(o instanceof Number) {
					numbers.add((Number)o);
					continue;
				}
				if(o instanceof INumberProvider) {
					numbers.add(NumberWrapper.getNumber((INumberProvider)o));
					continue;					
				}
				if(strict) throw new RuntimeException("List of items had a non-number item [" + o.getClass().getName() + "] and aggregator was strict", new Throwable());
			}
			return numbers;
		}
		
	}
	
	
	/**
	 * <p>Title: SumAggregator</p>
	 * <p>Description: Aggregates the numeric sum of all the values</p> 
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.SumAggregator</code></p>
	 */
	public static class SumAggregator extends NumericAggregator {
		/**
		 * Creates a new SumAggregator
		 * @param strict If true, throws an error if any item is null or not a number. Otherwise ignores the non number items
		 */
		public SumAggregator(boolean strict) {
			super(strict);
		}
		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
		 */
		@Override
		public Double aggregate(List<Object> items) {
			double d = 0D;
			for(Number n: sift(items)) {
				d += n.doubleValue();
			}
			return d;
		}
		
	}
	
	/**
	 * <p>Title: AverageAggregator</p>
	 * <p>Description: Aggregates the mathematical average  of all the values</p> 
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.AverageAggregator</code></p>
	 */
	public static class AverageAggregator extends NumericAggregator {
		/**
		 * Creates a new AverageAggregator
		 * @param strict If true, throws an error if any item is null or not a number. Otherwise ignores the non number items
		 */
		public AverageAggregator(boolean strict) {
			super(strict);
		}
		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
		 */
		@Override
		public Double aggregate(List<Object> items) {
			double total = 0D;
			double count = 0D;			
			for(Number n: sift(items)) {
				count++;
				total += n.doubleValue();
			}
			if(total==0 || count==0) return 0D;
			return total/count;
		}		
	}
	
	/**
	 * <p>Title: MinAggregator</p>
	 * <p>Description: Computes the lowest numeric value of all the values. If non-strict, returns -1D for an empty list</p> 
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.MinAggregator</code></p>
	 */
	public static class MinAggregator extends NumericAggregator {
		/**
		 * Creates a new MinAggregator
		 * @param strict If true, throws an error if any item is null or not a number. Otherwise ignores the non number items
		 */
		public MinAggregator(boolean strict) {
			super(strict);
		}
		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
		 */
		@Override
		public Double aggregate(List<Object> items) {
			List<Number> numbers = sift(items);
			if(numbers.isEmpty()) {
				if(strict) throw new RuntimeException("List of items for MIN was empty and aggregator was strict", new Throwable());
				return -1D;
			}
			double min = Double.MAX_VALUE;
			for(Number n: sift(items)) {
				if(n.doubleValue() < min) min = n.doubleValue();
			}
			return min;
		}
	}

	/**
	 * <p>Title: MaxAggregator</p>
	 * <p>Description: Computes the highest numeric value of all the values. If non-strict, returns -1D for an empty list</p> 
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.MaxAggregator</code></p>
	 */
	public static class MaxAggregator extends NumericAggregator {
		/**
		 * Creates a new MaxAggregator
		 * @param strict If true, throws an error if any item is null or not a number. Otherwise ignores the non number items
		 */
		public MaxAggregator(boolean strict) {
			super(strict);
		}
		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
		 */
		@Override
		public Double aggregate(List<Object> items) {
			List<Number> numbers = sift(items);
			if(numbers.isEmpty()) {
				if(strict) throw new RuntimeException("List of items for MAX was empty and aggregator was strict", new Throwable());
				return -1D;
			}
			double max = Double.MIN_VALUE;
			for(Number n: sift(items)) {
				if(n.doubleValue() > max) max = n.doubleValue();
			}
			return max;
		}		
	}
	
	
	/**
	 * <p>Title: CountAggregator</p>
	 * <p>Description: Aggregates the items to a simple count of the number of items. If strict, throws an exception if any item is null, otherwise ignores null items.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.CountAggregator</code></p>
	 */
	public static class CountAggregator implements IAggregator {
		/** The strict indicator */
		private final boolean strict;
		
		/**
		 * Creates a new CountAggregator
		 * @param strict If true, implements strict count rules
		 */
		public CountAggregator(boolean strict) {
			this.strict = strict;
		}
		
		/**
		 * Counts the number if items
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
		 */
		@Override
		public Object aggregate(List<Object> items) {
			if(items==null) {
				if(strict) {
					throw new RuntimeException("List of items for Count was null and aggregator was strict", new Throwable());
				} 
				return 0;
			}
			if(strict) {
				for(Object o: items) {
					if(o==null) throw new RuntimeException("List of items for Count contained a null and aggregator was strict", new Throwable());
				}
			} 
			return items.size();
		}
	}
	
	
	/**
	 * <p>Title: DistinctAggregator</p>
	 * <p>Description: Aggregates the items to a simple count of the number of distinct items where equality is determined by {@link Object#equals(Object)}.  </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.DistinctAggregator</code></p>
	 */
	public static class DistinctAggregator implements IAggregator {

		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
		 */
		@Override
		public Object aggregate(List<Object> items) {
			if(items==null || items.isEmpty()) return 0;
			Set<Object> set = new CopyOnWriteArraySet<Object>(items);
			return set.size();
		}		
	}
	
	/**
	 * <p>Title: GroupAggregator</p>
	 * <p>Description: Aggregates the items to a a json group with each unique item and a count of the occurences where equality is determined by and items
	 * are represented using {@link Object#toString()}.  Null items are ignored.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.GroupAggregator</code></p>
	 */
	public static class GroupAggregator implements IAggregator {
		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
		 */
		@Override
		public Object aggregate(List<Object> items) {			
			if(items==null || items.isEmpty()) return new JSONObject();
			Map<String, Long> map = new HashMap<String, Long>();
			for(Object o: items) {
				if(o==null) continue;
				String key = o.toString();
				Long l = map.get(key);
				if(l==null) {
					l = 0L;
				}
				l = l++;
				map.put(key, l);
			}			
			return new JSONObject(map);
		}		
	}
	
	
	/**
	 * <p>Title: MinMaxAvgCntAggregator</p>
	 * <p>Description: Aggregates the passed numberic items to a JSON object representing the min, max, average and count. If non-strict, returns -1D values for a null list and for null items</p> 
	 * <p><code>org.helios.jzab.agent.commands.impl.aggregate.AggregateFunction.MinMaxAvgCntAggregator</code></p>
	 */
	public static class MinMaxAvgCntAggregator extends NumericAggregator {
		/** The MinMaxAvgCnt key for the min value */
		public static final String KEY_MIN = "min";
		/** The MinMaxAvgCnt key for the max value */
		public static final String KEY_MAX = "max";
		/** The MinMaxAvgCnt key for the avg value */
		public static final String KEY_AVG = "avg";
		/** The MinMaxAvgCnt key for the value count */
		public static final String KEY_CNT = "cnt";
		
		/** The base response for a non-strict MinMaxAvgCnt with null items */
		private static final JSONObject BASE_RESP;
		
		static {
			Map<String, Double> map = new HashMap<String, Double>(4);
			map.put(KEY_MIN, -1D); map.put(KEY_MAX, -1D); map.put(KEY_AVG, -1D); map.put(KEY_CNT, 0D);
			BASE_RESP = new JSONObject(map);
		}
		
		/**
		 * Creates a new MaxAggregator
		 * @param strict If true, throws an error if any item is null or not a number. Otherwise ignores the non number items
		 */
		public MinMaxAvgCntAggregator(boolean strict) {
			super(strict);
		}
		/**
		 * {@inheritDoc}
		 * @see org.helios.jzab.agent.commands.impl.aggregate.IAggregator#aggregate(java.util.List)
		 */
		@Override
		public JSONObject aggregate(List<Object> items) {
			List<Number> numbers = sift(items);
			if(numbers.isEmpty()) {
				if(strict) throw new RuntimeException("List of items for MinMaxAvgCnt was empty and aggregator was strict", new Throwable());
				return BASE_RESP;
			}
			double max = Double.MIN_VALUE;
			double min = Double.MAX_VALUE;
			double total = 0D;
			double count = 0D;
			for(Number n: sift(items)) {
				Double d = n.doubleValue();
				if(d > max) max = d;
				if(d < min) min = d;
				total += d;
				count++;
			}
			double avg = (total==0D || count==0D) ? 0D : (total/count);
			Map<String, Double> map = new HashMap<String, Double>(4);
			map.put(KEY_MIN, min); map.put(KEY_MAX, max); map.put(KEY_AVG, avg); map.put(KEY_CNT, count);
			return new JSONObject(map);
		}		
	}

	
}
