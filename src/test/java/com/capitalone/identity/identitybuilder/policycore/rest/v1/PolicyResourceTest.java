package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.model.response.ResponseData;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import io.vavr.API;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PolicyResourceTest {

    /**
     * The target class is managed by Jersey and does not support constructor injection.
     */
    @InjectMocks
    private PolicyResource policyResource;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private PolicyService policyService;
    @Mock
    private APIRequest apiRequest;
    @Spy
    private ExecutePolicyRequest executePolicyRequest;
    @Spy
    private ResponseData responseData;
    @Captor
    private ArgumentCaptor<Integer> statusPreferenceCaptor;

    @Test
    public void testCreateProcess() {
        PolicyResponse policyResponse = PolicyResponse.builder().results("test").policyStatus(PolicyStatus.FAILURE)
                .errorInfo(new ErrorInfo()).metadata(new ProcessMetadata()).build();
        APIResponse apiResponse = APIResponse.APISuccessResponse.builder().results(policyResponse.getResults())
                .policyStatus(policyResponse.getPolicyStatus())
                .metadata(policyResponse.getMetadata())
                .errorInfo(policyResponse.getErrorInfo()).build();
        when(policyService.createProcess(any(APIRequest.class), anyMap())).thenReturn(apiResponse);
        when(executePolicyRequest.getPolicyParameters()).thenReturn(new HashMap<>());
        Response resp = policyResource.createProcess(apiRequest, executePolicyRequest);
        verify(responseData).setStatusPreference(statusPreferenceCaptor.capture());
        assertEquals(Response.Status.OK.getStatusCode(), (int) statusPreferenceCaptor.getValue());
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        assertSame(apiResponse, resp.getEntity());
    }

    @Test
    public void testGetPolicyMetadata() {
        PolicyMetadata policyMetadata = new PolicyMetadata();
        policyMetadata.setPolicyName("policyName");
        when(policyService.getPolicyMetadata(anyString(), anyString(), isNull())).thenReturn(policyMetadata);
        Response resp = policyResource.getPolicyMetadata(executionContext, "policyName", "1.0", null);
        verify(responseData).setStatusPreference(statusPreferenceCaptor.capture());
        assertEquals(Response.Status.OK.getStatusCode(), (int) statusPreferenceCaptor.getValue());
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        assertSame(policyMetadata, resp.getEntity());
    }

    @Test
    public void testResumeProcess() {
        PolicyResponse policyResponse = PolicyResponse.builder().results("test").policyStatus(PolicyStatus.FAILURE)
                .errorInfo(new ErrorInfo()).metadata(new ProcessMetadata()).build();
        APIResponse apiResponse = APIResponse.APISuccessResponse.builder().results(policyResponse.getResults())
                .policyStatus(policyResponse.getPolicyStatus())
                .metadata(policyResponse.getMetadata())
                .errorInfo(policyResponse.getErrorInfo()).build();
        when(policyService.resumeProcess(any(APIRequest.class), anyMap())).thenReturn(apiResponse);
        when(executePolicyRequest.getPolicyParameters()).thenReturn(new HashMap<>());
        Response resp = policyResource.resumeProcess(apiRequest, executePolicyRequest);
        verify(responseData).setStatusPreference(statusPreferenceCaptor.capture());
        assertEquals(Response.Status.OK.getStatusCode(), (int) statusPreferenceCaptor.getValue());
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        assertSame(apiResponse, resp.getEntity());
    }

    @Test
    public void testGetProcessMetadata() {
        ProcessMetadata processMetadata = new ProcessMetadata();
        when(policyService.getProcessMetadata(anyString())).thenReturn(processMetadata);
        Response resp = policyResource.getProcessMetadata(executionContext, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        verify(responseData).setStatusPreference(statusPreferenceCaptor.capture());
        assertEquals(Response.Status.OK.getStatusCode(), (int) statusPreferenceCaptor.getValue());
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        assertSame(processMetadata, resp.getEntity());
    }
}