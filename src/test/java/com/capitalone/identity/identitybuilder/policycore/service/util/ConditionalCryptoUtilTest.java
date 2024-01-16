package com.capitalone.identity.identitybuilder.policycore.service.util;

import com.capitalone.api.security.CryptoSerializerDeserializer;
import com.capitalone.dsd.utilities.crypto.lib.pki.PKIEncryption;
import com.capitalone.dsd.utilities.crypto.lib.pkiaes.PKIAESEncryption;
import com.capitalone.identity.identitybuilder.policycore.camel.util.RequestParameter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author NZB294 - Chandra Sekhar Aadhanapattu
 *
 * This test class is intended flip the property dynalically for unit testing
 *
 */

public class ConditionalCryptoUtilTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConditionEvaluationReportLoggingListener())  // to print out conditional config report to log
            .withUserConfiguration(TestConfig.class)                          // satisfy transitive dependencies of beans being tested
            .withUserConfiguration(CryptoUtil.class, RequestParameter.class);

    @Test
    public void encryptDynamicTest() {
        contextRunner
                .withPropertyValues("identity.identitybuilder.policycore.crypto.util.load=true")  // here is the changing part (property value)
                .run(context -> assertAll(
                        () -> Assertions.assertThat(context).hasSingleBean(CryptoUtil.class),
                        () -> assertEquals(null, new RequestParameter("foo{type=encrypt}").toValue("bar123"))));
    }

    @Test
    public void aesEncryptDynamicTest() {
        contextRunner
                .withPropertyValues("identity.identitybuilder.policycore.crypto.util.load=true")  // here is the changing part (property value)
                .run(context -> assertAll(
                        () -> Assertions.assertThat(context).hasSingleBean(CryptoUtil.class),
                        () -> assertEquals(null, new RequestParameter("foo{type=aesencrypt}").toValue("bar123"))));
    }


    @Configuration
    // this annotation is not required here as the class is explicitly mentioned in `withUserConfiguration` method
    protected static class TestConfig {

        @Bean
        public PKIEncryption pkiEncryption() throws IOException {
            return Mockito.mock(PKIEncryption.class);   // this bean will be automatically autowired into tested beans
        }

        @Bean
        public PKIAESEncryption pkiAesEncryption() {
            return Mockito.mock(PKIAESEncryption.class);   // this bean will be automatically autowired into tested beans
        }
        @Bean(name = "defaultCryptoSerializerDeserializer")
        public CryptoSerializerDeserializer crypto() {
            return Mockito.mock(CryptoSerializerDeserializer.class);   // this bean will be automatically autowired into tested beans
        }
    }
}
