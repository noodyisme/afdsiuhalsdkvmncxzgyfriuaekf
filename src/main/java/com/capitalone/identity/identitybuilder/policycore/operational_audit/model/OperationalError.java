package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.Optional;

@Value
@Builder (toBuilder = true)
public class OperationalError {

    @JsonProperty("description")
    String text;

    @JsonProperty("developer_text")
    String developerText;

    @JsonProperty("error_id")
    String errorId;

    @JsonProperty("stack_trace")
    String stackTrace;

    /**
     * Convenience method to convert throwable to OperationalError if throwable non-null, otherwise
     * returns null.
     * @param throwable throwable
     * @return OperationalError (or null)
     */
    public static OperationalError of(final Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return OperationalAuditUtil.mergeThrowableInfo(
                OperationalError.builder().build(),
                Optional.of(throwable));
    }
}
