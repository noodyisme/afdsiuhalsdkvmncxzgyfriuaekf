package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import com.capitalone.identity.identitybuilder.policycore.service.exception.DownstreamException;
import com.capitalone.identity.identitybuilder.policycore.service.exception.ForwardedDownstreamException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Consume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;


@Component
public class PolicyErrorUtil {
    private static final Logger logger = LoggerFactory.getLogger(PolicyErrorUtil.class);
    private static final String DOWNSTREAM_ERROR_INFO = "errorInfo";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Consume("policy-error:forwardDownstreamErrors")
    public void forwardDownstreamErrors(Exception e) throws Exception { //NOSONAR

        logger.info(" Actual ForwardDownstreamError ", e);

        if (e == null)
            throw new IllegalArgumentException("Error: Null exception was passed into policy-error:forwardDownstreamErrors.");

        //proxy pass
        if (e instanceof DownstreamException && !CollectionUtils.isEmpty(((DownstreamException) e).getBody()) &&
                (((DownstreamException) e).getBody().containsKey(DOWNSTREAM_ERROR_INFO))) {

            LinkedHashMap<String, Object> errorInfoMap;
            errorInfoMap = (LinkedHashMap<String, Object>) ((DownstreamException) e).getBody().get(DOWNSTREAM_ERROR_INFO);
            ErrorInfo errorInfo = ErrorInfo.builder()
                    .id(errorInfoMap.containsKey("id") ? String.valueOf(errorInfoMap.get("id")) : null)
                    .text(errorInfoMap.containsKey("text") ? String.valueOf(errorInfoMap.get("text")) : null)
                    .developerText(errorInfoMap.containsKey("developerText") ? String.valueOf(errorInfoMap.get("developerText")) : null)
                    .build();
            if(errorInfoMap.containsKey("additionalDetails")){
                Map<String, String> additionalDetails = objectMapper.convertValue(errorInfoMap.get("additionalDetails"), new TypeReference<>() {
                });
                errorInfo = errorInfo.toBuilder().additionalDetails(additionalDetails).build();
            }
            throw new ForwardedDownstreamException(errorInfo);
        } else {
            throw e;
        }

    }

}