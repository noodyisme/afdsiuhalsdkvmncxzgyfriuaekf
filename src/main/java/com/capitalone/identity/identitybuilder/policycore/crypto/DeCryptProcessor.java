package com.capitalone.identity.identitybuilder.policycore.crypto;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.Map;

import static com.capitalone.identity.identitybuilder.policycore.crypto.DecryptUtil.createError;
import static java.util.Optional.ofNullable;

/**
 * invoke in a route using
 * <CryptoComponent:execute>
 */
@Log4j2
public class DeCryptProcessor implements Processor {
    private final DeCryptService service;

    public DeCryptProcessor(DeCryptService service) {
        this.service = service;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String encryptedData = ofNullable(exchange.getIn().getBody(String.class)).orElseThrow(createError(
                "encryptedString must be required"));
        Map<String, Object> headers = ofNullable(exchange.getIn().getHeaders()).orElseThrow(createError(
                "headers must be present but [null] found"));
        Map<String, String> cosMap = (Map<String, String>) ofNullable(headers.get("decrypt.cosMap")).orElseThrow(
                createError("headers must contains decrypt.cosMap, but [null] found"));

        // derive kid value and remove from argument
        COSRequestProperties config = COSRequestProperties.builder()
                .lockBoxId(ofNullable(cosMap.get("lockBoxId")).orElseThrow(createError(
                        "lockBoxId is required, but [null] found")))
                .productId(ofNullable(cosMap.get("productId")).orElseThrow(createError(
                        "productId is required, but [null] found")))
                .vaultRole(ofNullable(cosMap.get("vaultRole")).orElseThrow(createError(
                        "vaultRole is required, but [null] found")))
                .vaultAddress(ofNullable(cosMap.get("vaultAddress")).orElseThrow(createError(
                        "vaultAddress is required, but [null] " + "found")))
                .build();

        log.info("cosMap config : {}", config);

        String decryptedMessage = service.decrypt(encryptedData, config);
        exchange.getIn().setBody(decryptedMessage);
    }
}
