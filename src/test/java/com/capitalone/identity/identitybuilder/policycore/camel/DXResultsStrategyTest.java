package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DXResultsStrategyTest {

	private Exchange exchange;
	private DXResultsStrategy strategy;
	
	// DX Results: name, status, body
	private static String[][] TEST_DATA = {
			{ "foo",    "300", "foo-body" },
			{ "bar",    "200", "bar-body" },
			{ "foobar", "400", "foobar-body" }
	};

	@BeforeEach
	public void setUp() throws Exception {
		strategy = new DXResultsStrategy();
		exchange = new DefaultExchange(new DefaultCamelContext());
		exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYSTATE, new HashMap<String, Object>());
	}

	/**
	 * Make sure the single argument aggregate method is just a synonym for
	 * aggregate(null, exchange).
	 */
	@Test
	public void testAggregateSingleArgument() {
		Exchange[] argsFound = { new DefaultExchange(new DefaultCamelContext()), null };
		strategy = new DXResultsStrategy() {
			@Override
			public Exchange aggregate(Exchange ignored, Exchange current) {
				argsFound[0] = ignored;
				argsFound[1] = current;
				return current;
			}
		};
		strategy.aggregate(exchange);
		assertNull(argsFound[0]);
		assertSame(exchange, argsFound[1]);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStrategy() {
		assertNull(getDXResults(exchange));
		
		for (String[] entry : TEST_DATA) {
			exchange.getIn().setHeader(PolicyConstants.HEADER_DXRESULTNAME, entry[0]);
			exchange.getIn().setHeader(PolicyConstants.HEADER_HTTPSTATUS, entry[1]);
			exchange.getIn().setBody(entry[2], String.class);	
			
			assertNull(strategy.aggregate(null, exchange));
			Map<String, Object> results = getDXResults(exchange);
			Map<String, Object> dxResult = (Map<String, Object>) results.get(entry[0]);
			assertNotNull(dxResult);
			assertEquals(entry[1], (String) dxResult.get("httpStatus"));
			assertEquals(entry[2], (String) dxResult.get("result"));
		}
		
		assertEquals(TEST_DATA.length, getDXResults(exchange).size());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getDXResults(Exchange exchange) {
		return (Map<String, Object>) exchange.getIn().getHeader(PolicyConstants.HEADER_POLICYSTATE, Map.class).get(PolicyConstants.STATE_DXRESULTS);
	}
}
