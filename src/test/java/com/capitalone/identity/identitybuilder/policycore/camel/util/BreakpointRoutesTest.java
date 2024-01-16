package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.identity.identitybuilder.policycore.camel.routes.PolicyTestSupportPolicyExchange;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import com.capitalone.identity.identitybuilder.policycore.utils.XmlDslCamelTestContextConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;

@CamelSpringBootContextAwareTest(contextConfigClasses = BreakpointRoutesTest.ContextConfig.class)
public class BreakpointRoutesTest implements PolicyTestSupportPolicyExchange {

	@Autowired
	CamelContext camelContext;
	
	@InjectMocks
	private BreakpointRoutes util;
	
	private Exchange exchange;

	@Override
	public CamelContext camelContext() {
		return this.camelContext;
	}

	/**
	 * Return the list of route files to load for testing.
	 */
	protected Collection<String> routeFiles() {
		return Collections.emptyList();
	}

	@BeforeEach
	public void setup() {
		exchange = policyExchange(null, null);
	}

	/**
	 * These tests are just to exercise the code to make sure we have
	 * code coverage.  They cannot fail and do not throw an exception so
	 * there is nothing to test.
	 */
	@Test
	public void testBreakpointRoutes() {
		Assertions.assertDoesNotThrow(() -> {
		util.breakpoint1(exchange, 
			exchange.getIn().getBody(),
			exchange.getIn().getHeaders());
		
		util.breakpoint2(exchange, 
				exchange.getIn().getBody(),
				exchange.getIn().getHeaders());
		
		util.breakpoint3(exchange, 
				exchange.getIn().getBody(),
				exchange.getIn().getHeaders());
		});
	}

	@Configuration
	public static class ContextConfig extends XmlDslCamelTestContextConfiguration {
	}
}