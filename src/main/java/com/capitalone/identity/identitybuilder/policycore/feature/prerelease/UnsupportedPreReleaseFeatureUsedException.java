package com.capitalone.identity.identitybuilder.policycore.feature.prerelease;

/**
 * @see PreRelease
 */
public final class UnsupportedPreReleaseFeatureUsedException extends UnsupportedOperationException {

    public UnsupportedPreReleaseFeatureUsedException(String message) {
        super(message);
    }
}
