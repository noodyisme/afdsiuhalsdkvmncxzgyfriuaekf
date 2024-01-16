package com.capitalone.identity.identitybuilder.policycore.camel.routes;

import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public interface PolicyTestSupportPolicyExchange {

    /**
     * Overridden by derived classes to inject CamelContext into base PolicyTestSupport class.
     * Classes that do not require Camel Context should return null instead
     *
     * @return CamelContext, instance of {@link CamelContext}
     */
    // TODO : Refactor
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
     * @param state  the policy state data to use (may be empty or <code>null</code>)
     */
    default Exchange policyExchange(Map<String, Object> params, Map<String, Object> state) {
        params = (params == null) ? new HashMap<>() : params;
        state = (state == null) ? new HashMap<>() : state;

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
        exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYAUTHOR_SUPPRESS_DEVTEXT, "true");

        return exchange;
    }
}
