package com.capitalone.identity.identitybuilder.policycore.service.util;


import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.camel.util.RequestParameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author NZB294 - Chandra Sekhar Aadhanapattu
 *
 * Conditional bean injection testing for Crypto based on property value
 *
 */
@SpringBootConfiguration
@ExtendWith(MockitoExtension.class)
public class RequestParameterConditionalCryptoUtilTest {

    @Value("${identity.identitybuilder.policycore.crypto.util.load}")
    private String cryptoUtilLoadFlag;

    private static CryptoUtil cryptoUtilNull;

    @Test
    public void encryptConditionalTest() {
        System.out.println("cryptoUtilLoadFlag::" + cryptoUtilLoadFlag);
        System.out.println("cryptoUtilNull::" + cryptoUtilNull);
        cryptoUtilNull = null;
        RequestParameter requestParameter = new RequestParameter("foo{type=encrypt}");
        assertThrows(RequestValidationException.class, () -> requestParameter.toValue("bar"));
    }

    @Test
    public void aesEncryptConditionalTest() {
        System.out.println("cryptoUtilLoadFlag::" + cryptoUtilLoadFlag);
        System.out.println("cryptoUtilNull::" + cryptoUtilNull);
        cryptoUtilNull = null;
        RequestParameter requestParameter = new RequestParameter("foo{type=aesencrypt}");
        assertThrows(RequestValidationException.class, () -> requestParameter.toValue("bar"));
    }
}
