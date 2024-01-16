package com.capitalone.identity.identitybuilder.policycore.service.versions;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import lombok.Builder;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PolicyVersionService {

    public static final String FEATURE_FLAG_VERSION_FORWARDER_KEY = "identitybuilder.policycore.feature.version-forwarder.enabled";

    private final Map<PolicyDataObject, EntityActivationStatus> policyStatusMap = new HashMap<>();

    private final MultiValuedMap<String, PolicyDataObject> policyVersionMap = new HashSetValuedHashMap<>();

    private final boolean isFeatureEnabled;

    public PolicyVersionService(@Value("${" + FEATURE_FLAG_VERSION_FORWARDER_KEY + ":false}") boolean featureEnabled) {
        this.isFeatureEnabled = featureEnabled;
    }

    public PolicyInfo.Patch getPolicyVersion(String policyShortName, String version) {
        if (version.contains(".")) {
            return policyVersionMap.get(policyShortName + version).stream()
                    .max(Comparator.comparingInt(PolicyDataObject::getPolicyPatchVersion))
                    .filter(policy -> {
                        EntityActivationStatus status = policyStatusMap.get(policy);
                        return status == EntityActivationStatus.ACTIVE || status == EntityActivationStatus.AVAILABLE;
                    })
                    .orElse(null);
        } else if (isFeatureEnabled) {
            final int majorVersion = Integer.parseInt(version);
            return policyVersionMap.get(policyShortName + majorVersion).stream()
                    .collect(Collectors.groupingBy(PolicyDataObject::getPolicyMinorVersion))
                    .values().stream()
                    .map(patches -> patches.stream().max(Comparator.comparingInt(PolicyDataObject::getPolicyPatchVersion)))
                    .flatMap(patch -> patch.map(Stream::of).orElseGet(Stream::empty))
                    .filter(entry -> policyStatusMap.get(entry) == EntityActivationStatus.ACTIVE)
                    .max(Comparator.comparingInt(PolicyDataObject::getPolicyMinorVersion))
                    .orElse(null);
        } else {
            return null;
        }
    }

    public void set(PolicyInfo.Patch policy, EntityActivationStatus policyActivationStatus) {
        policyStatusMap.keySet().stream()
                .filter(loadedPolicy -> loadedPolicy.getPolicyShortName().equals(policy.getPolicyShortName()))
                .filter(matchingShortPolicyName -> !matchingShortPolicyName.getPolicyFullName().equals(policy.getPolicyFullName()))
                .findAny()
                .ifPresent(minor -> {
                    final String msg = String.format("Policy Name '%s' cannot be loaded. " +
                                    "Policy Short Name '%s' is already mapped to long name=%s",
                            policy.getPolicyFullName(), policy.getPolicyShortName(), minor.getPolicyFullName());
                    throw new IllegalArgumentException(msg);
                });

        PolicyDataObject policyDataObject = PolicyDataObject.create(policy);
        policyStatusMap.put(policyDataObject, policyActivationStatus);
        policyVersionMap.get(policy.getPolicyShortName() + policy.getPolicyVersion()).add(policyDataObject);
        policyVersionMap.get(policy.getPolicyShortName() + policy.getPolicyMajorVersion()).add(policyDataObject);
        policyVersionMap.get(policy.getPolicyShortName() + policy.getPolicyPatchVersionString()).add(policyDataObject);

    }

    public void remove(PolicyInfo.Patch policy) {
        PolicyDataObject policyDataObject = PolicyDataObject.create(policy);
        policyStatusMap.remove(policyDataObject);
        policyVersionMap.get(policy.getPolicyShortName() + policy.getPolicyVersion()).remove(policyDataObject);
        policyVersionMap.get(policy.getPolicyShortName() + policy.getPolicyMajorVersion()).remove(policyDataObject);
        policyVersionMap.get(policy.getPolicyShortName() + policy.getPolicyPatchVersionString()).remove(policyDataObject);

    }

    @lombok.Value
    @Builder
    private static class PolicyDataObject implements PolicyInfo.Patch {

        String policyVersion;
        int policyMinorVersion;
        int policyMajorVersion;
        int policyPatchVersion;
        String policyShortName;
        String policyFullName;

        static PolicyDataObject create(PolicyInfo.Patch source) {
            return new PolicyDataObjectBuilder()
                    .policyVersion(source.getPolicyVersion())
                    .policyMinorVersion(source.getPolicyMinorVersion())
                    .policyMajorVersion(source.getPolicyMajorVersion())
                    .policyPatchVersion(source.getPolicyPatchVersion())
                    .policyShortName(source.getPolicyShortName())
                    .policyFullName(source.getPolicyFullName())
                    .build();
        }
    }
}
