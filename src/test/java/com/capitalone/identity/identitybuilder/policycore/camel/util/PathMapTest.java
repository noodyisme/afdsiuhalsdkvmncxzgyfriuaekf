package com.capitalone.identity.identitybuilder.policycore.camel.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the corresponding object.
 * 
 * @author oqu271
 */
@ExtendWith(MockitoExtension.class)
public class PathMapTest {

	static private final String TEST_VALUE = "test-value";
	static private final Object TEST_VALUE_NON_STRING = new Object();
	static private final String FORBIDDEN_VALUE = "Can't touch this!";
	static private final Predicate<Object> TEST_FILTER = t -> !FORBIDDEN_VALUE.equals(t);

	private Map<String, Object> baseMap;
	private PathMap map;

	@BeforeEach
	public void setup() {
		// Start with the equivalent of PathMap.putPath("a.b.c", TEST_VALUE).
		baseMap = new HashMap<String, Object>();
		Map<String, Object> mapA = new HashMap<String, Object>();
		baseMap.put("a", mapA);
		Map<String, Object> mapB = new HashMap<String, Object>();
		mapA.put("b", mapB);
		mapB.put("c", TEST_VALUE);
		map = new PathMap(baseMap);
	}
	
	@Test
	public void testGetPath() {
		assertSame(TEST_VALUE, map.get("a.b.c"));
		assertEquals(extract(map, "a.b"), map.get("a.b"));
		assertEquals(extract(map, "a"), map.get("a"));
		assertNull(map.get("a.c.d"));
		assertNull(map.get("a.b.c.d"));
		assertNull(map.get("a. .c"));
		assertNull(map.get("x.y"));
	}

	@Test
	public void testGetEmptyPath() {
		assertThrows(IllegalArgumentException.class, () -> map.get(""));
	}

	@Test
	public void testGetEmptyPathComponent() {
		assertThrows(IllegalArgumentException.class, () -> map.get("a..c"));
	}

	@Test
	public void testToString() {
		assertEquals("{a={b={c=test-value}}}", map.toString());
	}

	@Test
	public void testPutAdd() {
		assertNull(map.put("a.b.c2", TEST_VALUE));
		assertSame(TEST_VALUE, extract(map, "a.b.c2"));
	}
	
	@Test
	public void testPutAddInvalidPath() {
		assertThrows(IllegalArgumentException.class, () -> map.put("a.b.c.d", TEST_VALUE));
	}
	
	@Test
	public void testPutAddWithFilter() {
		map = new PathMap(baseMap).filter(TEST_FILTER);
		
		// The filter should pass this.
		assertNull(map.put("a.b.c2", TEST_VALUE));
		assertSame(TEST_VALUE, extract(map, "a.b.c2"));
		
		// The filter should reject this.
		assertNull(map.put("x", FORBIDDEN_VALUE));
		assertFalse(map.containsKey("x"));
	}
	
	@Test
	public void testPutAddWithSparseFilter() {
		map = new PathMap(baseMap).filter(PathMap.SPARSE_FILTER);
		
		// The filter should pass this.
		assertNull(map.put("a.b.c2", TEST_VALUE));
		assertSame(TEST_VALUE, extract(map, "a.b.c2"));
		assertNull(map.put("a.b.c3", TEST_VALUE_NON_STRING));
		assertSame(TEST_VALUE_NON_STRING, extract(map, "a.b.c3"));
		
		
		// The filter should reject null or empty strings.
		assertNull(map.put("x", null));
		assertFalse(map.containsKey("x"));
		assertNull(map.put("x", ""));
		assertFalse(map.containsKey("x"));
	}
	
	@Test
	public void testPutAddWithDefaultSupplier() {
		// The default should create a HashMap.
		assertNull(map.put("b.x.y", TEST_VALUE));
		assertTrue(extract(map, "b.x") instanceof HashMap);
		assertTrue(extract(map, "b") instanceof HashMap);
	}

	@Test
	public void testPutAddWithCustomSupplier() {
		map = map.supplier(TreeMap::new);
		assertNull(map.put("b.x.y", TEST_VALUE));
		assertTrue(extract(map, "b.x") instanceof TreeMap);
		assertTrue(extract(map, "b") instanceof TreeMap);
	}
	
	@Test
	public void testPutAddWithNullSupplier() {
		assertThrows(NullPointerException.class, () -> map.supplier(null));
	}
	
	@Test
	public void testPutReplace() {
		assertEquals(Collections.singletonMap("c", TEST_VALUE), map.put("a.b", TEST_VALUE));
		assertSame(TEST_VALUE, extract(map, "a.b"));
	}
	
	@Test
	public void testPutNewRoot() {
		assertNull(map.put("x", TEST_VALUE));
		assertSame(TEST_VALUE, extract(map, "x"));
	}
	
	@Test
	public void testPutWithMissingParents() {
		assertNull(map.put("a.x.y.z", TEST_VALUE));
		assertSame(TEST_VALUE, extract(map, "a.x.y.z"));
	}
	
	@Test
	public void testPutEmptyPath() {
		assertThrows(IllegalArgumentException.class, () ->  map.put("", TEST_VALUE));
	}

	@Test
	public void testPutEmptyPathComponent() {
		assertThrows(IllegalArgumentException.class, () ->  map.put("a..c", TEST_VALUE));
	}
	
	@Test
	public void testPutAll() {
		Map<String, Object> addedMap = new HashMap<>();
		addedMap.put("foo", "bar");
		addedMap.put("x", "y");
		map.putAll(addedMap);
		assertEquals("bar", baseMap.get("foo"));
		assertEquals("y", baseMap.get("x"));
	}
	
	@Test
	public void testRemoveMissing() {
		assertNull(map.remove("a.b.c2"));
		assertNull(map.remove("a.x.c"));
	}
	
	@Test
	public void testRemove() {
		assertSame(TEST_VALUE, map.remove("a.b.c"));
		assertNull(extract(map, "a.b.c"));
	}

	@Test
	public void testRemove2() {
		Object value = map.get("a.b");
		assertSame(value, map.remove("a.b"));
		assertNull(extract(map, "a.b"));
	}

	@Test
	public void testRemoveWithMissingParents() {
		assertNull(map.remove("a.x.y.z"));
	}
	
	@Test
	public void testContainsPath() {
		assertTrue(map.containsKey("a.b.c"));
		assertTrue(map.containsKey("a.b"));
		assertTrue(map.containsKey("a"));
		assertFalse(map.containsKey("a.b.d"));
		assertFalse(map.containsKey("a.c"));
		assertFalse(map.containsKey("b.c"));
	}
	
	@Test
	public void testRemoveEmptyPath() {
		assertThrows(IllegalArgumentException.class, () -> map.remove(""));
	}
	
	@Test
	public void testSizeClearIsEmpty() {
		assertEquals(1, map.size());
		assertFalse(map.isEmpty());
		map.put("b", TEST_VALUE);
		assertEquals(2, map.size());
		assertEquals(2, baseMap.size());
		assertFalse(map.isEmpty());
		map.clear();
		assertEquals(0, map.size());
		assertEquals(0, baseMap.size());
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void testContainsValue() {
		assertTrue(map.containsValue(baseMap.get("a")));
		map.clear();
		assertFalse(map.containsValue(baseMap.get("a")));		
	}

	@Test
	public void testKeySet() {
		assertEquals(baseMap.keySet(), map.keySet());
	}

	@Test
	public void testValues() {
		assertEquals(baseMap.values(), map.values());
	}

	@Test
	public void testEntrySet() {
		assertEquals(baseMap.entrySet(), map.entrySet());
	}	

	/**
	 * Recursively search a tree of <code>Map</code>s to extract a node value.
	 * <p>
	 * This is the equivalent of {@link PathMap.getPath} but we'll do it by
	 * hand so we don't rely on that.
	 * <p>
	 * Example:
	 * <pre>
	 * Map<String, Object> map = Collections.singletonMap("c", "foo");
	 * map = Collections.singletonMap("b", map);
	 * map = Collections.singletonMap("a", map);
	 * String value = extractField(map, "a.b.c"); 
	 * // value == "foo"
	 * </pre>
	 * 
	 * @param map the map to search
	 * @param path the key path to traverse
	 * @return the value at the specified terminal node, or <code>null</code> if
	 *        the target value (or any parent key) is not found. 
	 */
	@SuppressWarnings("unchecked")
	private Object extract(Map<String, Object> map, String path) {
		String[] names = path.split("\\.", 2);
		Object value = map.get(names[0]);
		if (value == null) {
			return null;
		}
		return (names.length == 1) ? value : extract((Map<String, Object>) value, names[1]);
	}
}
