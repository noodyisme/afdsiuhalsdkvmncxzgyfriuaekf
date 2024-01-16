package com.capitalone.identity.identitybuilder.policycore.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyPolicyResponseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    @Test
    void testResponseError() throws JsonProcessingException {
        PolicyStatus policyStatus = PolicyStatus.FAILURE;
        Object results = null;
        ErrorInfo errorInfo = new ErrorInfo("123456", "error text", "developer text");
        ProcessMetadata metadata = null;

        APIResponse.APISuccessResponse policyResponse = APIResponse.APISuccessResponse.builder()
                .policyStatus(policyStatus)
                .errorInfo(errorInfo)
                .results(results)
                .metadata(metadata)
                .build();

        JourneyPolicyResponse response = new JourneyPolicyResponse(policyResponse);
        String output = objectMapper.writeValueAsString(response);

        String expected = "{\"policyResponseBody\":null,\"policyStatus\":\"FAILURE\",\"errorInformation\":{\"id\":\"123456\",\"text\":\"error text\",\"developerText\":\"developer text\"}}";
        assertEquals(expected, output);
    }

    @Test
    void testResponseSuccessList() throws JsonProcessingException {
        PolicyStatus policyStatus = PolicyStatus.SUCCESS;
        Object results = Stream.of("val1", "val2", "val3").collect(Collectors.toList());
        ErrorInfo errorInfo = null;
        ProcessMetadata metadata = null;

        APIResponse.APISuccessResponse policyResponse = APIResponse.APISuccessResponse.builder()
                .policyStatus(policyStatus)
                .errorInfo(errorInfo)
                .results(results)
                .metadata(metadata)
                .build();

        JourneyPolicyResponse response = new JourneyPolicyResponse(policyResponse);

        String output = objectMapper.writeValueAsString(response);

        String expected = "{\"policyResponseBody\":[\"val1\",\"val2\",\"val3\"],\"policyStatus\":\"SUCCESS\"}";
        assertEquals(expected, output);
    }

    @Test
    void testResponseSuccessString() throws JsonProcessingException {
        PolicyStatus policyStatus = PolicyStatus.SUCCESS;
        Object results = "val1";
        ErrorInfo errorInfo = null;
        ProcessMetadata metadata = null;

        APIResponse.APISuccessResponse policyResponse = APIResponse.APISuccessResponse.builder()
                .policyStatus(policyStatus)
                .errorInfo(errorInfo)
                .results(results)
                .metadata(metadata)
                .build();

        JourneyPolicyResponse response = new JourneyPolicyResponse(policyResponse);

        String output = objectMapper.writeValueAsString(response);

        String expected = "{\"policyResponseBody\":\"val1\",\"policyStatus\":\"SUCCESS\"}";
        assertEquals(expected, output);
    }

    @Test
    void testResponseSuccessMap() throws JsonProcessingException {
        PolicyStatus policyStatus = PolicyStatus.SUCCESS;
        Object results = new HashMap<String, String>() {{
            put("key1", "value1");
            put("key2", "value2");
        }};
        ErrorInfo errorInfo = null;
        ProcessMetadata metadata = null;

        APIResponse.APISuccessResponse policyResponse = APIResponse.APISuccessResponse.builder()
                .policyStatus(policyStatus)
                .errorInfo(errorInfo)
                .results(results)
                .metadata(metadata)
                .build();
        JourneyPolicyResponse response = new JourneyPolicyResponse(policyResponse);

        String output = objectMapper.writeValueAsString(response);

        String expected = "{\"policyResponseBody\":{\"key1\":\"value1\",\"key2\":\"value2\"},\"policyStatus\":\"SUCCESS\"}";
        assertEquals(expected, output);
    }

}
