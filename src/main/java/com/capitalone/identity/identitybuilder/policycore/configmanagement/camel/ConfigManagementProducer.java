package com.capitalone.identity.identitybuilder.policycore.configmanagement.camel;


import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.UnsupportedBusinessEventException;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Objects;

public class ConfigManagementProducer extends DefaultProducer {

    final ConfigManagementModel configManagementModel;
    final String paramKey;
    final String headerOutput;

    public ConfigManagementProducer(@Nullable ConfigManagementEndpoint endpoint,
                                    @NonNull ConfigManagementModel configManagementModel,
                                    @NonNull String configPropertyKey, @NonNull String headerOutput) {
        super(endpoint);

        this.paramKey = Objects.requireNonNull(configPropertyKey);
        this.headerOutput = Objects.requireNonNull(headerOutput);
        this.configManagementModel = Objects.requireNonNull(configManagementModel);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (!configManagementModel.isValidKey(paramKey)) {
            throw new UnsupportedOperationException("invalid configuration management key:=" + paramKey);
        }
    }

    @Override
    public void process(Exchange exchange) {
        String useCase = (String) exchange.getMessage().getHeader(PolicyConstants.HEADER_BUSINESS_EVENT);
        if (useCase != null && !configManagementModel.isUseCaseDefined(useCase) &&
                !configManagementModel.isAppLevelUseCaseDefined(useCase)) {
            String msg = String.format(
                    "Use-case not defined for policy [useCase=%s, policy=%s, policyVersionPatch=%s]", useCase,
                    exchange.getMessage().getHeader(PolicyConstants.HEADER_POLICYNAME),
                    exchange.getMessage().getHeader(PolicyConstants.HEADER_POLICYVERSION_PATCH));
            throw new UnsupportedBusinessEventException(msg);
        }

        exchange.getMessage().setHeader(
                headerOutput,
                configManagementModel.getValueOrThrow(paramKey, useCase));
    }

}
