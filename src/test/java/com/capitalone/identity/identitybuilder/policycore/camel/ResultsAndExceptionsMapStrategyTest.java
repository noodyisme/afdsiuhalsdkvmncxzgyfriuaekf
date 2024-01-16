package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@ExtendWith(MockitoExtension.class)
public class ResultsAndExceptionsMapStrategyTest {
    private static final String resultsKeyString = "results";
    private static final String exceptionsKeyString = "exceptions";
    private static final String firstResultsBody = "firstResultsBody";
    private static final String secondResultsBody = "secondResultsBody";
    private static final Exception firstException = new java.lang.Exception("firstException");
    private static final Exception secondException = new java.lang.Exception("secondException");
    private static final CamelContext context = new DefaultCamelContext();
    private static final Exchange secondExceptionExchange = new DefaultExchange(context);
    private static final Exchange secondResultsExchange = new DefaultExchange(context);
    private ResultsAndExceptionsMapStrategy aggregator;

    @BeforeEach
    void init() {
        // Initialize the aggregator being tested
        aggregator = new ResultsAndExceptionsMapStrategy();

        // Initialize an exchange body and a results body that will be the same for both tests
        secondExceptionExchange.setException(secondException);
        secondResultsExchange.getIn().setBody(secondResultsBody, String.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testStrategyAggregateResultsBodyFirst() {
        //Initialize our inputs
        Exchange actualAggregatedExchange = new DefaultExchange(context);
        actualAggregatedExchange.getIn().setBody(firstResultsBody, String.class);
        Exchange firstExceptionExchange = new DefaultExchange(context);
        firstExceptionExchange.setException(firstException);

        // Initialize our expected output
        Map <String, List<Object>> expectedOutputMap = new HashMap<>();
        List<Object> expectedOutputResultsList = new ArrayList<>();
        List<Object> expectedOutputExceptionsList = new ArrayList<>();
        expectedOutputResultsList.add(firstResultsBody);
        expectedOutputResultsList.add(secondResultsBody);
        expectedOutputExceptionsList.add(firstException);
        expectedOutputExceptionsList.add(secondException);
        expectedOutputMap.put(resultsKeyString, expectedOutputResultsList);
        expectedOutputMap.put(exceptionsKeyString, expectedOutputExceptionsList);

        // Aggregate the first results, then the first exception, then the second results, then the second exception
        aggregator.aggregate(null, actualAggregatedExchange);
        aggregator.aggregate(actualAggregatedExchange, firstExceptionExchange);
        aggregator.aggregate(actualAggregatedExchange, secondResultsExchange);
        aggregator.aggregate(actualAggregatedExchange, secondExceptionExchange);

        // Assert our expected output against our actual output
        assertEquals(expectedOutputMap, actualAggregatedExchange.getIn().getBody(Map.class));

        // Assert that there are no residual exceptions in this Exchange
        assertNull(actualAggregatedExchange.getException());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testStrategyAggregateExceptionFirst(){
        //Initialize our inputs
        Exchange actualAggregatedExchange = new DefaultExchange(context);
        actualAggregatedExchange.setException(firstException);
        Exchange firstResultsExchange = new DefaultExchange(context);
        firstResultsExchange.getIn().setBody(firstResultsBody, String.class);

        // Initialize our expected output
        Map<String, List<Object>> expectedOutputMap = new HashMap<>();
        List<Object> expectedOutputResultsList = new ArrayList<>();
        List<Object> expectedOutputExceptionsList = new ArrayList<>();
        expectedOutputResultsList.add(firstResultsBody);
        expectedOutputExceptionsList.add(firstException);
        expectedOutputMap.put(resultsKeyString, expectedOutputResultsList);
        expectedOutputMap.put(exceptionsKeyString, expectedOutputExceptionsList);

        // Aggregate the first exception, then the first results, no need to check additional aggregations because they are covered in the other test
        aggregator.aggregate(null, actualAggregatedExchange);
        aggregator.aggregate(actualAggregatedExchange, firstResultsExchange);

        // Assert our expected output against our actual output
        assertEquals(expectedOutputMap, actualAggregatedExchange.getIn().getBody(Map.class));

        // Assert that there are no residual exceptions in this Exchange
        assertNull(actualAggregatedExchange.getException());
    }
}
