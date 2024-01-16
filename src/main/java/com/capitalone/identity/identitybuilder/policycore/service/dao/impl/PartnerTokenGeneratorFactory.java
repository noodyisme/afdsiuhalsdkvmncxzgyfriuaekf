package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated
 */
@Component
@Deprecated
public class PartnerTokenGeneratorFactory {

    @Autowired
    DevExchangeRestTemplateFactory devExchangeRestTemplateFactory;

    private Map<String, PartnerTokenGenerator> tokenGeneratorMap = new HashMap<>();

    public PartnerTokenGenerator getPartnerTokenGenerator(String serviceName) {
        PartnerTokenGenerator tokenGenerator = tokenGeneratorMap.get(serviceName);
        if(tokenGenerator != null) {
            return tokenGenerator;
        }
        tokenGenerator = new PartnerTokenGenerator(devExchangeRestTemplateFactory);
        tokenGeneratorMap.put(serviceName, tokenGenerator);
        return tokenGenerator;
    }
}
