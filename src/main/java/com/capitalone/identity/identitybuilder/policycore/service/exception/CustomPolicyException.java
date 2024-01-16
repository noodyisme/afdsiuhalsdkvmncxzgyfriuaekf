package com.capitalone.identity.identitybuilder.policycore.service.exception;

import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("java:S5993") //suppress warnings for public constructors on an abstract class.
@Getter
public abstract class CustomPolicyException extends RuntimeException {
    private static final long serialVersionUID = -1L;
    private static final String DOWNSTREAM_ERROR_ID = "202020";
    private static final String DOWNSTREAM_ERROR_TEXT = "Downstream API error";
    private final transient ErrorInfo errorInfo;

    /**
     * Default constructor that takes a message string, and parse to create ErrorInfo
     * message String supports 2 format:
     * 1. ~ delimited string will be parsed as id~text~developerText~additionalDetailsKey1:additionalDetailsValue1~additionalDetailsKey2:additionalDetailsValue2. Fourth token and onward are placed into additionalDetails Map that is : delimited.
     * 2. Message as it is and append ID=202020 and Text = "Downstream API error"
     * @param message message string
     */
    public CustomPolicyException(final String message) {

        super(message);
        if (message != null && message.contains("~")) {
            String[] exceptionMessageArray = message.split("~");
            this.errorInfo =  new ErrorInfo(
                    exceptionMessageArray.length >= 1 ? exceptionMessageArray[0] : null,
                    exceptionMessageArray.length >= 2 ? exceptionMessageArray[1] : null,
                    exceptionMessageArray.length >= 3 ? exceptionMessageArray[2] : null,
                    exceptionMessageArray.length >= 4 ? parseAdditionalDetails(exceptionMessageArray) : null
            );
        } else {
            this.errorInfo = new ErrorInfo(
                    DOWNSTREAM_ERROR_ID,
                    DOWNSTREAM_ERROR_TEXT,
                    message
            );
        }
    }

    private Map<String, String> parseAdditionalDetails(String[] exceptionMessageArray){

        Map<String, String> additionalDetails = new HashMap<>();
        for(int i = 3; i < exceptionMessageArray.length; i++){
            if(StringUtils.isNotBlank(exceptionMessageArray[i]))
            {
                String[] keyValue = exceptionMessageArray[i].split(":");
                additionalDetails.put(keyValue[0], keyValue.length >= 2 ? keyValue[1]: null);
            }
        }
        return additionalDetails;


    }

    public CustomPolicyException(final ErrorInfo errorInfo) {
        super(errorInfo.getText());
        this.errorInfo = errorInfo;
    }

    public String getId() { return errorInfo.getId(); }
    public String getText() { return errorInfo.getText(); }
    public String getDeveloperText() { return errorInfo.getDeveloperText(); }

    public Map<String, String> getAdditionalDetails() {return errorInfo.getAdditionalDetails();}

}
