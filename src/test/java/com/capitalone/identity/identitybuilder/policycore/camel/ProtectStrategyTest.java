package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
public class ProtectStrategyTest {
	
	@Test
	public void testStrategy() {
		CamelContext context = new DefaultCamelContext();
		Exchange original = new DefaultExchange(context);
		String originalBody = "some original body data to not modify";
		original.getIn().setBody(originalBody, String.class);
		Map<String, Object> originalHeadersCopy = new HashMap<>(original.getIn().getHeaders());

		Exchange changed = new DefaultExchange(context);
		String changedBody = "body data to be ignored";
		changed.getIn().setBody(changedBody, String.class);
		
		assertSame(original, new ProtectStrategy().aggregate(original, changed));
		assertEquals(originalHeadersCopy, original.getIn().getHeaders());
		assertSame(originalBody, original.getIn().getBody(String.class));
	}
}
