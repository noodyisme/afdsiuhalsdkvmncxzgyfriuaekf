package com.capitalone.identity.identitybuilder.policycore.abac.config;

import com.capitalone.identity.identitybuilder.model.abac.PolicyAccess;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

@Component
public class PolicyAccessRegistry {

    private final Map<String, PolicyAccess> policyAccessStore = new HashMap<>();

    @Nullable
    public PolicyAccess getPolicyAccess(@NonNull String policyName, @NonNull int policyMajorVersion) {
        return this.policyAccessStore.get(getPolicyAccessLookupKey(policyName, policyMajorVersion));
    }

    public synchronized PolicyAccess setPolicyAccess(@NotNull PolicyAccess policyAccess) {
        String policyAccessLookupKey = getPolicyAccessLookupKey(policyAccess.getPolicyShortName(), policyAccess.getPolicyMajorVersion());
        if (this.policyAccessStore.containsKey(policyAccessLookupKey)) {
            // if ids don't match, throw an error
            if (!this.policyAccessStore.get(policyAccessLookupKey).getAccessId().equals(policyAccess.getAccessId())) {
                throw new IllegalArgumentException("PolicyAccess could not be added/updated because accessId provided already exists");
            } else {
                return this.policyAccessStore.put(policyAccessLookupKey, policyAccess);
            }
        } else {
            return this.policyAccessStore.put(policyAccessLookupKey, policyAccess);
        }
    }

    @NotNull
    private String getPolicyAccessLookupKey(@NotNull String policyName, int policyMajorVersion) {
        return policyName + "_" + policyMajorVersion;
    }

    public synchronized PolicyAccess removePolicyAccess(@NonNull String policyName, @NonNull int policyMajorVersion) {
        String policyAccessLookupKey = getPolicyAccessLookupKey(policyName, policyMajorVersion);
        return this.policyAccessStore.remove(policyAccessLookupKey);
    }

}

