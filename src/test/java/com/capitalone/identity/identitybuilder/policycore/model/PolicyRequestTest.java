package com.capitalone.identity.identitybuilder.policycore.model;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class PolicyRequestTest {

    @Test
    public void testNonPolicyNameConstructor() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String xJourneyPolicy = "xJourneyPolicy";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        String clientId = "clientId";
        String processId = "processId";
        String apiKey = "apiKey";
        Boolean mockMode = false;
        APIRequest apiRequest = new APIRequest(apiKey, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, policyName,
                policyVersion, processId, step, mockMode);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, systemParams);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(xJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
        assertEquals(apiKey, policyRequest.getDxHeaders().get(CLIENT_API_KEY));
        assertEquals(apiKey, policyRequest.getDxHeaders().get(API_KEY));
        assertEquals(mockMode, policyRequest.getMockModeFlag());
    }

    @Test
    public void testNotNullXJourneyPolicy() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String clientId = "clientId";
        String processId = "processId";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";

        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, null, customerIdToken, sessionCorrelationId, policyName,
                policyVersion, processId, step, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, systemParams);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertNotNull(policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
    }

    @Test
    public void testNullSessionCorrelationId() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String xJourneyPolicy = "xJourneyPolicy";
        String businessEvent = "businessEvent";
        String clientId = "clientId";
        String processId = "processId";
        String customerIdToken = "customerIdToken";

        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, xJourneyPolicy, customerIdToken,null, policyName,
                policyVersion, processId, step, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, systemParams);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertNull(apiRequest.getSessionCorrelationId());
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
    }

    @Test
    public void testMBTwoXJourneyPolicy() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String clientId = "clientId";
        String processId = "processId";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        String expectedXJourneyPolicy = policyName + ":" + policyVersion + ":" + processId;
        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, null, customerIdToken, sessionCorrelationId, policyName,
                policyVersion, processId, step, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "masterbuilder-2.0", null);
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, systemParams);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(expectedXJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(expectedXJourneyPolicy, policyRequest.getDxHeaders().get(X_JOURNEY_POLICY));
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
    }

    @Test
    public void testNullProcessId() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String xJourneyPolicy = "xJourneyPolicy";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";

        String clientId = "clientId";
        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, policyName,
                policyVersion, null, step, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, systemParams);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(xJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertNotNull(policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
    }

    @Test
    public void testClientApiKey() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String xJourneyPolicy = "xJourneyPolicy";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        String clientId = "clientId";
        String processId = "processId";
        String apiKey = "apiKey";
        String clientApiKey = "clientApiKey";
        APIRequest apiRequest = new APIRequest(apiKey, clientApiKey, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, policyName,
                policyVersion, processId, step, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, systemParams);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(xJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
        assertEquals(clientApiKey, policyRequest.getDxHeaders().get(CLIENT_API_KEY));
        assertEquals(apiKey, policyRequest.getDxHeaders().get(API_KEY));
    }

    @Test
    void testNullMockMode() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String xJourneyPolicy = "xJourneyPolicy";
        String clientId = "clientId";
        String processId = "processId";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";

        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, policyName,
                policyVersion, processId, step, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, systemParams);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(xJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
        assertNull(policyRequest.getMockModeFlag());
    }

    @Test
    public void testPolicyNameConstructor() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String xJourneyPolicy = "xJourneyPolicy";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        String clientId = "clientId";
        String processId = "processId";
        Boolean mockMode = false;
        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, null,
                null, processId, null, mockMode);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyState policyState = new PolicyState();
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, policyName, policyVersion, step, systemParams, policyState);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(step, policyRequest.getStep());
        assertEquals(policyState, policyRequest.getPolicyState());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(xJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
        assertEquals(mockMode, policyRequest.getMockModeFlag());
    }

    @Test
    public void testPolicyNameConstructorNotNullXJourneyPolicy() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String clientId = "clientId";
        String processId = "processId";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, null, sessionCorrelationId,  customerIdToken, null,
                null, processId, null, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyState policyState = new PolicyState();
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, policyName, policyVersion, step, systemParams, policyState);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(policyState, policyRequest.getPolicyState());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertNotNull(policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
    }

    @Test
    public void testPolicyNameConstructorNullProcessId() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String xJourneyPolicy = "xJourneyPolicy";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";

        String clientId = "clientId";
        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, null,
                null, null, null, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyState policyState = new PolicyState();
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, policyName, policyVersion, step, systemParams, policyState);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(policyState, policyRequest.getPolicyState());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(xJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertNotNull(policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
    }

    @Test
    void testPolicyNameConstructorNullMockMode() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String xJourneyPolicy = "xJourneyPolicy";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        String processId = "processId";
        String clientId = "clientId";
        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, null,
                null, processId, null, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);
        PolicyState policyState = new PolicyState();
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, policyName, policyVersion, step, systemParams, policyState);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(policyState, policyRequest.getPolicyState());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(xJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId,policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
        assertTrue(policyRequest.getDxHeaders().containsKey(X_JOURNEY_POLICY));
        assertNull(policyRequest.getMockModeFlag());
    }

    @Test
    public void testPolicyNameConstructorMBTwoXJourneyPolicy() {

        Map<String, Serializable> requestBody = new HashMap<>();
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String step = "step";
        String businessEvent = "businessEvent";
        String clientId = "clientId";
        String processId = "processId";
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        String expectedXJourneyPolicy = policyName + ":" + policyVersion + ":" + processId;
        APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, clientId, businessEvent, null, customerIdToken, sessionCorrelationId, policyName,
                policyVersion, processId, step, null);
        PolicyRequestType policyRequestType = PolicyRequestType.CREATE;
        PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "masterbuilder-2.0", null);
        PolicyState policyState = new PolicyState();
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, policyRequestType, requestBody, policyName, policyVersion, step, systemParams, policyState);
        assertSame(requestBody, policyRequest.getRequestBody());
        assertEquals(policyName, policyRequest.getPolicyName());
        assertEquals(policyVersion, policyRequest.getPolicyVersionRequested());
        assertEquals(policyState, policyRequest.getPolicyState());
        assertEquals(step, policyRequest.getStep());
        assertEquals(businessEvent, policyRequest.getBusinessEvent());
        assertEquals(expectedXJourneyPolicy, policyRequest.getXJourneyPolicy());
        assertEquals(expectedXJourneyPolicy, policyRequest.getDxHeaders().get(X_JOURNEY_POLICY));
        assertEquals(clientId, policyRequest.getClientId());
        assertEquals(processId, policyRequest.getProcessId());
        assertEquals(policyRequestType, policyRequest.getPolicyRequestType());
    }

    @Test
    void testPolicyRequestSerializationDoesNotIncludeState() throws JsonProcessingException {

        final APIRequest apiRequest = new APIRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy",
                "customerIdToken", "sessionCorrelationId",null, null, "processId", null, null);
        final PolicyRequest.SystemParams systemParams = new PolicyRequest.SystemParams("none", "test", null);

        final String statePolicyName = UUID.randomUUID().toString();
        final PolicyState policyState = new PolicyState();
        policyState.setPolicyName(statePolicyName);
        PolicyRequest policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, new HashMap<>(),
                "policyName", "policyVersion", "step", systemParams, policyState);

        assertNotNull(policyRequest.getPolicyState());
        final String serializedPolicyRequest = new ObjectMapper().writeValueAsString(policyRequest);
        assertFalse(serializedPolicyRequest.contains("policyState"));
        assertFalse(serializedPolicyRequest.contains(statePolicyName));

    }
}
