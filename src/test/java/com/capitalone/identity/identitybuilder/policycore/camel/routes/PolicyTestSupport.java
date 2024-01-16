package com.capitalone.identity.identitybuilder.policycore.camel.routes;

import com.capitalone.identity.identitybuilder.policycore.camel.BodyEnrichStrategy;
import com.capitalone.identity.identitybuilder.policycore.camel.DXResultsStrategy;
import com.capitalone.identity.identitybuilder.policycore.camel.ListStrategy;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.util.HeaderUtil;
import com.capitalone.identity.identitybuilder.policycore.camel.util.ISO8601DateFormatter;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.DmnEnrichStrategy;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.*;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultExchange;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Deprecated
public abstract class PolicyTestSupport  {
	
	/**
	 * Overridden by derived classes to inject CamelContext into base PolicyTestSupport class.
	 * Classes that do not require Camel Context should return null instead
	 * @return  CamelContext, instance of {@link CamelContext}
	 */
	protected abstract CamelContext camelContext();

	/**
	 * Define the beans that we need for this test, both real and mocked.
	 */
	protected Registry createRegistry() throws Exception {
		Registry registry = this.camelContext().getRegistry();
		registry.bind("DXResultsStrategy", new DXResultsStrategy());
		registry.bind("bodyEnrichStrategy", new BodyEnrichStrategy());
		registry.bind("dmnEnrichStrategy", new DmnEnrichStrategy());
		registry.bind("listStrategy", new ListStrategy());
		registry.bind("headerUtil", new HeaderUtil());
		registry.bind("ISO8601DateFormatter", new ISO8601DateFormatter());
		return registry;
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
	 * @param  json the JSON string to parse
	 * @return a map representing the nested JSON data structure
	 * @throws IOException if an error occurs
	 */
	protected Map<String, Object> jsonToMap(String json) throws IOException {
		return new ObjectMapper().readValue(json, new TypeReference<TreeMap<String, Object>>() {
		});
	}

	
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
	protected Exchange policyExchange(Map<String, Object> params, Map<String, Object> state) {
		params = (params == null) ? new HashMap<>() : params;
		state  = (state == null)  ? new HashMap<>() : state;

		Map<String, Object> dxHeaders = new TreeMap<String, Object>();
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
	protected Map<String, Object> buildModularResult(int httpStatus, boolean rulePassed, Map<String, Object> ruleOutput) {
		Map<String, Object> map = new HashMap<>();
		map.put("httpStatus", httpStatus);
		map.put("rulePassed", httpStatus < 300 && rulePassed);
		if (httpStatus < 300 && rulePassed && ruleOutput != null) {
			map.put("ruleOutput", ruleOutput);
		}
		return map;
	}
	
	/**
	 * Returns the exchange's IN body as a <code>Map&lt;String, Objeect%gt;<code>.
	 *
	 * @param exchange the exchange to examine
	 * @return the body viewed as a <code>Map</code>
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> getBodyAsMap(Exchange exchange) {
		return exchange.getIn().getBody(Map.class);
	}

	/**
	 * Extracts an entry from an exchange's policy state as a <i>Map</i>.
	 * This convenience method reduces casting noise for callers.
	 * 
	 * @param  exchange the exchange to use
	 * @param  name the name of the state entry
	 * @return the entry from the exchange's policy state
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Serializable> getStateEntry(Exchange exchange, String name) {
		return (Map<String, Serializable>) getState(exchange).get(name);
	}
	
	/**
	 * Extracts an entry from an exchange's policy state.
	 * This convenience method reduces casting noise for callers.
	 * 
	 * @param  exchange the exchange to use
	 * @param  name the name of the state entry
	 * @return the entry from the exchange's policy state
	 */
	protected <T> T getStateEntry(Exchange exchange, String name, Class<T> type) {
		return type.cast(getState(exchange).get(name));
	}
	
	/**
	 * Extracts the policy state map from the given exchange.
	 * This convenience method reduces casting noise for callers.
	 * 
	 * @param  exchange the exchange to use
	 * @return the policy state map from the exchange
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Serializable> getState(Exchange exchange) {
		return (Map<String, Serializable>) exchange.getIn().getHeader(PolicyConstants.HEADER_POLICYSTATE);
	}

	
	@Consume("direct:breakpoint")
	public void breakpoint(Exchange exchange, @Body Object body, @Headers Object headers) {
		System.getProperty("foo");
	}
}
