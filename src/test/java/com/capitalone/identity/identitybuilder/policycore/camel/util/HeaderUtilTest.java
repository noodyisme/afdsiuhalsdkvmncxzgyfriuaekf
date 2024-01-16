package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.camel.routes.PolicyTestSupportPolicyExchange;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import com.capitalone.identity.identitybuilder.policycore.utils.XmlDslCamelTestContextConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootContextAwareTest(contextConfigClasses = HeaderUtilTest.ContextConfig.class)
public class HeaderUtilTest implements PolicyTestSupportPolicyExchange {

	private Exchange exchange;
	private HeaderUtil consumer;
	private Map<String, Object> headers;
	private Map<String, Object> params;

	@Autowired
	private CamelContext camelContext;

	@Override
	public CamelContext camelContext() {
		return this.camelContext;
	}

	@BeforeEach
	public void setup() {
		Map<String, Object> subMap = new HashMap<>();
		subMap.put("map1", "map1-value");
		subMap.put("map2", "map2-value");
		
		List<Object> list = new ArrayList<>();
		list.add("l0");
		list.add("l1");
		list.add(3);
		
		params = new PathMap(new HashMap<>());
		params.put("string", "string-value");
		params.put("boolean", true);
		params.put("integer", 42);
		params.put("double", 42.5);
		params.put("map", subMap);
		params.put("list", list);
		
		exchange = policyExchange(params, null);
		headers = new PathMap(exchange.getIn().getHeaders());
		
		consumer = new HeaderUtil();
	}
	
	@Test
	public void testMissingHeader() {
		assertThrows(IllegalArgumentException.class, () -> consumer.process(exchange, null));
	}
	
	@Test
	public void testValueBody() {
		consumer.process(exchange, "foo value body 'string'");
		assertEquals(params.get("string"), headers.get("foo"));
	}
	
	@Test
	public void testValueBodyMulltipleValues() {
		assertThrows(IllegalArgumentException.class, () -> consumer.process(exchange, "foo value body 'string, boolean'"));
	}
	
	@Test
	public void testMapBodyAllValueTypes() {
		consumer.process(exchange, "foo map body 'string, boolean, integer, double, list, map, map.*'");
		assertEquals(params.get("string"),   headers.get("foo.string"));
		assertEquals(params.get("boolean"),  headers.get("foo.boolean"));
		assertEquals(params.get("integer"),  headers.get("foo.integer"));
		assertEquals(params.get("double"),   headers.get("foo.double"));
		assertEquals(params.get("list"),     headers.get("foo.list"));
		assertEquals(params.get("map"),      headers.get("foo.map"));
		// Check flattened map.
		assertEquals(params.get("map.map1"), headers.get("foo.map1"));
		assertEquals(params.get("map.map2"), headers.get("foo.map2"));
	}
	
	@Test
	public void testMapBodyRename() {
		consumer.process(exchange, "foo map body 'string{rename=stringx}, boolean{rename=booleanx}, integer{rename=integerx}, double{rename=doublex}, list{rename=listx}, map{rename=mapx}'");
		assertEquals(params.get("string"),   headers.get("foo.stringx"));
		assertEquals(params.get("boolean"),  headers.get("foo.booleanx"));
		assertEquals(params.get("integer"),  headers.get("foo.integerx"));
		assertEquals(params.get("double"),   headers.get("foo.doublex"));
		assertEquals(params.get("list"),     headers.get("foo.listx"));
		assertEquals(params.get("map"),      headers.get("foo.mapx"));
	}

	@Test
	public void testBodyJson() throws IOException {
		consumer.process(exchange, "foo json body 'string, boolean, integer, double, list, map'");
		assertEquals("{\"boolean\":true,\"double\":42.5,\"integer\":42,\"list\":[\"l0\",\"l1\",3],\"map\":{\"map2\":\"map2-value\",\"map1\":\"map1-value\"},\"string\":\"string-value\"}", headers.get("foo"));
	}

	@Test
	public void testBodyJsonNonJsonValue() {
		params.put("xyz", new Object());
		assertThrows(ChassisSystemException.class, () -> consumer.process(exchange, "foo json body 'string, xyz'"));
	}

	@Test
	public void testMapHeaderNonMapAnnotations() {
		headers.put("foo1.phoneNumber", "0123456789");
		headers.put("foo1.flag", "true");
		headers.put("foo1.date", new Date());
		consumer.process(exchange, "foo map hdr 'foo1.phoneNumber{rename=phone}, foo1.flag{type=boolean}, foo1.date'");
		assertEquals("0123456789", headers.get("foo.phone"));
		assertTrue((Boolean) headers.get("foo.flag"));
		assertSame(headers.get("foo1.date"), headers.get("foo.date"));
	}

	@Test
	public void testMapHeaderMapAnnotationSyntax() {
		headers.put("foo1.phoneNumber", "01234567890");
		assertThrows(IllegalArgumentException.class, () ->  consumer.process(exchange, "foo map hdr 'foo1{syntax=phone}'"));
	}
	
	@Test
	public void testMapHeaderAnnotationType() {
		headers.put("foo1.phoneNumber", "01234567890");
		assertThrows(RequestValidationException.class, () -> consumer.process(exchange, "foo map hdr 'foo1{type=boolean}'"));
	}
	
	@Test
	public void testMapHeaderFlattenedAnnotationRename() {
		headers.put("foo1.phoneNumber", "01234567890");
		assertThrows(IllegalArgumentException.class, ()  -> consumer.process(exchange, "foo map hdr 'foo1.*{rename=bar1}'"));
	}
	
	@Test
	public void testValueHeader() {
		headers.put("a", "a-value");
		consumer.process(exchange, "foo value hdr 'a'");
		assertEquals("a-value", headers.get("foo"));
	}
					
	@Test
	public void testMapHeaderAllValueTypes() {
		headers.put("hdr1", params);
		consumer.process(exchange, "foo map hdr 'hdr1.string, hdr1.boolean, hdr1.integer, hdr1.double, hdr1.map, hdr1.map.*'");
		assertEquals(params.get("string"),   headers.get("foo.string"));
		assertEquals(params.get("boolean"),  headers.get("foo.boolean"));
		assertEquals(params.get("integer"),  headers.get("foo.integer"));
		assertEquals(params.get("double"),   headers.get("foo.double"));
		assertEquals(params.get("map"),      headers.get("foo.map"));
		// Check flattened map.
		assertEquals(params.get("map.map1"), headers.get("foo.map1"));
		assertEquals(params.get("map.map2"), headers.get("foo.map2"));
	}

	
	@Test
	public void testAttrPairsHeader() {
		params.remove("map"); // Not supported by this operation.
		params.remove("list"); // Not supported by this operation.
		headers.put("hdr1", params);
		consumer.process(exchange, "foo map body 'string, boolean, integer, double'\n"
								+ " bar attrpairs hdr 'foo.*'");
		assertEquals("["
				+ "{\"attributeName\":\"boolean\",\"attributeValue\":\"true\"},"
				+ "{\"attributeName\":\"double\",\"attributeValue\":\"42.5\"},"
				+ "{\"attributeName\":\"integer\",\"attributeValue\":\"42\"},"
				+ "{\"attributeName\":\"string\",\"attributeValue\":\"string-value\"}]",
				headers.get("bar"));
	}

	@Test
	public void testNestedTargetPath() {
		consumer.process(exchange, "foo.bar.heimdall map body 'string, boolean'");
		assertEquals(params.get("string"),   headers.get("foo.bar.heimdall.string"));
		assertEquals(params.get("boolean"),  headers.get("foo.bar.heimdall.boolean"));
	}
	
	@Test
	public void testNestedTarrgetPathNonMap() {
		TreeMap<String, Object> map = new TreeMap<>();
		map.put("bar", "some-string-not-a-map");
		headers.put("foo", map);
		assertThrows(IllegalArgumentException.class, ()  -> consumer.process(exchange, "foo.bar.heimdall map body 'string, boolean'"));
	
	}
	
	@Test
	public void testBodySparseMapping() {
		params.put("null", null);
		params.put("empty", "");
		params.put("onlyWhiteSpace", "   ");
		
		consumer.process(exchange, "foo map body 'string, null, empty, onlyWhiteSpace' \n");
		assertEquals(1, exchange.getIn().getHeader("foo", Map.class).size());
		assertEquals(params.get("string"),   headers.get("foo.string"));
	}
	
	@Test
	public void testMultipleMapDirectivesAdditive() {
		Map<String, Object> map = new HashMap<>();
		map.put("hdr1a", "hdr1a-value");
		map.put("hdr1b", "hdr1b-value");
		headers.put("hdr1", map);
		
		consumer.process(exchange, "foo map body 'string, integer' hdr 'hdr1.*'");
		assertEquals(params.get("string"),   headers.get("foo.string"));
		assertEquals(params.get("integer"),  headers.get("foo.integer"));
		assertEquals(map.get("hdr1a"),  headers.get("foo.hdr1a"));
		assertEquals(map.get("hdr1b"),  headers.get("foo.hdr1b"));
	}

	@Test
	public void testMultipleHeadersAndDirectives() {
		headers.put("hdr1", "hdr1-value");
		consumer.process(exchange, 
				"foo map body 'string, boolean' hdr 'hdr1'\n" +
				"bar map body 'integer'");
		assertEquals(params.get("string"), headers.get("foo.string"));
		assertEquals(params.get("boolean"), headers.get("foo.boolean"));
		assertEquals(headers.get("hdr1"), headers.get("foo.hdr1"));
		assertEquals(params.get("integer"), headers.get("bar.integer"));
	}
	
	@Test
	public void testMultipleHeadersAndDirectivesNoNewline() {
		assertThrows(IllegalArgumentException.class, ()  -> consumer.process(exchange,
				"foo map body 'string, integer'" +
				"bar map body  'model'"));
	}

	@Test
	public void testReplaceMapWithValue() {
		consumer.process(exchange, 
				"foo map   body 'string, integer'\n" +
				"foo value body 'boolean'");
		assertEquals(params.get("boolean"), headers.get("foo"));
	}
	
	@Test
	public void testReplaceValueWithMap() {
		consumer.process(exchange, 
				"foo value body 'integer'\n" +
				"foo map   body 'string, boolean'\n");
		assertEquals(params.get("string"), headers.get("foo.string"));
		assertEquals(params.get("boolean"), headers.get("foo.boolean"));
	}
	
	@Test
	public void testAddToExistingMap() {
		// MAPA to an existing map should add the entries to it.
		consumer.process(exchange, 
				"foo.bar.xyz map  body 'string, integer'\n" +
				"foo.bar.xyz mapa body 'boolean'\n" );
		assertEquals(params.get("string"), headers.get("foo.bar.xyz.string"));
		assertEquals(params.get("integer"), headers.get("foo.bar.xyz.integer"));
		assertEquals(params.get("boolean"), headers.get("foo.bar.xyz.boolean"));
	}
	
	@Test
	public void testAddToNonExistingMap() {
		// MAPA to a non-existent entry should create a map.
		consumer.process(exchange, 
				"foo.bar     map  body 'string, integer'\n" +
				"foo.bar.xyz mapa body 'boolean'\n");
		assertEquals(params.get("string"), headers.get("foo.bar.string"));
		assertEquals(params.get("integer"), headers.get("foo.bar.integer"));
		assertEquals(params.get("boolean"), headers.get("foo.bar.xyz.boolean"));
	}
	
	@Test
	public void testAddToNonMap() {
		// MAPA against a non-map entry should replace it with a map.
		consumer.process(exchange, 
				"foo.bar.xyz value body 'integer'\n" +
				"foo.bar.xyz mapa  body 'string, boolean'\n");
		assertEquals(params.get("string"), headers.get("foo.bar.xyz.string"));
		assertEquals(params.get("boolean"), headers.get("foo.bar.xyz.boolean"));
	}
	
	@Test
	public void testEmptyLinesAndContinuedLinesInDirectives() {
		consumer.process(exchange, "\n   \nfoo map body 'string, double' \\   \nbody 'integer, boolean'\n  \n ");
		assertEquals(params.get("string"), headers.get("foo.string"));
		assertEquals(params.get("integer"), headers.get("foo.integer"));
		assertEquals(params.get("boolean"), headers.get("foo.boolean"));
		assertEquals(params.get("double"), headers.get("foo.double"));
	}
	
	@Test
	public void testCommentLine() {
		consumer.process(exchange, "   // a comment line \n   # another comment line\nfoo map body 'string, double' \\ \n  body 'integer, boolean'");
		assertEquals(params.get("string"), headers.get("foo.string"));
		assertEquals(params.get("integer"), headers.get("foo.integer"));
		assertEquals(params.get("boolean"), headers.get("foo.boolean"));
		assertEquals(params.get("double"), headers.get("foo.double"));
	}

	@Test
	public void testContinuedLinesToEOF() {
		assertThrows(IllegalArgumentException.class, ()  ->
				consumer.process(exchange, "\n   \nfoo map body 'string, double' \\   \nbody 'integer, boolean'\n  \\"));
	}

	@Test
	public void testInvalidAction() {
		assertThrows(IllegalArgumentException.class, ()  -> consumer.process(exchange, "foo bar body 'string, integer'"));
	}

	@Test
	public void testInvalidSource() {
		assertThrows(IllegalArgumentException.class, ()  -> consumer.process(exchange, "foo map xyz 'string, integer'"));
	}


	@Configuration
	public static class ContextConfig extends XmlDslCamelTestContextConfiguration {
	}
}

