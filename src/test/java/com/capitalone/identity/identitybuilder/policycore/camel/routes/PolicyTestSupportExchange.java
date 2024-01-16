package com.capitalone.identity.identitybuilder.policycore.camel.routes;

import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.exception.AttributeValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public interface PolicyTestSupportExchange {

    /**
     * Overridden by derived classes to inject CamelContext into base PolicyTestSupport class.
     * Classes that do not require Camel Context should return null instead
     * @return  CamelContext, instance of {@link CamelContext}
     */
    CamelContext camelContext();

    /**
     * Return a new exchange for a policy request.
     * <ul>
     * <li>The standard policy headers are set with test values</li>
     * <li>The request body is a <code><i>Map</i></code> containing the
     *     request parameters</li>
     * <li>Policy state default values may all be overridden</li>
     * </ul>
     *
     * @param params the request parameters (may be empty or <code>null</code>)
     * @param state the policy state data to use (may be empty or <code>null</code>)
     */
	default Exchange policyExchange(Map<String, Object> params, Map<String, Object> state) {
		params = (params == null) ? new HashMap<>() : params;
		state  = (state == null)  ? new HashMap<>() : state;

		Map<String, Object> dxHeaders = new TreeMap<>();
		dxHeaders.put(ApplicationConstants.API_KEY, "Api-Key-value");
		dxHeaders.put(ApplicationConstants.CHANNEL_TYPE, "WEB");
		dxHeaders.put(ApplicationConstants.CLIENT_API_KEY, "Client-Api-Key-value");
		dxHeaders.put(ApplicationConstants.CLIENT_CORRELATION_ID, "Client-Correlation-ID-value");
		dxHeaders.put(ApplicationConstants.CUSTOMER_IP_ADDR, "Customer-IP-value");

		Exchange exchange = new DefaultExchange(camelContext());
		exchange.getIn().setBody(params);
		exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYSTATE, state);
		exchange.getIn().setHeader(PolicyConstants.HEADER_DXHEADERS, dxHeaders);
		exchange.getIn().setHeader(PolicyConstants.HEADER_CLIENTID, "clientId-value");

		return exchange;
	}

    @NotNull
    default Map<String, Object> createDxHeaders() {
        Map<String, Object> dxHeaders = new TreeMap<String, Object>();
        dxHeaders.put(ApplicationConstants.API_KEY, "Api-Key-value");
        dxHeaders.put(ApplicationConstants.CHANNEL_TYPE, "WEB");
        dxHeaders.put(ApplicationConstants.CLIENT_API_KEY, "Client-Api-Key-value");
        dxHeaders.put(ApplicationConstants.CLIENT_CORRELATION_ID, "Client-Correlation-ID-value");
        dxHeaders.put(ApplicationConstants.CUSTOMER_IP_ADDR, "Customer-IP-value");
        return dxHeaders;
    }

    /**
     * Validates that the exchange encountered an exception due to an invalid parameter.
     *
     * @param exchange        the exchange to test
     * @param expectedDevText the expected developer's text
     * @return always returns <code>true</code> so simple tests can assert on the result and
     * avoid SonarQube complaints
     */
    default boolean checkInvalidAttribute(Exchange exchange, String expectedDevText) {
        assertNotNull(exchange.getException());
        RequestValidationException e = exchange.getException(RequestValidationException.class);
        assertEquals("INVALID_ATTRIBUTE_DEV_TEXT", e.getApiError().getDeveloperTextId());
        assertEquals(expectedDevText, e.getApiError().getDeveloperTextMessageParms().toString());
        assertEquals("201216", e.getApiError().getId());
        assertTrue(e.getCause() instanceof AttributeValidationException);
        return true;
    }

     /**
     * Returns a modular route response body (useful for testing policy routes).
     * <p>
     * For consistency and convenience, if the status is an error (&gt;= 300)
     * then <code>rulePassed<code> is forced to <code>false</code> and
     * <code>ruleOutput<code> is ignored.
     * <p>
     * Similarly, if <code>rulePassed<code> is false, <code>ruleOutput<code> is ignored.
     *
     * @param httpStatus the status for the result
     * @param rulePassed whether or not the rule passed
     * @param ruleOutput the output of the rule (or <code>null</code> if none)
     * @return A map of the JSON structure for the modular route response body
     */
        default Map<String, Object> buildModularResult(int httpStatus, boolean rulePassed, Map<String, Object> ruleOutput) {
        Map<String, Object> map = new HashMap<>();
        map.put("httpStatus", httpStatus);
        map.put("rulePassed", httpStatus < 300 && rulePassed);
        if (httpStatus < 300 && rulePassed && ruleOutput != null) {
            map.put("ruleOutput", ruleOutput);
        }
        return map;
    }

    /**
     * Extracts an entry from an exchange's policy state as a <i>Map</i>.
     * This convenience method reduces casting noise for callers.
     *
     * @param exchange the exchange to use
     * @param name     the name of the state entry
     * @return the entry from the exchange's policy state
     */
    @SuppressWarnings("unchecked")
    default Map<String, Serializable> getStateEntry(Exchange exchange, String name) {
        return (Map<String, Serializable>) getState(exchange).get(name);
    }

    /**
     * Extracts an entry from an exchange's policy state.
     * This convenience method reduces casting noise for callers.
     *
     * @param exchange the exchange to use
     * @param name     the name of the state entry
     * @return the entry from the exchange's policy state
     */
    default <T> T getStateEntry(Exchange exchange, String name, Class<T> type) {
        return type.cast(getState(exchange).get(name));
    }

    default Map<String, Serializable> getState(Exchange exchange) {
        return (Map<String, Serializable>) exchange.getIn().getHeader(PolicyConstants.HEADER_POLICYSTATE);
    }

    /**
     * Parses a JSON string into a <i>Map</i>.
     * <p>
     * This is used to make assertion comparisons independent of JSON formatting.
     * JSON string comparisons depend on the order of the attributes at each level
     * and how whitespace is used (e.g., pretty-printing).  Converting JSON to maps
     * allows for a more robust assertion.
     * <pre>
     *      assertEquals(jsonToMap(expectedJSON), jsontoMap(foundJSON));
     * </pre>
     *
     * @param json the JSON string to parse
     * @return a map representing the nested JSON data structure
     * @throws IOException if an error occurs
     */
    default Map<String, Object> jsonToMap(String json) throws IOException {
        return new ObjectMapper().readValue(json, new TypeReference<TreeMap<String, Object>>() {
        });
    }

    /**
     * Returns the exchange's IN body as a <code>Map&lt;String, Objeect%gt;<code>.
     *
     * @param exchange the exchange to examine
     * @return the body viewed as a <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    default Map<String, Object> getBodyAsMap(Exchange exchange) {
        return exchange.getIn().getBody(Map.class);
    }


}
