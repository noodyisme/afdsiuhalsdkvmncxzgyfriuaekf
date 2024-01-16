package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A custom Camel aggregation strategy that aggregates both the message bodies and thrown exceptions separately into a Map in the following format:
 * {"results": List, "exceptions": List}
 * <p>
 * Example usage: <code>&lt;split strategyRef="ResultsAndExceptionsMapStrategy" parallelProcessing="true" stopOnException="false"&gt;</code>
 * <p>
 *
 * @author mzr480
 */
@Component
public class ResultsAndExceptionsMapStrategy implements AggregationStrategy {
    private static final String RESULTS_KEY_STRING = "results";
    private static final String EXCEPTIONS_KEY_STRING = "exceptions";
    @SuppressWarnings("unchecked")
    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {
        Object entry = resource.getIn().getBody();
        Exception entryException = resource.getException();
        if(original == null){
            // This is the first entry, we need to build our results and exceptions map
            Map<String, List<Object>> aggregateMap = new HashMap<>();
            aggregateMap.put(RESULTS_KEY_STRING, new ArrayList<>());
            aggregateMap.put(EXCEPTIONS_KEY_STRING, new ArrayList<>());
            // Wrap the body in this Map structure
            resource.getIn().setBody(aggregateMap, Map.class);
            // Add this to either the results or exceptions part of the map
            if(entryException == null){
                ((List) resource.getIn().getBody(Map.class).get(RESULTS_KEY_STRING)).add(entry);
            } else{
                ((List) resource.getIn().getBody(Map.class).get(EXCEPTIONS_KEY_STRING)).add(entryException);
                // The exception has been stored in the map, so it can be removed from the current exchange
                resource.setException(null);
            }
            return resource;
        } else {
            // This is not the first entry, add this to either the results or exceptions part of the map
            if(entryException == null){
                ((List) original.getIn().getBody(Map.class).get(RESULTS_KEY_STRING)).add(entry);
            } else{
                ((List) original.getIn().getBody(Map.class).get(EXCEPTIONS_KEY_STRING)).add(entryException);
            }
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