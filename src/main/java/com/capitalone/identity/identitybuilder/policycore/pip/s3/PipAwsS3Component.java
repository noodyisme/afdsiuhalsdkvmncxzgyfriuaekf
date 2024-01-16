package com.capitalone.identity.identitybuilder.policycore.pip.s3;


import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("pip-aws-s3")
public class PipAwsS3Component extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        return getCamelContext().getComponent("aws2-s3").createEndpoint(uri, parameters);
    }
}
