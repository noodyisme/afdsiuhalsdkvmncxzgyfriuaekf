package com.capitalone.identity.identitybuilder.policycore.configmanagement;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ConfigManagementService {

    private final Map<String, ConfigManagementModel> configurationMap = new HashMap<>();

    public void setPolicyConfiguration(@NonNull String identifier, @Nullable ConfigManagementModel configuration) {
        if (configuration != null) configurationMap.put(identifier, configuration);
    }

    public ConfigManagementModel getPolicyConfiguration(String identifier) {
        return configurationMap.get(identifier);
    }

    public ConfigManagementModel deletePolicyConfiguration(@NonNull String identifier) {
        return configurationMap.remove(identifier);
    }

}
