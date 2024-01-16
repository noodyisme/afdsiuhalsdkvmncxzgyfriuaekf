package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalExecutionContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalPolicyMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class OperationalEventTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Test
    public void serializationCorrectSchemaSuccess() throws IOException {
        // Arrange
        final OperationalEvent dummyOperationalEvent = buildDummyOperationalEvent();

        // Act
        final String actualDummyOperationalEventString = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(dummyOperationalEvent);

        // Assert
        Assertions.assertEquals(expectedOperationalEventString, actualDummyOperationalEventString);
    }

    private static OperationalEvent buildDummyOperationalEvent() {
        return OperationalEvent.builder()
                .eventName("testEventName")
                .eventEntity("testEventEntity")
                .eventAction("testEventAction")
                .eventOperationId("testEventOpId")
                .eventEntityName("testEventEntityName")
                .eventOutcome(OperationalEventOutcome.of(OperationalStatus.SUCCESS, "INFO"))
                .eventStartTimestamp(0L)
                .eventEndTimestamp(1L)
                .eventDurationMs("1")
                .systemContext(dummyOperationalSystemContext())
                .errorMetadata(dummyOperationalError())
                .requestMessageId("reqMessageId")
                .requestCorrelationId("reqCorId")
                .policyMetadata(dummyOperationalPolicyMetadata())
                .pipMetadata(null)
                .executionContext(dummyOperationalExecutionContext())
                .configStoreMetadata(null)
                .dmnMetadata(null)
                .supplementalMetadata(Lists.newArrayList(
                        new OperationalSupplementalAttribute("key1", "val1"),
                        new OperationalSupplementalAttribute("key2", "val2")))
                .build();
    }

    private static OperationalSystemContext dummyOperationalSystemContext() {
        return OperationalSystemContext.builder()
                .applicationName("testappname")
                .applicationCode("testappcode")
                .clusterId("testappcluster")
                .containerId("testappcontainer")
                .hostName("testapphost")
                .region("testappregion")
                .environmentName("testappenv")
                .clientId("testappclientid")
                .businessApplication("test")
                .systemId("testappcode.testappenv.testappregion.testappcluster-testappcontainer")
                .build();
    }

    static OperationalError dummyOperationalError() {
        return OperationalError.builder()
                .text("text")
                .developerText("devText")
                .stackTrace("stacktrace")
                .errorId("errorIdStr")
                .build();
    }

    static OperationalPolicyMetadata dummyOperationalPolicyMetadata() {
        return OperationalPolicyMetadata.builder()
                .policyVersionRequested("polVerReq")
                .stepExpiration("stepExp")
                .availableNextSteps(Lists.newArrayList("step3", "step4"))
                .stepsCompleted(Lists.newArrayList("step0","step1"))
                .step("step2")
                .requestType(PolicyRequestType.RESUME)
                .processId("polMetProcId")
                .pipsEvaluated(Lists.newArrayList("pip1", "pip2"))
                .dmnsEvaluated(Lists.newArrayList("dmn1", "dmn2"))
                .policyName("polMetPolName")
                .versionExecuted("polMetVerEx")
                .effectiveNextStep("effectiveNextStep")
                .clientId("clientid")
                .build();
    }

    static OperationalExecutionContext dummyOperationalExecutionContext() {
        return OperationalExecutionContext.builder()
                .apiKey("testHApiKey")
                .contentType("testHContentType")
                .channelType("testHChannelType")
                .countryCode("testHCountryCode")
                .acceptLanguage("testHLanguage")
                .subDomain("testSubdomain")
                .userAgent("hUserAgent")
                .accept("hAccept")
                .businessEvent("testBusinessEvent")
                .xJourneyPolicy("testXJourneyPolicy")
                .domain("testDomain")
                .build();
    }

    private static String expectedOperationalEventString = "{\n"
            + "  \"event_name\" : \"testEventName\",\n"
            + "  \"event_entity\" : \"testEventEntity\",\n"
            + "  \"event_action\" : \"testEventAction\",\n"
            + "  \"event_operation_id\" : \"testEventOpId\",\n"
            + "  \"event_entity_name\" : \"testEventEntityName\",\n"
            + "  \"event_entity_value\" : null,\n"
            + "  \"event\" : {\n"
            + "    \"result\" : \"SUCCESS\",\n"
            + "    \"severity\" : \"INFO\"\n"
            + "  },\n"
            + "  \"event_start_timestamp\" : 0,\n"
            + "  \"event_end_timestamp\" : 1,\n"
            + "  \"event_duration_milliseconds\" : \"1\",\n"
            + "  \"system_context\" : {\n"
            + "    \"application_name\" : \"testappname\",\n"
            + "    \"application_code\" : \"testappcode\",\n"
            + "    \"cluster_id\" : \"testappcluster\",\n"
            + "    \"container_id\" : \"testappcontainer\",\n"
            + "    \"host_name\" : \"testapphost\",\n"
            + "    \"host_ip_address\" : null,\n"
            + "    \"region\" : \"testappregion\",\n"
            + "    \"environment_name\" : \"testappenv\",\n"
            + "    \"client_id\" : \"testappclientid\",\n"
            + "    \"business_application\" : \"test\",\n"
            + "    \"system_id\" : \"testappcode.testappenv.testappregion.testappcluster-testappcontainer\"\n"
            + "  },\n"
            + "  \"error_metadata\" : {\n"
            + "    \"description\" : \"text\",\n"
            + "    \"developer_text\" : \"devText\",\n"
            + "    \"error_id\" : \"errorIdStr\",\n"
            + "    \"stack_trace\" : \"stacktrace\"\n"
            + "  },\n"
            + "  \"policy_metadata\" : {\n"
            + "    \"policy_version_requested\" : \"polVerReq\",\n"
            + "    \"step_expiration\" : \"stepExp\",\n"
            + "    \"available_next_steps\" : [ \"step3\", \"step4\" ],\n"
            + "    \"steps_completed\" : [ \"step0\", \"step1\" ],\n"
            + "    \"current_step\" : \"step2\",\n"
            + "    \"request_type\" : \"RESUME\",\n"
            + "    \"process_id\" : \"polMetProcId\",\n"
            + "    \"policy_information_points_requested\" : [ \"pip1\", \"pip2\" ],\n"
            + "    \"decision_modeling_notations_evaluated\" : [ \"dmn1\", \"dmn2\" ],\n"
            + "    \"policy_name\" : \"polMetPolName\",\n"
            + "    \"version_executed\" : \"polMetVerEx\",\n"
            + "    \"effective_next_step\" : \"effectiveNextStep\",\n"
            + "    \"policy_to_policy_parent_name\" : null,\n"
            + "    \"policy_to_policy_children_policy_names\" : null,\n"
            + "    \"client_id\" : \"clientid\"\n"
            + "  },\n"
            + "  \"policy_information_point_metadata\" : null,\n"
            + "  \"execution_context\" : {\n"
            + "    \"api_key\" : \"testHApiKey\",\n"
            + "    \"content_type\" : \"testHContentType\",\n"
            + "    \"channel_type\" : \"testHChannelType\",\n"
            + "    \"country_code\" : \"testHCountryCode\",\n"
            + "    \"accept_language\" : \"testHLanguage\",\n"
            + "    \"sub_domain\" : \"testSubdomain\",\n"
            + "    \"user_agent\" : \"hUserAgent\",\n"
            + "    \"http_accept\" : \"hAccept\",\n"
            + "    \"business_event\" : \"testBusinessEvent\",\n"
            + "    \"x_journey_policy\" : \"testXJourneyPolicy\",\n"
            + "    \"domain\" : \"testDomain\"\n"
            + "  },\n"
            + "  \"request_message_id\" : \"reqMessageId\",\n"
            + "  \"request_correlation_id\" : \"reqCorId\",\n"
            + "  \"configuration_store_metadata\" : null,\n"
            + "  \"decision_modeling_notation_metadata\" : null,\n"
            + "  \"supplemental_metadata\" : [ {\n"
            + "    \"key\" : \"key1\",\n"
            + "    \"value\" : \"val1\"\n"
            + "  }, {\n"
            + "    \"key\" : \"key2\",\n"
            + "    \"value\" : \"val2\"\n"
            + "  } ],\n"
            + "  \"additional_supporting_information\" : null,\n"
            + "  \"host_mac_address\" : null,\n"
            + "  \"amazon_resource_name\" : null,\n"
            + "  \"process_id\" : null,\n"
            + "  \"thread_id\" : null,\n"
            + "  \"request_id\" : null,\n"
            + "  \"client_ip_address\" : null,\n"
            + "  \"on_behalf_of_user_id\" : null,\n"
            + "  \"user_id\" : null,\n"
            + "  \"user_type\" : null,\n"
            + "  \"target_resource_id\" : null,\n"
            + "  \"target_resource_type\" : null,\n"
            + "  \"session_id\" : null,\n"
            + "  \"event_reason\" : \"UNDEFINED\",\n"
            + "  \"event_detail\" : \"UNDEFINED\",\n"
            + "  \"protocol_headers\" : [ ],\n"
            + "  \"protocol_uri\" : null,\n"
            + "  \"protocol_type\" : null,\n"
            + "  \"protocol_version\" : null,\n"
            + "  \"protocol_transport_detail\" : null,\n"
            + "  \"protocol_request_detail\" : null,\n"
            + "  \"protocol_response_detail\" : null\n"
            + "}";

}