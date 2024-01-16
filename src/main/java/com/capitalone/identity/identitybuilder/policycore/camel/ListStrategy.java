package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A custom Camel aggregation strategy that aggregates the message bodies
 * into a list in the result body.
 * <p>
 * Example usage: <code>&lt;split strategyRef="listStrategy" parallelProcessing="true"&gt;</code>
 * <p>
 *
 * @author oqu271
 */
@Component
public class ListStrategy implements AggregationStrategy {
	@SuppressWarnings("unchecked")
	@Override
	public Exchange aggregate(Exchange original, Exchange resource) {
		Object entry = resource.getIn().getBody();
		if (original == null) {
			// This is the first response, so wrap the body in a list.
			resource.getIn().setBody(new ArrayList<Object>(Arrays.asList(entry)));
			return resource;
		} else {
			// For later responses, add the response body to the original list.
			original.getIn().getBody(List.class).add(entry);
			return original;
		}
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