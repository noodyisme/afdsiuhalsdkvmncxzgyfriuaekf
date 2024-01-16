package com.capitalone.identity.identitybuilder.policycore.abac.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class PolicyAuthSubject {
    String clientId;
}
