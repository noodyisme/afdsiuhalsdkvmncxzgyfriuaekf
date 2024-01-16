package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * see {@link RuntimeUpdateEventLogger}
 */
@Value
public class LoggedResult {

    static LoggedResult newFromTransaction(@NonNull UpdateTransactionResult result) {
        return new LoggedResult(result.getStatus(),
                result.getError() == null ? null : ExceptionUtils.getStackTrace(result.getError()));
    }

    @NonNull UpdateTransactionResult.Status status;
    String error;

}
