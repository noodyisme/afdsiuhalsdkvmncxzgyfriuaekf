package com.capitalone.identity.identitybuilder.policycore.service.util;

import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import joptsimple.internal.Strings;
import lombok.Builder;

import java.util.Arrays;

@lombok.Value
@Builder
public class MockPolicyVersion implements PolicyInfo.Patch {

    int policyMinorVersion;
    int policyMajorVersion;
    int policyPatchVersion;
    String policyShortName;
    String policyFullName;

    public static MockPolicyVersion create(String name) {
        String[] split = name.split("/");
        String fullName = Strings.join(Arrays.copyOf(split, 3), "/");
        String shortName = split[2];
        String version = split[3];
        String[] semanticVersions = version.split("\\.");
        int major = Integer.parseInt(semanticVersions[0]);
        int minor = Integer.parseInt(semanticVersions[1]);
        int patch = semanticVersions.length < 3 ? 0 : Integer.parseInt(semanticVersions[2]);
        return MockPolicyVersion.builder()
                .policyShortName(shortName)
                .policyFullName(fullName)
                .policyMajorVersion(major)
                .policyMinorVersion(minor)
                .policyPatchVersion(patch)
                .build();
    }

    public static MockPolicyVersion create(PolicyInfo.Patch result) {
        return MockPolicyVersion.builder()
                .policyShortName(result.getPolicyShortName())
                .policyFullName(result.getPolicyFullName())
                .policyMajorVersion(result.getPolicyMajorVersion())
                .policyMinorVersion(result.getPolicyMinorVersion())
                .policyPatchVersion(result.getPolicyPatchVersion())
                .build();
    }
}
