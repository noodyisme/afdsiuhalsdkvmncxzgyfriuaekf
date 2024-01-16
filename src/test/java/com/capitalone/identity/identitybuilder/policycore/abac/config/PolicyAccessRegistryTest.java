package com.capitalone.identity.identitybuilder.policycore.abac.config;

import com.capitalone.identity.identitybuilder.model.abac.AccessGrant;
import com.capitalone.identity.identitybuilder.model.abac.Client;
import com.capitalone.identity.identitybuilder.model.abac.PolicyAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PolicyAccessRegistryTest {

    @Test
    void setGetPolicyAccessSuccessfullyTest() {
        final String policyShortName = "one_second_policy_api";
        final int policyMajorVersion = 1;
        PolicyAccessRegistry registry = new PolicyAccessRegistry();

        assertNull(registry.getPolicyAccess(policyShortName, policyMajorVersion));

        PolicyAccess accessObject = new PolicyAccess("a/b/c/1", policyShortName, policyMajorVersion,
                Collections.emptyList());
        PolicyAccess priorVersion = registry.setPolicyAccess(accessObject);
        assertNull(priorVersion);

        assertEquals(accessObject, registry.getPolicyAccess(policyShortName, policyMajorVersion));
    }

    @Test
    void getPolicyAccessForNullLookupKeyTest() {
        PolicyAccessRegistry registry = new PolicyAccessRegistry();
        assertThrows(NullPointerException.class, () -> registry.getPolicyAccess(null, 1));
    }

    @Test
    void getPolicyAccessForNonExistingLookupKeyTest() {
        PolicyAccessRegistry registry = new PolicyAccessRegistry();
        PolicyAccess policyAccess = registry.getPolicyAccess("sample_unknown_policy", 1);
        assertNull(policyAccess);
    }

    @Test
    void setPolicyAccessForExistingPolicyAccessTest() {
        final String policyShortName = "policy_a";
        final int policyMajorVersion = 1;
        final String accessId = "a/b/c/1";
        PolicyAccessRegistry registry = new PolicyAccessRegistry();

        PolicyAccess accessObject = new PolicyAccess(accessId, policyShortName, policyMajorVersion,
                Collections.emptyList());
        registry.setPolicyAccess(accessObject);

        PolicyAccess accessObjectNew = new PolicyAccess(accessId, policyShortName, policyMajorVersion,
                Collections.singletonList(new Client("11223344", AccessGrant.ALLOW)));
        PolicyAccess priorVersion = registry.setPolicyAccess(accessObjectNew);
        assertEquals(accessObject, priorVersion);

        PolicyAccess fetchedAfterAdded = registry.getPolicyAccess(policyShortName, policyMajorVersion);
        assertEquals(accessObjectNew, fetchedAfterAdded);

    }

    @Test
    void setPolicyAccessForExisting_throwsWhenReplacingDifferentAccessId() {
        final String policyShortName = "policy_a";
        final int policyMajorVersion = 1;
        PolicyAccessRegistry registry = new PolicyAccessRegistry();

        PolicyAccess accessObject = new PolicyAccess("a/b/c/1", policyShortName, policyMajorVersion,
                Collections.emptyList());
        registry.setPolicyAccess(accessObject);

        PolicyAccess accessObjectNew = new PolicyAccess("a/b/z/1", policyShortName, policyMajorVersion,
                Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> registry.setPolicyAccess(accessObjectNew));

    }

    @Test
    void removePolicyAccessSuccessfullyTest() {
        final String policyShortName = "policy_a";
        final int policyMajorVersion = 1;
        PolicyAccessRegistry registry = new PolicyAccessRegistry();
        PolicyAccess accessObject = new PolicyAccess("a/b/c/1", policyShortName, policyMajorVersion,
                Collections.emptyList());
        registry.setPolicyAccess(accessObject);

        assertNotNull(registry.removePolicyAccess(policyShortName, policyMajorVersion));
    }

    @Test
    void removeNonExistingPolicyAccessTest() {
        PolicyAccessRegistry registry = new PolicyAccessRegistry();
        assertNull(registry.removePolicyAccess("policy_a", 1));
    }

}
