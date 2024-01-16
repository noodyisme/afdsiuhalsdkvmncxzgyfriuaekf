package com.capitalone.identity.identitybuilder.policycore.policymode;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants.HEADER_MOCKMODE;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.MOCK_MODE_CONFIG;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyModeProducerTest {
    @Mock
    PolicyModeEndpoint policyModeEndpoint;

    Exchange ex;

    @BeforeEach
    void  setup() {
        ex = new DefaultExchange(new DefaultCamelContext());
    }

    @Test
    void mockModeEnabledTest() {
        PolicyModeProducer policyModeProducer = new PolicyModeProducer(policyModeEndpoint, true);

        policyModeProducer.process(ex);

        assertTrue(ex.getProperty(MOCK_MODE_CONFIG, Boolean.class));
        assertTrue(ex.getMessage().getHeader(HEADER_MOCKMODE, Boolean.class));
    }

    @Test
    void mockModeDisabledTest() {
        PolicyModeProducer policyModeProducer = new PolicyModeProducer(policyModeEndpoint, false);

        policyModeProducer.process(ex);

        assertFalse(ex.getProperty(MOCK_MODE_CONFIG, Boolean.class));
        assertFalse(ex.getMessage().getHeader(HEADER_MOCKMODE, Boolean.class));
    }

    @Test
    void mockModeHeaderEnabledTest() {
        PolicyModeProducer policyModeProducer = new PolicyModeProducer(policyModeEndpoint, false);
        ex.getMessage().setHeader(HEADER_MOCKMODE, "true");

        policyModeProducer.process(ex);

        assertFalse(ex.getProperty(MOCK_MODE_CONFIG, Boolean.class));
        assertTrue(ex.getMessage().getHeader(HEADER_MOCKMODE, Boolean.class));
    }

    @Test
    void mockModeHeaderDisabledTest() {
        PolicyModeProducer policyModeProducer = new PolicyModeProducer(policyModeEndpoint, true);
        ex.getMessage().setHeader(HEADER_MOCKMODE, "false");

        policyModeProducer.process(ex);

        assertTrue(ex.getProperty(MOCK_MODE_CONFIG, Boolean.class));
        assertFalse(ex.getMessage().getHeader(HEADER_MOCKMODE, Boolean.class));
    }
}
