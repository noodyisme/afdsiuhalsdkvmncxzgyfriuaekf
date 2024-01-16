package com.capitalone.identity.identitybuilder.policycore.feature.prerelease;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
        TestPreReleaseInterfaceImpl.class,
        TestPreReleaseClass.class,
        TestPreReleaseMethod.class,
        PreReleaseGuard.class,
        AnnotationAwareAspectJAutoProxyCreator.class,
})
class PreReleaseGuardPreventTest {

    @SpyBean
    PreReleaseGuard guard;

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
        RuntimeException exception = assertThrows(UnsupportedPreReleaseFeatureUsedException.class,
                () -> preReleaseInterface.testMethod());
        assertTrue(exception.getMessage().contains("testMethod"));
        assertTrue(exception.getMessage().contains("TestPreReleaseInterfaceImpl"), exception.getMessage());


    }

    @Test
    void testPrereleaseAnnotatedClassCallFails() {
        assertThrows(UnsupportedPreReleaseFeatureUsedException.class, () -> preReleaseClass.testMethod());
        RuntimeException exception = assertThrows(UnsupportedPreReleaseFeatureUsedException.class,
                () -> preReleaseClass.testMethod());
        assertTrue(exception.getMessage().contains("testMethod"));
        assertTrue(exception.getMessage().contains("TestPreReleaseClass"), exception.getMessage());
    }

    @Test
    void testPrereleaseAnnotatedMethodCallFails() {
        assertThrows(UnsupportedPreReleaseFeatureUsedException.class, () -> preReleaseMethod.testMethod());
        RuntimeException exception = assertThrows(UnsupportedPreReleaseFeatureUsedException.class,
                () -> preReleaseMethod.testMethod());
        assertTrue(exception.getMessage().contains("testMethod"));
        assertTrue(exception.getMessage().contains("TestPreReleaseMethod"), exception.getMessage());
    }

}
