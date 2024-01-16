package com.capitalone.identity.identitybuilder.policycore.camel.util;

import org.apache.camel.Body;
import org.apache.camel.Consume;
import org.apache.camel.Exchange;
import org.apache.camel.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility routes that are useful for setting breakpoints on. 
 * 
 * @author oqu271
 */
@Component
public class BreakpointRoutes {
		
	/**
	 * A Camel route that can have a breakpoint set on it.
	 * 
	 * @param  exchange the current message exchange
	 * @param  body the input message body
	 * @param  headers the input message headers
	 */
	@Consume(value = "direct:breakpoint1")
	public void breakpoint1(Exchange exchange, @Body Object body, @Headers Map<String, Object> headers) {
		System.getProperty("foo");
	}
	/**
	 * A Camel route that can have a breakpoint set on it.
	 * 
	 * @param  exchange the current message exchange
	 * @param  body the input message body
	 * @param  headers the input message headers
	 */
	@Consume(value = "direct:breakpoint2")
	public void breakpoint2(Exchange exchange, @Body Object body, @Headers Map<String, Object> headers) {
		System.getProperty("foo");
	}
	/**
	 * A Camel route that can have a breakpoint set on it.
	 * 
	 * @param  exchange the current message exchange
	 * @param  body the input message body
	 * @param  headers the input message headers
	 */
	@Consume(value = "direct:breakpoint3")
	public void breakpoint3(Exchange exchange, @Body Object body, @Headers Map<String, Object> headers) {
		System.getProperty("foo");
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