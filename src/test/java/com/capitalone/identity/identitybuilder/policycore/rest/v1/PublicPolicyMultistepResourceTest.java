package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.model.response.ResponseData;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicPolicyMultistepResourceTest {

    @InjectMocks
    private PublicPolicyMultistepResource publicPolicyMultistepResource;
    @Mock
    private PolicyService policyService;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private Environment env;
    @Spy
    private ResponseData responseData;
    @Mock
    private HttpHeaders headers;


    private APIRequest apiRequest;

    public HttpHeaders getSuccessfulHeaders() {
        when(headers.getRequestHeader("Client-Correlation-Id")).thenReturn(new LinkedList<>(Collections.singletonList("123")));
        when(headers.getRequestHeader("Channel-Type")).thenReturn(new LinkedList<>(Collections.singletonList("web")));
        when(headers.getRequestHeader("Customer-Ip-Address")).thenReturn(new LinkedList<>(Collections.singletonList("127.0.0.1")));
        return headers;
    }

    @BeforeEach
    public void setup() {
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
        String customerIdToken = "customerIdToken";
        String sessionCorrelationId = "sessionCorrelationId";
        String xJourneyPolicy = "xJourneyPolicy";
        String policyName = "policyName";
        String policyVersion = "policyVersion";
        String processId = "processId";
        String step = "step";
        Boolean mockModeFlag = false;
        apiRequest = new APIRequest(apiKey, clientApiKey, clientCorrelationId, customerIpAddress,
                contentType, channelType, countryCode, language, domain, subDomain, userAgent, accept, clientId,
                businessEvent, xJourneyPolicy, customerIdToken, sessionCorrelationId, policyName, policyVersion, processId, step, mockModeFlag
        );
        when(env.getProperty("identitybuilder.policycore.feature.public-endpoint-header-validation.enabled")).thenReturn("true");
    }

    @Test
    void testResumeProcess() {
        APIResponse.APISuccessResponse policyResponse = APIResponse.APISuccessResponse.builder().policyStatus(PolicyStatus.SUCCESS).build();
        when(policyService.resumeProcess(any(APIRequest.class), anyMap())).thenReturn(policyResponse);
        Response resp = publicPolicyMultistepResource.resumeProcess(apiRequest, new HashMap<>(), getSuccessfulHeaders());
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }

    @Test
    void testGetProcess() {
        ProcessMetadata processMetadata = new ProcessMetadata();
        when(policyService.getProcessMetadata(anyString())).thenReturn(processMetadata);
        Response resp = publicPolicyMultistepResource.getProcessMetadata(executionContext,"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", getSuccessfulHeaders());
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }
}
