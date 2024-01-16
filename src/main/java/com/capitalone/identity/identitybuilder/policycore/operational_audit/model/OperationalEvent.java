package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.OperationalConfigStoreMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.decision.OperationalDmnMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.exchange_schema.OperationalProtocolHeader;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.pip.OperationalPipMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalExecutionContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalPolicyMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
@Builder (toBuilder = true)
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE)
public class OperationalEvent {

    @JsonProperty("event_name")
    @NonNull @Builder.Default String eventName = OperationalAuditConstants.UNDEFINED;

    @JsonProperty("event_entity")
    String eventEntity;

    @JsonProperty("event_action")
    @NonNull @Builder.Default String eventAction = OperationalAuditConstants.UNDEFINED;

    @JsonProperty("event_operation_id")
    String eventOperationId;

    @JsonProperty("event_entity_name")
    String eventEntityName;

    @JsonProperty("event_entity_value")
    String eventEntityValue;

    @JsonProperty("event")
    @NonNull @Builder.Default OperationalEventOutcome eventOutcome = OperationalAuditConstants.OUTCOME_UNDEFINED;

    @JsonProperty("event_start_timestamp")
    Long eventStartTimestamp;

    @JsonProperty("event_end_timestamp")
    @NonNull @Builder.Default Long eventEndTimestamp = 0L;

    @JsonProperty("event_duration_milliseconds")
    @NonNull @Builder.Default String eventDurationMs = OperationalAuditConstants.UNDEFINED;

    @JsonProperty("system_context")
    OperationalSystemContext systemContext;

    @JsonProperty("error_metadata")
    OperationalError errorMetadata;

    @JsonProperty("policy_metadata")
    OperationalPolicyMetadata policyMetadata;

    @JsonProperty("policy_information_point_metadata")
    OperationalPipMetadata pipMetadata;

    @JsonProperty("execution_context")
    OperationalExecutionContext executionContext;

    @JsonProperty("request_message_id")
    String requestMessageId;

    @JsonProperty("request_correlation_id")
    String requestCorrelationId;

    @JsonProperty("configuration_store_metadata")
    OperationalConfigStoreMetadata configStoreMetadata;

    @JsonProperty("decision_modeling_notation_metadata")
    OperationalDmnMetadata dmnMetadata;

    @JsonProperty("supplemental_metadata")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Builder.Default List<OperationalSupplementalAttribute> supplementalMetadata = Collections.emptyList();

    @JsonProperty("additional_supporting_information")
    String additionalSupportingInformation;

    @JsonProperty("host_mac_address")
    String hostMacAddress;

    @JsonProperty("amazon_resource_name")
    String amazonResourceName;

    @JsonProperty("process_id")
    String processId;

    @JsonProperty("thread_id")
    String threadId;

    @JsonProperty("request_id")
    String requestId;

    @JsonProperty("client_ip_address")
    String clientIpAddress;

    @JsonProperty("on_behalf_of_user_id")
    String onBehalfOfUser;

    @JsonProperty("user_id")
    String userId;

    @JsonProperty("user_type")
    String userType;

    @JsonProperty("target_resource_id")
    String targetResourceId;

    @JsonProperty("target_resource_type")
    String targetResourceType;

    @JsonProperty("session_id")
    String sessionId;

    @JsonProperty("event_reason")
    @NonNull @Builder.Default String eventReason = OperationalAuditConstants.UNDEFINED;

    @JsonProperty("event_detail")
    @NonNull @Builder.Default String eventDetail = OperationalAuditConstants.UNDEFINED;

    @JsonProperty("protocol_headers")
    @NonNull @Builder.Default List<OperationalProtocolHeader> protocolHeaders = new ArrayList<>();

    @JsonProperty("protocol_uri")
    String protocolUri;

    @JsonProperty("protocol_type")
    String protocolType;

    @JsonProperty("protocol_version")
    String protocolVersion;

    @JsonProperty("protocol_transport_detail")
    String protocolTransportDetail;

    @JsonProperty("protocol_request_detail")
    String protocolRequestDetail;

    @JsonProperty("protocol_response_detail")
    String protocolResponseDetail;
}
