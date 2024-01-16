package com.capitalone.identity.identitybuilder.policycore.policymode;

import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import lombok.AllArgsConstructor;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("policy-mode")
@AllArgsConstructor
public class PolicyModeComponent extends DefaultComponent {

    private final ConfigManagementService configManagementService;

    @Value("${identitybuilder.policycore.feature.mock-mode.enabled: false}")
    private final boolean mockFeatureEnabled;

    @Value("${ENVIRONMENT_NAME:UNK}")
    private final String environmentName;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) {
        return new PolicyModeEndpoint(uri, this, configManagementService, mockFeatureEnabled, environmentName);
    }
}
