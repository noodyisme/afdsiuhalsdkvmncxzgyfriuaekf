package com.capitalone.identity.identitybuilder.policycore.camel.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A wrapper around a <i>Map</i> that adds support for multi-level keys.
 * <p>
 * Only some of the operations are "path aware" and are listed in the following table.
 * Any operations not in the following table work <i>exactly</i> like they do
 * on the underlying map.
 * <p>
 * <table border="1" cellpadding="5">
 * <caption>Path-Aware Operations</caption>
 * <tr><th>Operation</th><th>Function</th></tr>
 * <tr><td>containsKey</td><td>acts like "contains key path"</td></tr>
 * <tr><td>get</td><td>gets value at the key path</td></tr>
 * <tr><td>put</td><td>stores the value at the key path</td></tr>
 * <tr><td>remove</td><td>removes the value at the key path</td></tr>
 * </table>
 * 
 * <h2>Key Paths</h2>
 * All standard <i>Map</i> methods that take a <code>key</code> argument will interpret
 * the key as a path.
 * <p>
 * A <i>key path</i> is a key with one or more non-empty components, separated by
 * periods. Examples of valid key paths are "foo", "foo.bar", and "foo.bar.CapitalOne".
 * <p>
 * Simple key paths like "foo" and "bar" are treated exactly like regular
 * keys in the underlying map. When a key path has multiple components, though,
 * all components other than the last one must also refer to maps.
 * 
 * <h2><i>Get</i> Operations</h2>
 * <p>
 * Using <code>get("foo.bar")</code> is the equivalent of:
 * <ol>
 * <li><code>get("foo")</code> on the underlying map</li>
 * <li>Cast the result as a <code>Map&lt;String, Object&gt;</code></li>
 * <li><code>get("bar")</code> on that map</li>
 * </ol>
 * Obviously, if "foo" doesn't correspond to a map then we have
 * a problem! We take a lenient approach so in this example we would
 * return <code>null</code> to indicate that "foo.bar" was not found.
 *
 * <h2><i>Put</i> Operations</h2>
 * <i>Put</i> operations work very naturally but there are a few differences:
 * <p>
 * <ul>
 * <li>Parent maps are created as needed</li>
 * <li>Filtering can be done on values to bypass <i>put</i> operations.
 * <li>Existing parent nodes must be maps. Unlike <i>get</i> operations,
 *     if a path causes traversal through an existing key that is not a map
 *     an exception is thrown.</li>
 * </ul>
 * 
 * <h4>Parent Map Creation</h4>
 * In keeping with our "lenient" approach, if any parent maps are missing
 * when doing a <i>put</i>, they are created automatically as needed.
 * <p>
 * For example, if the underlying map is empty and we do a <code>put("a.b.c", "foo")</code>
 * then two new maps will be created (referenced by "a" and "a.b").
 * The map at "a.b" will have a key "c" with the value "foo".
 * <p>
 * How are these new maps created? A default <i>Supplier</i> is used to generate
 * <code>HashMap</code> objects.  You can easily provide your own supplier
 * to generate other types of maps like this:
 * <pre>    PathMap map = new PathMap(originalMap).supplier(TreeMap::new);</pre>
 * 
 * <h4>Put Filtering</h4>
 * Sometimes it is convenient to skip a <i>put</i> operation based on the value
 * being saved. For this purpose a <i>Predicate</i> filter can be supplied that
 * returns <code>false</code> for any unwanted values.
 * <p>
 * For example, if you want to build a "sparse" map without constantly checking
 * whether the value you are setting is <code>null</code> or an empty string,
 * the following code will handle it for you:
 * <pre>     PathMap map = new PathMap(originalMap).filter(PathMap.SPARSE_FILTER);</pre>
 * With that filter in place, invoking <code>put("x.y.z", null)</code> will return
 * <code>null</code> and not modify the map at all.
 * 
 * <h2><i>Remove</i> Operations</h2>
 * Remove behaves just like you would expect although if the key path forces
 * traversal of a non-map node the <i>remove</i> will return <code>null</code>
 * and fail silently as if the path was "not found".
 * 
 * @author oqu271
 */
public class PathMap implements Map<String, Object> {
	/** A value filter that rejects nulls and empty strings. */
	public static final Predicate<Object> SPARSE_FILTER = t -> t != null && !(t instanceof String && ((String) t).isEmpty());
	
	/** The underlying map to manipulate. */
	private Map<String, Object> map;
	/** An optional pass filter for values in <i>put</i> operations. */
	private Predicate<Object> filter;
	/** The supplier of new maps for created nodes (defaults to supplying a <code>HashMap</code>. */
	private Supplier<Map<String, Object>> supplier = HashMap::new;
	
	/**
	 * Wraps an existing map with a map that treats keys as "paths" that
	 * route through nested maps.
	 * 
	 * @param map the map to wrap
	 */
	public PathMap(Map<String, Object> map) {
		this.map = map;
	}
	
	/**
	 * Sets an optional pass filter that can prevent certain values from
	 * being set in the map.
	 * <p>
	 * If a filter is present any <i>put</i> operation will run the value
	 * being stored through the filter and if it does not pass the filter the
	 * <i>put</i> will do nothing and return <code>null</code>.
	 * 
	 * @param filter the filter to set, or <code>null</code> to disable filtering
	 * @return the map
	 */
	public PathMap filter(Predicate<Object> filter) {
		this.filter = filter;
		return this;
	}
	
	/**
	 * Sets a supplier used when a <code>put</code> operation needs to create
	 * a missing, intermediate map.
	 * <p>
	 * A <code>HashMap</code> is used by default.
	 * 
	 * @param supplier the supplier to set (e.g.; <code>TreeMap:new</code>)
	 * @return the map
	 * @throws NullPointerException if <code>supplier</code> is <code>null</code>
	 */
	public PathMap supplier(Supplier<Map<String, Object>> supplier) {
		if (supplier == null) {
			throw new NullPointerException("The supplier cannot be null");
		}
		this.supplier = supplier;
		return this;
	}
	
	@Override
	public int size() { return map.size(); }

	@Override
	public boolean isEmpty() { return map.isEmpty(); }

	@Override
	public boolean containsKey(Object key) {
		LinkedList<String> keyList = pathToList((String) key);
		Map<String, Object> parent = parentLookup(map, keyList, false);
		return parent != null && parent.containsKey(keyList.getLast());
	}

	@Override
	public boolean containsValue(Object value) { return map.containsValue(value); }

	@Override
	public Object get(Object key) {
		LinkedList<String> keyList = pathToList((String) key);
		Map<String, Object> parent = parentLookup(map, keyList, false);
		return (parent != null) ? parent.get(keyList.getLast()) : null;
	}

	@Override
	public Object put(String key, Object value) {
		if (filter != null && !filter.test(value)) {
			return null;
		}
		LinkedList<String> keyList = pathToList((String) key);		
		Map<String, Object> parent = parentLookup(map, keyList, true);
		return parent.put(keyList.getLast(), value);
	}

	@Override
	public Object remove(Object key) {
		LinkedList<String> keyList = pathToList((String) key);	
		Map<String, Object> parent = parentLookup(map, keyList, false);
		return (parent != null) ? parent.remove(keyList.getLast()) : null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) { map.putAll(m); }

	@Override
	public void clear() { map.clear(); }

	@Override
	public Set<String> keySet() { return map.keySet(); }

	@Override
	public Collection<Object> values() { return map.values(); }

	@Override
	public Set<Entry<String, Object>> entrySet() { return map.entrySet(); }
	
	@Override
	public String toString() { return map.toString(); }
	
	/**
	 * Returns a list that represents the components in the supplied
	 * path string.
	 * 
	 * @param path the key path to process
	 * @return a list representing the key path components
	 * @throws IllegalArgumentException if <code>path</code> is empty or
	 *        contains empty path components
	 */
	@SuppressWarnings("squid:S1319")
	public static LinkedList<String> pathToList(String path) {
		LinkedList<String> list = Stream.of(path.split("\\.")).collect(Collectors.toCollection(LinkedList::new));
		for (String key : list) {
			if (key.isEmpty()) {
				throw new IllegalArgumentException("An empty key path entry was specified");
			}
		}
		return list;
	}

	/**
	 * A common method that recursively traverses the map looking for
	 * the parent node of the entry specified by the key list.
	 * <p>
	 * By returning the parent node, we can support callers implementing
	 * the get, put, and remove actions with just one method. 
	 * <p>
	 * When <code>create</code> is <code>true</code>, missing intermediate
	 * maps are created automatically using the current supplier.
	 * If not, missing nodes result in an exception.
	 * <p>
	 * Returns the <i>Map</i> entry at at the node specified by the key list,
	 * or <code>null</code> it the entry doesn't exist or the key list crosses
	 * a non-Map node.
	 * <p>
	 * <b>NOTE:</b> As part of the recursion, the <code>keyList</code> will be
	 * modified by this call. If the parent node is found, the keyList will consist
	 * of only the last entry (which is useful to the caller for doing an operation
	 * on the underlying map).
	 * 
	 * @param node the current map node to search
	 * @param keyList the remaining path keys to search for
	 *        (must not be empty and will be modified during traversal)
	 * @param create whether to create missing ancestor node maps
	 * @return the map at the specified path, or <code>null</code> if
	 *        the target path (or any parent key) is not found is not a map 
	 * @throws IllegalArgumentException if traversal crosses a non-<i>Map</i> node
	 *        (including a missing one if <code>create</code> is <code>false</code>)
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> parentLookup(Map<String, Object> node, LinkedList<String> keyList, boolean create) {
		if (keyList.size() <= 1) {
			return node;
		}
		String key = keyList.removeFirst();
		Object value = node.get(key);
		if (!(value instanceof Map<?,?>)) {
			if (value == null && create) {
				value = supplier.get();
				node.put(key, value);
			} else if (!create) {
				return null;
			} else {
				throw new IllegalArgumentException(String.format("The path traversed a non-Map node at %s", key));
			}
		}
		return parentLookup((Map<String, Object>) value, keyList, create);
	}
}


/*
 * Copyright 2019 Capital One Financial Corporation All Rights Reserved.
 * 
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */