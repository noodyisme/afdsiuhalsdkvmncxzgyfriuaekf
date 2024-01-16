package com.capitalone.identity.identitybuilder.policycore.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class APIRequestTest {

    @Test
    public void testAllArgConstructor() {
        String apiKey = "apiKey";
        String clientApiKey = "clientApiKey";
        String clientCorrelationId = "clientCorrelationId";
        String customerIpAddress = "customerIpAddress";
        String contentType = "contentType";
        String channelType = "channelType";
        String countryCode = "countryCode";
        String language = "language";
        String domain = "domain";
        String subDomain = "subDomain";
        String userAgent = "userAgent";
        String accept = "accept";
        String clientId = "clientId";
        String businessEvent = "businessEvent";
        String xJourneyPolicy = "xJourneyPolicy";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String processId = "processId";
        String step = "step";
        Boolean mockMode = false;
        APIRequest apiRequest = new APIRequest(apiKey, clientApiKey, clientCorrelationId, customerIpAddress,
                contentType, channelType, countryCode, language, domain, subDomain, userAgent, accept, clientId,
                businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, policyName, policyVersion, processId, step,
                mockMode);
        assertSame(apiKey, apiRequest.getApiKey());
        assertSame(clientApiKey, apiRequest.getClientApiKey());
        assertEquals(clientCorrelationId, apiRequest.getClientCorrelationId());
        assertEquals(customerIpAddress, apiRequest.getCustomerIpAddress());
        assertEquals(contentType, apiRequest.getContentType());
        assertEquals(channelType, apiRequest.getChannelType());
        assertEquals(language, apiRequest.getLanguage());
        assertEquals(domain, apiRequest.getDomain());
        assertEquals(subDomain, apiRequest.getSubDomain());
        assertEquals(userAgent, apiRequest.getUserAgent());
        assertEquals(accept, apiRequest.getAccept());
        assertEquals(clientId, apiRequest.getClientId());
        assertEquals(businessEvent, apiRequest.getBusinessEvent());
        assertEquals(xJourneyPolicy, apiRequest.getXJourneyPolicy());
        assertEquals(policyName, apiRequest.getPolicyName());
        assertEquals(policyVersion, apiRequest.getPolicyVersion());
        assertEquals(processId, apiRequest.getProcessId());
        assertEquals(step, apiRequest.getStep());
        assertEquals(mockMode, apiRequest.getMockModeFlag());
    }
}