package com.capitalone.identity.identitybuilder.policycore.abac.model;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

@Getter
public class PolicyAuthResource {

    @NotNull
    private final String policyName;
    private final int policyMajorVersion;

    public PolicyAuthResource(@NonNull String policyName, @NonNull String policyVersion) {
        this.policyName = policyName;
        this.policyMajorVersion = !policyVersion.isEmpty() ? Float.valueOf(policyVersion).intValue() : 0;
    }
}

