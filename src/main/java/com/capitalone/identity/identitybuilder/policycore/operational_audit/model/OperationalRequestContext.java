package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import lombok.Builder;
import lombok.Value;

@Builder (toBuilder = true)
@Value
public class OperationalRequestContext {

    // It is an optional field that clients can provide to any eAPI service to track traffic across different APIs.
    // If this ID is provided, its value will be added to Audit. Otherwise it will be ignored.
    // Clients who utilize this are responsible for generating the value, guaranteeing uniqueness,
    // and providing it as a Header in every request.
    String correlationId;

    // Header Param. Will be populated for the request made at the source of the
    // dataflow and linked back from subsequent requests/replies through the correlation_id.
    String messageId;

}
