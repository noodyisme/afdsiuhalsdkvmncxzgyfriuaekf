package com.capitalone.identity.identitybuilder.policycore.abac.service;

import com.capitalone.identity.identitybuilder.model.abac.AccessGrant;
import com.capitalone.identity.identitybuilder.model.abac.PolicyAccess;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import com.capitalone.identity.identitybuilder.policycore.abac.model.AbacResponse;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthAction;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthResource;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyAuthorizationEnforcerTest {

    private PolicyAuthResource definedPolicyResource = null;
    private PolicyAuthResource undefinedPolicyResource = null;
    private PolicyAuthSubject allowedPolicyAuthSubject = null;
    private PolicyAuthSubject deniedPolicyAuthSubject = null;
    private PolicyAuthSubject nonExistingPolicyAuthSubject = null;

    @Mock
    private PolicyAccess policyAccess;

    @Mock
    private PolicyAccessRegistry policyAccessRegistry;

    private PolicyAuthorizationEnforcer authorizationEnforcer;

    @BeforeEach
    void setup() {
        // Strict enforcement is enabled
        authorizationEnforcer = new PolicyAuthorizationEnforcer(false, policyAccessRegistry);
        ReflectionTestUtils.setField(authorizationEnforcer, "strictEnforcement", true);

        definedPolicyResource = new PolicyAuthResource("one_second_policy_api", "1.0");
        undefinedPolicyResource = new PolicyAuthResource("sample_undefined_policy", "1.0");
        allowedPolicyAuthSubject = new PolicyAuthSubject("11223345");
        deniedPolicyAuthSubject = new PolicyAuthSubject("11223346");
        nonExistingPolicyAuthSubject = new PolicyAuthSubject("112233445566");

        lenient().when(policyAccess.getAccess("11223345")).thenReturn(AccessGrant.ALLOW);
        lenient().when(policyAccess.getAccess("11223346")).thenReturn(AccessGrant.DENY);
        lenient().when(policyAccess.getAccess("112233445566")).thenReturn(AccessGrant.UNDEFINED);

        ReflectionTestUtils.setField(authorizationEnforcer, "applicationId", "test");
    }

    @Test
    void allowedSubjectForResourceTest() {
        when(policyAccessRegistry.getPolicyAccess("one_second_policy_api", 1)).thenReturn(policyAccess);
        AbacResponse abacResponse = authorizationEnforcer.authorize(allowedPolicyAuthSubject, definedPolicyResource, PolicyAuthAction.INITIATE);
        assertTrue(abacResponse.isAuthorized());
    }

    @Test
    void resourceAccessUndefinedWithSoftEnforcement() {
        ReflectionTestUtils.setField(authorizationEnforcer, "strictEnforcement", false);
        when(policyAccessRegistry.getPolicyAccess("sample_undefined_policy", 1)).thenReturn(null);
        AbacResponse abacResponse = authorizationEnforcer.authorize(allowedPolicyAuthSubject, undefinedPolicyResource, PolicyAuthAction.INITIATE);
        assertTrue(abacResponse.isAuthorized());
        assertEquals("401001", abacResponse.getErrorCode());
    }

    @Test
    void subjectDoesNotExistTestWithSoftEnforcement() {
        ReflectionTestUtils.setField(authorizationEnforcer, "strictEnforcement", false);
        when(policyAccessRegistry.getPolicyAccess("one_second_policy_api", 1)).thenReturn(policyAccess);
        AbacResponse abacResponse = authorizationEnforcer.authorize(nonExistingPolicyAuthSubject, definedPolicyResource, PolicyAuthAction.INITIATE);
        assertTrue(abacResponse.isAuthorized());
        assertEquals("401002", abacResponse.getErrorCode());
    }

    @Test
    void subjectExistsButNotPermittedWithSoftEnforcement() {
        ReflectionTestUtils.setField(authorizationEnforcer, "strictEnforcement", false);
        when(policyAccessRegistry.getPolicyAccess("one_second_policy_api", 1)).thenReturn(policyAccess);
        AbacResponse abacResponse = authorizationEnforcer.authorize(deniedPolicyAuthSubject, definedPolicyResource, PolicyAuthAction.INITIATE);
        assertTrue(abacResponse.isAuthorized());
        assertEquals("401003", abacResponse.getErrorCode());
    }


    @Test
    void resourceAccessUndefined() {
        when(policyAccessRegistry.getPolicyAccess("sample_undefined_policy", 1)).thenReturn(null);
        AbacResponse abacResponse = authorizationEnforcer.authorize(allowedPolicyAuthSubject, undefinedPolicyResource, PolicyAuthAction.INITIATE);
        assertFalse(abacResponse.isAuthorized());
        assertEquals("401001", abacResponse.getErrorCode());
    }

    @Test
    void subjectDoesNotExistTest() {
        when(policyAccessRegistry.getPolicyAccess("one_second_policy_api", 1)).thenReturn(policyAccess);
        AbacResponse abacResponse = authorizationEnforcer.authorize(nonExistingPolicyAuthSubject, definedPolicyResource, PolicyAuthAction.INITIATE);
        assertFalse(abacResponse.isAuthorized());
        assertEquals("401002", abacResponse.getErrorCode());
    }

    @Test
    void subjectExistsButNotPermitted() {
        when(policyAccessRegistry.getPolicyAccess("one_second_policy_api", 1)).thenReturn(policyAccess);
        AbacResponse abacResponse = authorizationEnforcer.authorize(deniedPolicyAuthSubject, definedPolicyResource, PolicyAuthAction.INITIATE);
        assertFalse(abacResponse.isAuthorized());
        assertEquals("401003", abacResponse.getErrorCode());
    }

    @Test
    void validateAbacForGetRequests() {
        AbacResponse abacResponse = authorizationEnforcer.authorize(deniedPolicyAuthSubject, definedPolicyResource, PolicyAuthAction.GET);
        assertTrue(abacResponse.isAuthorized());
    }

    @Test()
    void providingNullResourceTest() {
        assertThrows(NullPointerException.class, () -> {
            authorizationEnforcer.authorize(allowedPolicyAuthSubject, null, PolicyAuthAction.GET);
        });
    }

    @Test()
    void providingNullSubjectTest() {
        assertThrows(NullPointerException.class, () -> {
            authorizationEnforcer.authorize(null, definedPolicyResource, PolicyAuthAction.GET);
        });
    }

    @Test()
    void providingNullActionTest() {
        assertThrows(NullPointerException.class, () -> {
            authorizationEnforcer.authorize(allowedPolicyAuthSubject, definedPolicyResource, null);
        });
    }
}
