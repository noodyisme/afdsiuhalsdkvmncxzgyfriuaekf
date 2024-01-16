package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.pip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder (toBuilder = true)
public class OperationalPipMetadata {

    @JsonProperty("policy_info")
    String policyInfo;

    @JsonProperty("http_method")
    String httpMethod;

    @JsonProperty("endpoint_service_name")
    String endpointServiceName;

    @JsonProperty("endpoint_target_uri")
    String endpointTargetUri;

    @JsonProperty("endpoint_scheme")
    String endpointScheme;

    @JsonProperty("response_http_status")
    String responseHttpStatus;

}

