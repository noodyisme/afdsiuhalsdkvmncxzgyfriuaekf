package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.decision;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class OperationalDmnMetadata {

    @JsonProperty("runtime_name")
    String dmnRuntimeName;

    @JsonProperty("result_key")
    String dmnResultKey;

}
