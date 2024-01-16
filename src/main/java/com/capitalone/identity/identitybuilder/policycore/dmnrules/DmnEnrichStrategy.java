package com.capitalone.identity.identitybuilder.policycore.dmnrules;

import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;


@Component
public class DmnEnrichStrategy implements AggregationStrategy {
	@Override
	public Exchange aggregate(Exchange original, Exchange resource) {
		if (original == null) {
			throw new IllegalStateException("Cannot use this enrichment strategy as an aggregation strategy.");
		}

		original.getIn().setBody(resource.getIn().getBody());

		if (resource.getIn().getHeader(ApplicationConstants.POLICY_RULE_RESULT) != null) {
			original.getIn().getHeader(PolicyConstants.POLICY_RULE_RESULTS, ArrayList.class)
					.add(resource.getIn().getHeader(ApplicationConstants.POLICY_RULE_RESULT));
		}

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
