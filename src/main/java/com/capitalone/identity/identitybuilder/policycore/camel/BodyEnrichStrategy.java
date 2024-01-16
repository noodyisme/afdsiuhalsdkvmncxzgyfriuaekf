package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

/**
 * A custom Camel enrichment aggregation strategy that updates the
 * body of the message but preserves the headers.
 * <p>
 * Example usage: <code>&lt;enrich strategyRef="bodyEnrichStrategy"&gt;</code>
 * <p>
 *
 * @author oqu271
 */
@Component
public class BodyEnrichStrategy implements AggregationStrategy {
	@Override
	public Exchange aggregate(Exchange original, Exchange resource) {
		if (original == null) {
			throw new IllegalStateException("Cannot use this enrichment strategy as an aggregation strategy.");
		}
		
		original.getIn().setBody(resource.getIn().getBody());
		return original;
	}
}
	
/*
 * Copyright 2020 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */