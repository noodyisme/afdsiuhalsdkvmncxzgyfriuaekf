package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom Camel aggregation strategy that collects the results of
 * DevExchange API call responses.
 * 
 * @author oqu271
 */
@Component
public class DXResultsStrategy implements AggregationStrategy {
	
	/**
	 * An aggregation helper method easily accessible from a bean invocation.
	 * <p>
	 * Example:
	 * <pre>
	 *     &lt;bean ref="DXResultsStrategy" method="aggregate"/&gt;
	 * </pre>
	 * 
	 * @param exchange an exchange containing a DevExchange result
	 */
	public void aggregate(Exchange exchange) {
		aggregate(null, exchange);
	}

	/**
	 * Accumulates the result of a DevExchange call in a map in the policy state.
	 * <p>
	 * The current exchange should contain the HTTP status in the <code>httpStatus</code>
	 * header and the HTTP result body in the exchange body.
	 * 
	 * @param  ignored the previous exchange for this aggregation (may be <code>null<code>)
	 * @param  current the current exchange with the DX result
	 * @return <code>null</code> to avoid modifying the multicast result
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Exchange aggregate(Exchange ignored, Exchange current) {
		Map<String, Object> policyState = (Map<String, Object>) current.getIn().getHeader(PolicyConstants.HEADER_POLICYSTATE, Map.class);
		Map<String, Object> dxResults = (Map<String, Object>) policyState.get(PolicyConstants.STATE_DXRESULTS);
		if (dxResults == null) {
			dxResults = new HashMap<>();
			policyState.put(PolicyConstants.STATE_DXRESULTS, dxResults);
		}
		HashMap<String, Object> map = new HashMap<>();
		map.put("httpStatus", current.getIn().getHeader(PolicyConstants.HEADER_HTTPSTATUS, String.class));
		map.put("result", current.getIn().getBody());
		dxResults.put(current.getIn().getHeader(PolicyConstants.HEADER_DXRESULTNAME, String.class), map);		
		return null;
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