package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class BodyEnrichStrategyTest  {

	@Autowired
	private CamelContext camelContext;
	
	private static final String ORIGINAL_DATA = "originalData";
	private static final String ENRICHED_DATA = "enrichedData";
	
	private Exchange originalExchange;
	private Exchange enrichmentExchange;
	
	@BeforeEach
	public void setup() {
		CamelContext context = new DefaultCamelContext();
		
		originalExchange = new DefaultExchange(context);
		originalExchange.getIn().setBody(ORIGINAL_DATA, String.class);
		originalExchange.getIn().setHeaders(makeHeaders());
		
		enrichmentExchange = new DefaultExchange(context);
		enrichmentExchange.getIn().setBody(ENRICHED_DATA, String.class);
		enrichmentExchange.getIn().setHeaders(makeHeaders());
	}

	@Test
	public void testAsAggregationStrategy() {
		assertThrows(IllegalStateException.class, () -> new BodyEnrichStrategy().aggregate(null, originalExchange));
	}
	
	@Test
	public void testAsEnrichStrategy() {
		enrichmentExchange.getIn().setHeader("a", "other-value");
				
		assertSame(originalExchange, new BodyEnrichStrategy().aggregate(originalExchange, enrichmentExchange));
		assertSame(ENRICHED_DATA, originalExchange.getIn().getBody());
		assertEquals(makeHeaders(), originalExchange.getIn().getHeaders());
	}
	
	/**
	 * Make a "standard" set of headers for comparison.
	 * 
	 * @return a map of header data
	 */
	private Map<String, Object> makeHeaders() {
		Map<String, Object> map = new HashMap<>();
		map.put("a", "a-value");
		map.put("b", "b-value");
		return map;
	}

}
