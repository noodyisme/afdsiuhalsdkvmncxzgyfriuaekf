package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty({"identity.identitybuilder.policycore.service.webclient.enabled"})
public class PartnerTokenWebClientGeneratorFactory {


    @Autowired
    DevExchangeWebClientFactory devExchangeWebClientFactory;

    private Map<String, PartnerWebClientTokenGenerator> tokenGeneratorMap = new HashMap<>();

    public PartnerWebClientTokenGenerator getPartnerTokenGenerator(String serviceName) {
        PartnerWebClientTokenGenerator tokenGenerator = tokenGeneratorMap.get(serviceName);
        if(tokenGenerator != null) {
            return tokenGenerator;
        }
        tokenGenerator = new PartnerWebClientTokenGenerator(devExchangeWebClientFactory);
        tokenGeneratorMap.put(serviceName, tokenGenerator);
        return tokenGenerator;
    }
}
