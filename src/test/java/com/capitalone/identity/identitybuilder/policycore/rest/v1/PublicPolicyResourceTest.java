package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.chassis.engine.model.response.ResponseData;
import com.capitalone.identity.identitybuilder.policycore.model.APIRequest;
import com.capitalone.identity.identitybuilder.policycore.model.APIResponse;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyMetadata;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyStatus;
import com.capitalone.identity.identitybuilder.policycore.model.PublicAPIRequest;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicPolicyResourceTest {

    @InjectMocks
    private DelegationPatternResource.JourneyResource publicPolicyResource;
    @Mock
    private PolicyService policyService;
    @Mock
    private Environment env;
    @Spy
    private ResponseData responseData;
    @Mock
    HttpHeaders headers;

    private static final String apiKey = "apiKey";
    private static final String clientApiKey = "clientApiKey";
    private static final String clientCorrelationId = "clientCorrelationId";
    private static final String customerIpAddress = "customerIpAddress";
    private static final String contentType = "contentType";
    private static final String channelType = "channelType";
    private static final String countryCode = "countryCode";
    private static final String language = "language";
    private static final String domain = "domain";
    private static final String subDomain = "subDomain";
    private static final String userAgent = "userAgent";
    private static final String accept = "accept";
    private static final String clientId = "clientId";
    private static final String businessEvent = "businessEvent";
    private static final String xJourneyPolicy = "xJourneyPolicy";
    private static final String customerIdToken = "customerIdToken";
    private static final String sessionCorrelationId = "sessionCorrelationId";
    private static final String policyName = "a";
    private static final String policyVersion = "1.0";
    private static final String processId = "processId";
    private static final String step = "step";
    private static final Boolean mockMode = false;
    private static final PublicAPIRequest publicAPIRequest = PublicAPIRequest.builder()
            .apiKey(apiKey).clientApiKey(clientApiKey).clientCorrelationId(clientCorrelationId).customerIpAddress(customerIpAddress)
            .contentType(contentType).channelType(channelType).countryCode(countryCode).language(language).domain(domain).subDomain(subDomain)
            .userAgent(userAgent).accept(accept).clientId(clientId).businessEvent(businessEvent).xJourneyPolicy(xJourneyPolicy).customerIdToken(customerIdToken)
            .sessionCorrelationId(sessionCorrelationId).policyName(policyName).policyVersion(policyVersion).processId(processId).step(step).build();

    private static final PublicAPIRequest JOURNEY_API_REQUEST_WITH_ALL_MANDATORY_FIELDS_WITH_QUERY = PublicAPIRequest.builder()
            .apiKey(apiKey).clientApiKey(clientApiKey).clientCorrelationId(clientCorrelationId).customerIpAddress(customerIpAddress)
            .contentType(contentType).channelType(channelType).countryCode(countryCode).language(language).domain(domain).subDomain(subDomain)
            .userAgent(userAgent).accept(accept).clientId(clientId).businessEvent(businessEvent).xJourneyPolicy(xJourneyPolicy).customerIdToken(customerIdToken)
            .sessionCorrelationId(sessionCorrelationId).policyName(policyName).policyVersion(policyVersion).processId(processId).step(step).build();

    private static final PublicAPIRequest JOURNEY_API_REQUEST_WITH_ALL_MANDATORY_FIELDS_NO_QUERY = PublicAPIRequest.builder()
            .apiKey(apiKey).clientApiKey(clientApiKey).clientCorrelationId(clientCorrelationId).customerIpAddress(customerIpAddress)
            .contentType(contentType).channelType(channelType).countryCode(countryCode).language(language).domain(domain).subDomain(subDomain)
            .userAgent(userAgent).accept(accept).clientId(clientId).businessEvent(businessEvent).xJourneyPolicy(xJourneyPolicy).customerIdToken(customerIdToken)
            .sessionCorrelationId(sessionCorrelationId).policyName(null).policyVersion(null).processId(processId).step(step).build();

    private static final APIResponse.APISuccessResponse policyResponse = APIResponse.APISuccessResponse.builder().policyStatus(PolicyStatus.SUCCESS).build();


    public HttpHeaders getSuccessfulHeaders() {
        when(headers.getRequestHeader("Client-Correlation-Id")).thenReturn(new LinkedList<>(Collections.singletonList("123")));
        when(headers.getRequestHeader("Channel-Type")).thenReturn(new LinkedList<>(Collections.singletonList("web")));
        when(headers.getRequestHeader("Customer-Ip-Address")).thenReturn(new LinkedList<>(Collections.singletonList("127.0.0.1")));
        return headers;
    }

    @Test
    void execute() {
        when(env.getProperty("identitybuilder.policycore.feature.public-endpoint-header-validation.enabled")).thenReturn("true");
        when(policyService.createProcess(any(APIRequest.class), anyMap())).thenReturn(policyResponse);

        Response resp = publicPolicyResource
                .execute(publicAPIRequest, new HashMap<>(), getSuccessfulHeaders());
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }

    @Test
    void executePathAndQueryTest() {
        when(policyService.createProcess(any(), any())).thenReturn(policyResponse);
        Response pathResp = publicPolicyResource.executeByPath(JOURNEY_API_REQUEST_WITH_ALL_MANDATORY_FIELDS_NO_QUERY, "a", "1.0", new HashMap<>(), headers);
        Response queryResp = publicPolicyResource.executeByQuery(JOURNEY_API_REQUEST_WITH_ALL_MANDATORY_FIELDS_WITH_QUERY, new HashMap<>(), headers);
        Mockito.verify(policyService, times(2)).createProcess(any(APIRequest.class), eq(new HashMap<>()));
        assertEquals(Response.Status.OK.getStatusCode(), pathResp.getStatus());
        assertEquals(Response.Status.OK.getStatusCode(), queryResp.getStatus());
    }

    @Test
    void validateParametersTest() {
        PublicAPIRequest publicAPIRequestNoPolicyName = PublicAPIRequest.builder()
                .apiKey(apiKey).clientApiKey(clientApiKey).clientCorrelationId(clientCorrelationId).customerIpAddress(customerIpAddress)
                .contentType(contentType).channelType(channelType).countryCode(countryCode).language(language).domain(domain).subDomain(subDomain)
                .userAgent(userAgent).accept(accept).clientId(clientId).businessEvent(businessEvent).xJourneyPolicy(xJourneyPolicy).customerIdToken(customerIdToken)
                .sessionCorrelationId(sessionCorrelationId).policyName(null).policyVersion(policyVersion).processId(processId)
                .step(step).mockModeFlag(mockMode).build();
        HashMap<String, Serializable> requestBody = new HashMap<>();
        assertThrows(ChassisBusinessException.class, () -> publicPolicyResource
                .executeByQuery(publicAPIRequestNoPolicyName, requestBody, null));

        PublicAPIRequest publicAPIRequestNoPolicyNamePath = PublicAPIRequest.builder()
                .apiKey(apiKey).clientApiKey(clientApiKey).clientCorrelationId(clientCorrelationId).customerIpAddress(customerIpAddress)
                .contentType(contentType).channelType(channelType).countryCode(countryCode).language(language).domain(domain).subDomain(subDomain)
                .userAgent(userAgent).accept(accept).clientId(clientId).businessEvent(businessEvent).xJourneyPolicy(xJourneyPolicy).customerIdToken(customerIdToken)
                .sessionCorrelationId(sessionCorrelationId).policyName(null).policyVersion(null).processId(processId).step(step).build();

        assertThrows(ChassisBusinessException.class, () -> publicPolicyResource
                .executeByPath(publicAPIRequestNoPolicyNamePath, null, "1.0", requestBody, null));

        PublicAPIRequest publicAPIRequestNoPolicyVersion = PublicAPIRequest.builder()
                .apiKey(apiKey).clientApiKey(clientApiKey).clientCorrelationId(clientCorrelationId).customerIpAddress(customerIpAddress)
                .contentType(contentType).channelType(channelType).countryCode(countryCode).language(language).domain(domain).subDomain(subDomain)
                .userAgent(userAgent).accept(accept).clientId(clientId).businessEvent(businessEvent).xJourneyPolicy(xJourneyPolicy).customerIdToken(customerIdToken)
                .sessionCorrelationId(sessionCorrelationId).policyName(policyName).policyVersion(null).processId(processId).step(step).build();
        assertThrows(ChassisBusinessException.class, () -> publicPolicyResource
                .executeByQuery(publicAPIRequestNoPolicyVersion, requestBody, null));

        PublicAPIRequest publicAPIRequestNoPolicyVersionPath = PublicAPIRequest.builder()
                .apiKey(apiKey).clientApiKey(clientApiKey).clientCorrelationId(clientCorrelationId).customerIpAddress(customerIpAddress)
                .contentType(contentType).channelType(channelType).countryCode(countryCode).language(language).domain(domain).subDomain(subDomain)
                .userAgent(userAgent).accept(accept).clientId(clientId).businessEvent(businessEvent).xJourneyPolicy(xJourneyPolicy).customerIdToken(customerIdToken)
                .sessionCorrelationId(sessionCorrelationId).policyName(null).policyVersion(null).processId(processId).step(step).build();
        assertThrows(ChassisBusinessException.class, () -> publicPolicyResource
                .executeByPath(publicAPIRequestNoPolicyVersionPath, "c", null, requestBody, null));

    }

    @Test
    void missingCCIDHeaderTest() {
        when(env.getProperty("identitybuilder.policycore.feature.public-endpoint-header-validation.enabled")).thenReturn("true");
        HttpHeaders noCCIDHeaders = Mockito.mock(HttpHeaders.class);
        when(noCCIDHeaders.getRequestHeader("Client-Correlation-Id")).thenReturn(new LinkedList<>(Collections.singletonList("")));
        ChassisBusinessException errorByQuery = assertThrows(ChassisBusinessException.class, () ->
                publicPolicyResource
                        .getPolicyMetadataByQuery("policyName", "1.0", null, noCCIDHeaders)
        );
        ChassisBusinessException errorByPath = assertThrows(ChassisBusinessException.class, () ->
                publicPolicyResource
                        .getPolicyMetadataByPath("policyName", "1.0", noCCIDHeaders)
        );
        assertEquals("Missing client correlation Id in the header", errorByQuery.getMessage());
        assertEquals(400, errorByQuery.getHttpStatusCode());
        assertEquals("Missing client correlation Id in the header", errorByPath.getMessage());
        assertEquals(400, errorByPath.getHttpStatusCode());

    }

    @Test
    void missingChannelTypeHeaderTest() {
        when(env.getProperty("identitybuilder.policycore.feature.public-endpoint-header-validation.enabled")).thenReturn("true");
        HttpHeaders noChannelTypeHeaders = Mockito.mock(HttpHeaders.class);
        when(noChannelTypeHeaders.getRequestHeader("Client-Correlation-Id")).thenReturn(new LinkedList<>(Collections.singletonList("123")));
        when(noChannelTypeHeaders.getRequestHeader("Channel-Type")).thenReturn(new LinkedList<>(Collections.singletonList("")));
        ChassisBusinessException errorByQuery = assertThrows(ChassisBusinessException.class, () ->
                publicPolicyResource
                        .getPolicyMetadataByQuery("policyName", "1.0", null, noChannelTypeHeaders)
        );
        ChassisBusinessException errorByPath = assertThrows(ChassisBusinessException.class, () ->
                publicPolicyResource
                        .getPolicyMetadataByPath("policyName", "1.0", noChannelTypeHeaders)
        );
        assertEquals("Missing channel type in the header", errorByQuery.getMessage());
        assertEquals(400, errorByQuery.getHttpStatusCode());
        assertEquals("Missing channel type in the header", errorByPath.getMessage());
        assertEquals(400, errorByPath.getHttpStatusCode());
    }

    @Test
    void invalidIPHeaderTest() {
        when(env.getProperty("identitybuilder.policycore.feature.public-endpoint-header-validation.enabled")).thenReturn("true");
        HttpHeaders invalidIPHeaders = Mockito.mock(HttpHeaders.class);
        when(invalidIPHeaders.getRequestHeader("Client-Correlation-Id")).thenReturn(new LinkedList<>(Collections.singletonList("123")));
        when(invalidIPHeaders.getRequestHeader("Channel-Type")).thenReturn(new LinkedList<>(Collections.singletonList("web")));
        when(invalidIPHeaders.getRequestHeader("Customer-Ip-Address")).thenReturn(new LinkedList<>(Collections.singletonList("127.0.0")));
        ChassisBusinessException errorByQuery = assertThrows(ChassisBusinessException.class, () ->
            publicPolicyResource
                    .getPolicyMetadataByQuery("policyName", "1.0", null, invalidIPHeaders)
        );
        ChassisBusinessException errorByPath = assertThrows(ChassisBusinessException.class, () ->
                publicPolicyResource
                        .getPolicyMetadataByPath("policyName", "1.0", invalidIPHeaders)
        );
        assertEquals("Invalid or missing Customer-IP-Address in header", errorByQuery.getMessage());
        assertEquals(400, errorByQuery.getHttpStatusCode());
        assertEquals("Invalid or missing Customer-IP-Address in header", errorByPath.getMessage());
        assertEquals(400, errorByPath.getHttpStatusCode());
    }

    @Test
    void invalidIPHeaderTestWithHeaderValidationDisabled() {
        when(env.getProperty("identitybuilder.policycore.feature.public-endpoint-header-validation.enabled")).thenReturn("false");
        HttpHeaders invalidIPHeaders = Mockito.mock(HttpHeaders.class);
        PolicyMetadata policyCoreMetadata = new PolicyMetadata();
        when(policyService.getPolicyMetadata(anyString(), anyString(), isNull())).thenReturn(policyCoreMetadata);
        Response respByQuery = publicPolicyResource
                .getPolicyMetadataByQuery("policyName", "1.0", null, invalidIPHeaders);
        Response respByPath = publicPolicyResource
                .getPolicyMetadataByPath("policyName", "1.0", invalidIPHeaders);
        assertEquals(Response.Status.OK.getStatusCode(), respByQuery.getStatus());
        assertEquals(Response.Status.OK.getStatusCode(), respByPath.getStatus());
    }

    @Test
    void testInstantiation() {
        assertDoesNotThrow(DelegationPatternResource.JourneyResource::new);
        assertDoesNotThrow(DelegationPatternResource.DomainResource::new);
        assertDoesNotThrow(DelegationPatternResource.DomainFileUploadResource::new);
    }

}
