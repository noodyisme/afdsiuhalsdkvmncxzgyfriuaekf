package com.capitalone.identity.identitybuilder.policycore.configmanagement.camel;

import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@org.apache.camel.spi.annotations.Component("policy-configuration")
public class ConfigManagementComponent extends DefaultComponent {

    @Autowired
    ConfigManagementService configManagementService;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) {
        return new ConfigManagementEndpoint(uri, this, configManagementService);
    }
}
