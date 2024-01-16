package com.capitalone.identity.identitybuilder.policycore.policymode;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultProducer;
import org.apache.commons.lang3.StringUtils;

import static com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants.HEADER_MOCKMODE;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.MOCK_MODE_CONFIG;

public class PolicyModeProducer extends DefaultProducer {
    private final boolean mockModeEnabled;
    public PolicyModeProducer(Endpoint endpoint, boolean mockModeEnabled) {
        super(endpoint);
        this.mockModeEnabled = mockModeEnabled;
    }

    @Override
    public void process(Exchange exchange) {
        // Set property for operational logging of current configuration
        exchange.setProperty(MOCK_MODE_CONFIG, mockModeEnabled);

        Message message = exchange.getMessage();
        if(StringUtils.isBlank(message.getHeader(HEADER_MOCKMODE,String.class))) {
            message.setHeader(HEADER_MOCKMODE, mockModeEnabled);
        }
    }

    public boolean isMockModeEnabled() {
        return mockModeEnabled;
    }
}
