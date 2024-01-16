package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.exchange_schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor
public class OperationalProtocolHeader {

    @JsonProperty("header_name")
    @NonNull String headerName;

    @JsonProperty("header_value")
    Object headerValue; // TODO set to String once major version upgrade of dataset complete

}
