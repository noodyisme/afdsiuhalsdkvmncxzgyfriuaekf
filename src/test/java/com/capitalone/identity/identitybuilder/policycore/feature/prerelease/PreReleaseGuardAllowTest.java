package com.capitalone.identity.identitybuilder.policycore.feature.prerelease;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
        PreReleaseGuard.class,
        TestPreReleaseInterfaceImpl.class,
        TestPreReleaseClass.class,
        TestPreReleaseMethod.class,
        AnnotationAwareAspectJAutoProxyCreator.class,
}, properties = {
        "identitybuilder.policycore.feature.prerelease.enabled=true",
})
class PreReleaseGuardAllowTest {

    @SpyBean
    private PreReleaseGuard guard;

    @Autowired
    private TestPreReleaseInterface preReleaseInterface;

    @Autowired
    private TestPreReleaseMethod preReleaseMethod;

    @Autowired
    private TestPreReleaseClass preReleaseClass;

    @Test
    void testGuardInjected() {
        assertNotNull(guard);
    }

    @Test
    void testPrereleaseAnnotatedInterfaceImplCallFails() {
        assertDoesNotThrow(() -> preReleaseInterface.testMethod());
        verify(guard, times(1)).verifyPreReleaseCallsAllowedOrThrow(any());
    }

    @Test
    void testPrereleaseAnnotatedClassCallFails() {
        assertDoesNotThrow(() -> preReleaseClass.testMethod());
    }

    @Test
    void testPrereleaseAnnotatedMethodCallFails() {
        assertDoesNotThrow(() -> preReleaseMethod.testMethod());
    }

}
