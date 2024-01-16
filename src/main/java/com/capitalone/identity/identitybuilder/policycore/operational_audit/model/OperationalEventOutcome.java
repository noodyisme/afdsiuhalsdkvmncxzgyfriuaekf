package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

@Value
public class OperationalEventOutcome {
    @JsonProperty("result")
    @NonNull String result;

    @JsonProperty("severity")
    @NonNull String severity;

    public static OperationalEventOutcome of(final OperationalStatus result, String severity) {
        return new OperationalEventOutcome(result.name(), severity);
    }
}
