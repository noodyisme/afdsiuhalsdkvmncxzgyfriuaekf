package com.capitalone.identity.identitybuilder.policycore.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ErrorInfo implements Serializable {
    private static final long serialVersionUID = -1158204410809749457L;

    private String id;
    private String text;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String developerText;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String,String> additionalDetails;

    public ErrorInfo(String id, String text, String developerText){
        this.id = id;
        this.text = text;
        this.developerText = developerText;
    }

}
