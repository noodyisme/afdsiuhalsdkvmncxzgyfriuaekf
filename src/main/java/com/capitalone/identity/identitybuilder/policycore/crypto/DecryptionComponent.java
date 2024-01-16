package com.capitalone.identity.identitybuilder.policycore.crypto;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultComponent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * usage <to uri="crypto-cos:decrypt" />
 */
@org.apache.camel.spi.annotations.Component("crypto-cos")
@Log4j2
public class DecryptionComponent extends DefaultComponent {

    private static final String DECRYPT = "decrypt";
    @Autowired
    DeCryptService service;

    @Override
    public Endpoint createEndpoint(String uri, String path, Map<String, Object> parameters) throws Exception {
        if (StringUtils.equalsIgnoreCase(path, DECRYPT)) {
            Endpoint endpoint = new DeCryptEndPoint(uri, this, createProcessor());
            setProperties(endpoint, parameters);
            return endpoint;
        } else {
            throw new IllegalArgumentException("path not matching");
        }
    }

    private Processor createProcessor() {
        return new DeCryptProcessor(service);
    }
}


