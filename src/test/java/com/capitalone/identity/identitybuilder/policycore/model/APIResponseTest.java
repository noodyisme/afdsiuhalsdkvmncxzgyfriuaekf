package com.capitalone.identity.identitybuilder.policycore.model;

import com.capitalone.identity.identitybuilder.decisionengine.service.audit.exceptions.ErrorDetails;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class APIResponseTest {
    @Test
    public void testAPISuccessResponse() {
        ProcessMetadata processMetadata = new ProcessMetadata();
        ErrorInfo errorInfo = new ErrorInfo();
        String results = "results";
        APIResponse.APISuccessResponse apiResponse = APIResponse.APISuccessResponse.builder().results(results)
                .policyStatus(PolicyStatus.FAILURE)
                .metadata(processMetadata)
                .errorInfo(errorInfo).build();
        assertEquals(results, apiResponse.getResults());
        assertEquals(PolicyStatus.FAILURE, apiResponse.getPolicyStatus());
        assertSame(errorInfo, apiResponse.getErrorInfo());
        assertSame(processMetadata, apiResponse.getMetadata());
    }

    @Test
    public void testAPIFailureResponse() {
        String id = "id";
        String text = "text";
        ArrayList<String> actions = new ArrayList<>();
        String developerText = "developerText";
        ArrayList<ErrorDetails> errorDetails = new ArrayList<>();
        APIResponse.APIFailureResponse apiResponse = APIResponse.APIFailureResponse.builder().id(id).text(text).actions(actions).developerText(developerText)
                .errorDetails(errorDetails).build();
        assertSame(actions, apiResponse.getActions());
        assertSame(errorDetails, apiResponse.getErrorDetails());
        assertEquals(id, apiResponse.getId());
        assertEquals(text, apiResponse.getText());
        assertEquals(developerText, apiResponse.getDeveloperText());
    }
}