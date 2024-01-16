package com.capitalone.identity.identitybuilder.policycore.model;

import java.util.Map;

import lombok.*;

/**
 * Metadata associated with policy. Returned by GET ../{policyName}/{policyVersion}
 */
@Data
@NoArgsConstructor
public class PolicyMetadata {
    @NonNull
    private String domain;
    @NonNull
    private String policyName;
    @NonNull
    private String policyVersion;

    private String step;
    @NonNull
    private Map<String, Object> policyJsonSchema;

}
