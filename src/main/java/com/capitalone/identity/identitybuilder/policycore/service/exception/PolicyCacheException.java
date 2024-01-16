package com.capitalone.identity.identitybuilder.policycore.service.exception;

import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;

import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import lombok.Getter;

@Getter
public class PolicyCacheException extends ChassisSystemException { // NOSONAR

    public static PolicyCacheException newWriteFailedException(final String resourceId) {
        return newPolicyCacheException(ApplicationConstants.CACHE_WRITE_FAILED_ID, "Cache Write Operation Failed.",
                String.format("Cache Failure: WRITE with resourceId %s", resourceId), resourceId);
    }

    public static PolicyCacheException newReadFailedException(final String resourceId) {
        return newPolicyCacheException(ApplicationConstants.CACHE_READ_FAILED_ID, "Cache Read Operation Failed.",
                String.format("Cache Failure: READ with resourceId %s", resourceId), resourceId);
    }

    public static PolicyCacheException newReadMissException(final String resourceId) {
        return newPolicyCacheException(ApplicationConstants.CACHE_READ_MISS_ID, "Cache Read Miss",
                String.format("Resource id %s is either missing or expired.", resourceId), resourceId);
    }

    private static PolicyCacheException newPolicyCacheException(
            final String id,
            final String text,
            final String developerText,
            final String resourceId) {
        final ChassisErrorCode chassisErrorCode = new ChassisErrorCode(id, id, ApplicationConstants.CACHE_OPERATION_FAILED_DEV_TEXT_ID);
        chassisErrorCode.getDeveloperTextMessageParms().add(resourceId);
        return new PolicyCacheException(chassisErrorCode, text, developerText);
    }

    private final String id;
    private final String text;
    private final String developerText;

    private PolicyCacheException(ChassisErrorCode errorCode, String text, String message) {
        super(errorCode, message);
        this.id = errorCode.getId();
        this.text = text;
        this.developerText = message;
    }

}
