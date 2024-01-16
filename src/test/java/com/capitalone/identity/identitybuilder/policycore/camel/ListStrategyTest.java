package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
public class ListStrategyTest extends CamelTestSupport {
	
	private static final String originalBody = "originalBody";
	private static final String secondBody = "secondBody";

	@Test
	@SuppressWarnings("unchecked")
	public void testStrategy() {
		CamelContext context = new DefaultCamelContext();
		Exchange firstExchange = new DefaultExchange(context);
		firstExchange.getIn().setBody(originalBody, String.class);

		Exchange secondExchange = new DefaultExchange(context);
		secondExchange.getIn().setBody(secondBody, String.class);
		
		// The first invocation should wrap the body in a list.
		assertSame(firstExchange, new ListStrategy().aggregate(null, firstExchange));
		List<Object> list = firstExchange.getIn().getBody(List.class);
		assertSame(originalBody, list.get(0));
				
		// A subsequent invocation should add the body to the same list.
		assertSame(firstExchange, new ListStrategy().aggregate(firstExchange, secondExchange));
		assertSame(list, firstExchange.getIn().getBody());
		assertSame(originalBody, list.get(0));
		assertSame(secondBody, list.get(1));
	}
}
