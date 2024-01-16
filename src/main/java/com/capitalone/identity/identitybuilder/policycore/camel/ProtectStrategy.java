package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

/**
 * A custom Camel aggregation strategy that protects the caller's exchange.
 * <p>
 * If we need to invoke a route that modifies the exchange as a side effect but
 * want the current exchange to remain unmodified, we can use this strategy to
 * protect the caller's exchange. 
 * <p>
 * <pre>.enrich("direct:foo", new ProtectStrategy())</pre>
 * 
 * @author oqu271
 */
@Component
public class ProtectStrategy implements AggregationStrategy {
	@Override
	public Exchange aggregate(Exchange original, Exchange resource) {
		return original;
	}
}

/*
 * Copyright 2018 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */