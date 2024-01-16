package com.capitalone.identity.identitybuilder.policycore.feature.prerelease;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Aspect
@Component
public class PreReleaseGuard {

    private final boolean arePreReleaseFeaturesAllowed;

    public static final String PROPERTY_NAME = "identitybuilder.policycore.feature.prerelease.enabled";

    @Inject
    public PreReleaseGuard(@Value("${" + PROPERTY_NAME + ":false}") boolean allowPreReleaseFeatures) {
        this.arePreReleaseFeaturesAllowed = allowPreReleaseFeatures;
    }

    @Before("@within(com.capitalone.identity.identitybuilder.policycore.feature.prerelease.PreRelease) || @annotation(com.capitalone.identity.identitybuilder.policycore.feature.prerelease.PreRelease)")
    public void verifyPreReleaseCallsAllowedOrThrow(JoinPoint joinPoint) {
        if (!arePreReleaseFeaturesAllowed) {
            throw new UnsupportedPreReleaseFeatureUsedException(
                    String.format("Method call to class or method marked @PreRelease are prohibited [class=%s, method=%s]. Set application property %s=true to enable",
                            joinPoint.getTarget().toString(),
                            joinPoint.getSignature().getName(),
                            PROPERTY_NAME)
            );
        }
    }

}
