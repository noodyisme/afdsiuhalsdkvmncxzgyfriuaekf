package com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel;

import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import lombok.Getter;

@Getter
public class PolicyFailureException extends RuntimeException {
    public final String id;
    public final String text;
    public final String developerText;

    public PolicyFailureException(ErrorInfo info) {
        super(String.format("ErrorInfo[id='%s', text='%s', developerText='%s']",
                info.getId(), info.getText(), info.getDeveloperText()));
        id = info.getId();
        text = info.getText();
        developerText = info.getDeveloperText();
    }
}
