package com.capitalone.identity.identitybuilder.policycore.service.versions;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.policycore.service.util.MockPolicyVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolicyVersionServiceTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "1.0",
            "1.1",
            "1.2",
            "2.2",
            "2.13",
    })
    void disabledVersionForwarderReturnsResultForNonDisabledMinorVersions(String policyVersion) {
        final PolicyInfo.Patch expected = MockPolicyVersion.create("a/b/c/" + policyVersion);

        PolicyVersionService service = new PolicyVersionService(false);
        service.set(MockPolicyVersion.create("a/b/c/1.0"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.1"), EntityActivationStatus.AVAILABLE);
        service.set(MockPolicyVersion.create("a/b/c/1.2"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.3"), EntityActivationStatus.DISABLED);
        service.set(MockPolicyVersion.create("a/b/c/2.2"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/2.13"), EntityActivationStatus.ACTIVE);

        assertEquals(expected, MockPolicyVersion.create(service.getPolicyVersion("c", policyVersion)));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.3",
            "1.10",
            "2.1",
            "2.110",
            "3.0",
            "1",
            "2",
            "3",
    })
    void disabledVersionForwarderReturnsNullForMissingAndDisabledOrMajorVersions(String policyVersion) {
        PolicyVersionService service = new PolicyVersionService(false);
        service.set(MockPolicyVersion.create("a/b/c/1.0"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.1"), EntityActivationStatus.AVAILABLE);
        service.set(MockPolicyVersion.create("a/b/c/1.2"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.3"), EntityActivationStatus.DISABLED);
        service.set(MockPolicyVersion.create("a/b/c/2.0"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/2.13"), EntityActivationStatus.ACTIVE);

        assertNull(service.getPolicyVersion("c", policyVersion));

    }

    @Test
    void getHighestMinorVersion() {
        PolicyVersionService service = new PolicyVersionService(true);
        service.set(MockPolicyVersion.create("a/b/c/1.0"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.1"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.2"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.3"), EntityActivationStatus.DISABLED);
        service.set(MockPolicyVersion.create("a/b/c/2.0"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/2.2"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/2.13"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/2.14"), EntityActivationStatus.AVAILABLE);

        assertEquals(MockPolicyVersion.create("a/b/c/1.2"), MockPolicyVersion.create(service.getPolicyVersion("c", "1")));
        assertEquals(MockPolicyVersion.create("a/b/c/2.13"), MockPolicyVersion.create(service.getPolicyVersion("c", "2")));
    }

    @Test
    void getHighestMinorVersion_loadedOutOfOrder() {
        PolicyVersionService service = new PolicyVersionService(true);
        service.set(MockPolicyVersion.create("a/b/c/1.2"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.1"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.0"), EntityActivationStatus.ACTIVE);
        assertEquals(MockPolicyVersion.create("a/b/c/1.2"), MockPolicyVersion.create(service.getPolicyVersion("c", "1")));
    }

    @Test
    void getHighestMinorVersion_noMajorVersionSideEffects() {

        final List<String> majorVersion1Patches = Arrays.asList("1.0.0", "1.0.1", "1.0.2", "1.1.0", "1.1.1", "1.1.2", "1.2.0", "1.2.1", "1.2.2");
        final List<String> majorVersion2Patches = Arrays.asList("2.0.0", "2.0.1", "2.0.2", "2.1.0", "2.1.1", "2.1.2", "2.2.0", "2.2.1", "2.2.2");
        for (String majorVersion1PatchVersion : majorVersion1Patches) {
            for (String majorVersion2PatchVersion : majorVersion2Patches) {
                PolicyVersionService service = new PolicyVersionService(true);

                MockPolicyVersion major1Patch = MockPolicyVersion.create("us/ep2/policy_a/" + majorVersion1PatchVersion);
                MockPolicyVersion major2Patch = MockPolicyVersion.create("us/ep2/policy_a/" + majorVersion2PatchVersion);

                service.set(major1Patch, EntityActivationStatus.ACTIVE);
                service.set(major2Patch, EntityActivationStatus.ACTIVE);

                PolicyInfo.Patch major1Result = service.getPolicyVersion("policy_a", "1");
                PolicyInfo.Patch major2Result = service.getPolicyVersion("policy_a", "2");

                assertEquals(major1Patch, MockPolicyVersion.create(major1Result));
                assertEquals(major2Patch, MockPolicyVersion.create(major2Result));
            }
        }

    }

    @Test
    void getHighestMinorVersion_noMinorVersionSideEffects() {

        final List<String> majorVersion1Patches = Arrays.asList("1.1.0", "1.1.1", "1.1.2");
        final List<String> majorVersion2Patches = Arrays.asList("1.2.0", "1.2.1", "1.2.2");
        for (String majorVersion1PatchVersion : majorVersion1Patches) {
            for (String majorVersion2PatchVersion : majorVersion2Patches) {
                PolicyVersionService service = new PolicyVersionService(true);
                MockPolicyVersion major1Patch = MockPolicyVersion.create("us/ep2/policy_a/" + majorVersion1PatchVersion);
                MockPolicyVersion major2Patch = MockPolicyVersion.create("us/ep2/policy_a/" + majorVersion2PatchVersion);

                service.set(major1Patch, EntityActivationStatus.ACTIVE);

                PolicyInfo.Patch major1Result = service.getPolicyVersion("policy_a", "1");
                assertEquals(major1Patch, MockPolicyVersion.create(major1Result));
                major1Result = service.getPolicyVersion("policy_a", "1.1");
                assertEquals(major1Patch, MockPolicyVersion.create(major1Result));

                service.set(major2Patch, EntityActivationStatus.ACTIVE);

                major1Result = service.getPolicyVersion("policy_a", "1.1");
                assertEquals(major1Patch, MockPolicyVersion.create(major1Result));
                PolicyInfo.Patch major2Result = service.getPolicyVersion("policy_a", "1");
                assertEquals(major2Patch, MockPolicyVersion.create(major2Result));
                major2Result = service.getPolicyVersion("policy_a", "1.2");
                assertEquals(major2Patch, MockPolicyVersion.create(major2Result));
            }
        }

    }

    @Test
    void getHighestPatchVersionActivationStatus() {
        PolicyVersionService service = new PolicyVersionService(true);
        MockPolicyVersion patch1 = MockPolicyVersion.create("us/ep2/policy_a/1.0.1");

        service.set(patch1, EntityActivationStatus.AVAILABLE);
        assertNull(service.getPolicyVersion("policy_a", "1"));
        assertEquals(patch1, MockPolicyVersion.create(service.getPolicyVersion("policy_a", "1.0")));

        MockPolicyVersion patch2 = MockPolicyVersion.create("us/ep2/policy_a/1.0.2");
        service.set(patch2, EntityActivationStatus.ACTIVE);
        assertEquals(patch2, MockPolicyVersion.create(service.getPolicyVersion("policy_a", "1")));
        assertEquals(patch2, MockPolicyVersion.create(service.getPolicyVersion("policy_a", "1.0")));

        MockPolicyVersion patch3 = MockPolicyVersion.create("us/ep2/policy_a/1.0.3");
        service.set(patch3, EntityActivationStatus.DISABLED);
        assertNull(service.getPolicyVersion("policy_a", "1"));
        assertNull(service.getPolicyVersion("policy_a", "1.0"));
    }

    @Test
    void getHighestMinorVersion_notLoaded() {
        PolicyVersionService service = new PolicyVersionService(true);
        assertNull(service.getPolicyVersion("c", "3"));
    }

    @Test
    void getHighestMinorVersion_specified() {
        PolicyVersionService service = new PolicyVersionService(true);
        service.set(MockPolicyVersion.create("a/b/c/1.1"), EntityActivationStatus.DISABLED);
        service.set(MockPolicyVersion.create("a/b/c/1.2"), EntityActivationStatus.ACTIVE);
        service.set(MockPolicyVersion.create("a/b/c/1.3"), EntityActivationStatus.AVAILABLE);

        assertNull(service.getPolicyVersion("c", "1.1"));
        assertEquals(MockPolicyVersion.create("a/b/c/1.2"), MockPolicyVersion.create(service.getPolicyVersion("c", "1.2")));
        assertEquals(MockPolicyVersion.create("a/b/c/1.3"), MockPolicyVersion.create(service.getPolicyVersion("c", "1.3")));
    }

    @Test
    void getHighestMinorVersion_putDynamic() {
        PolicyVersionService service = new PolicyVersionService(true);
        service.set(MockPolicyVersion.create("a/b/c/1.0"), EntityActivationStatus.ACTIVE);
        assertEquals(MockPolicyVersion.create("a/b/c/1.0"), MockPolicyVersion.create(service.getPolicyVersion("c", "1")));
        service.set(MockPolicyVersion.create("a/b/c/1.1"), EntityActivationStatus.ACTIVE);
        assertEquals(MockPolicyVersion.create("a/b/c/1.1"), MockPolicyVersion.create(service.getPolicyVersion("c", "1")));
        service.remove(MockPolicyVersion.create("a/b/c/1.1"));
        assertEquals(MockPolicyVersion.create("a/b/c/1.0"), MockPolicyVersion.create(service.getPolicyVersion("c", "1")));
    }

    @Test
    void putErrorOnConflict() {
        PolicyVersionService service = new PolicyVersionService(true);
        service.set(MockPolicyVersion.create("a/b/c/1.0"), EntityActivationStatus.ACTIVE);

        MockPolicyVersion conflictingFullNamePolicy = MockPolicyVersion.create("k/m/c/1.0");
        String errorMsg = assertThrows(
                IllegalArgumentException.class,
                () -> service.set(conflictingFullNamePolicy, EntityActivationStatus.ACTIVE)).getMessage();

        assertTrue(errorMsg.contains("a/b/c"));
        assertTrue(errorMsg.contains("k/m/c"));
    }

    @Test
    void removeOk() {
        PolicyVersionService service = new PolicyVersionService(true);
        MockPolicyVersion policy = MockPolicyVersion.create("a/b/c/1.1");
        assertDoesNotThrow(() -> service.remove(policy));
        service.set(policy, EntityActivationStatus.ACTIVE);
        assertEquals(MockPolicyVersion.create("a/b/c/1.1"), MockPolicyVersion.create(service.getPolicyVersion("c", "1")));
        assertDoesNotThrow(() -> service.remove(policy));
        assertNull(service.getPolicyVersion("c", "1"));

        service.set(policy, EntityActivationStatus.ACTIVE);
        MockPolicyVersion conflictPolicy = MockPolicyVersion.create("k/l/c/1.1");
        assertDoesNotThrow(() -> service.remove(conflictPolicy));
        assertEquals(MockPolicyVersion.create("a/b/c/1.1"), MockPolicyVersion.create(service.getPolicyVersion("c", "1")));
    }

    @Test
    void removeOk_ignoreConflictRemoval() {
        PolicyVersionService service = new PolicyVersionService(true);
        MockPolicyVersion policy = MockPolicyVersion.create("a/b/c/1.1");
        service.set(policy, EntityActivationStatus.ACTIVE);
        MockPolicyVersion conflictPolicy = MockPolicyVersion.create("k/l/c/1.1");
        assertDoesNotThrow(() -> service.remove(conflictPolicy));
        assertEquals(MockPolicyVersion.create("a/b/c/1.1"), MockPolicyVersion.create(service.getPolicyVersion("c", "1")));
    }

}
