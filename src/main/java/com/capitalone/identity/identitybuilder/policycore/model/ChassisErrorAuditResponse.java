package com.capitalone.identity.identitybuilder.policycore.model;

import com.capitalone.chassis.engine.model.error.ErrorDetail;
import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Error response for failure to execute policy (or step). Returned by POST/PATCH methods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class ChassisErrorAuditResponse {
    @NonNull
    private String id;
    @NonNull
    private String text;
    @NonNull
    private String developerText;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> actions = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ErrorDetail> errorDetails;

    public ChassisErrorAuditResponse(ErrorResponse errorResponse) {
        this.id = errorResponse.getId();
        this.text = errorResponse.getText();
        this.developerText = errorResponse.getDeveloperText();
        this.actions = errorResponse.getActions();
        this.errorDetails = errorResponse.getErrorDetails();
    }
}
