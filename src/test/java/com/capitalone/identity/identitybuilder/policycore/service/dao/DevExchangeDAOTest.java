package com.capitalone.identity.identitybuilder.policycore.service.dao;

import com.capitalone.chassis.engine.core.health.impl.HealthCheckableDiscoveryService;
import com.capitalone.identity.identitybuilder.policycore.service.dao.impl.DevExchangeDAOImpl;
import com.capitalone.identity.identitybuilder.policycore.service.dao.impl.DevExchangeDAOWebImpl;
import com.capitalone.identity.identitybuilder.policycore.service.dao.impl.DevExchangeRestTemplateFactory;
import com.capitalone.identity.identitybuilder.policycore.service.dao.impl.DevExchangeWebClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ContextConfiguration(classes = {DevExchangeDAOTest.TestConfig.class})
@TestPropertySource(properties = {
        "identity.identitybuilder.policycore.service.webclient.enabled=false",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",
})
public class DevExchangeDAOTest {

    @Autowired
    DevExchangeDAO devExchangeDAO;

//    Verify when the property below is not present or false, the DevExchangeDAO will use the original DevExchangeDAOImpl:
//    identity.identitybuilder.policycore.service.webclient.enabled
    @Test
    public void testImplementation() {
        assertTrue(devExchangeDAO instanceof DevExchangeDAOImpl);
    }

    @Configuration
    @ComponentScan(basePackages = {"com.capitalone.identity.identitybuilder.policycore.service.dao.impl"},
    basePackageClasses = {DevExchangeDAOWebImpl.class, DevExchangeDAOImpl.class})
    public static class TestConfig {
        @MockBean
        DevExchangeRestTemplateFactory devExchangeRestTemplateFactory;
        @MockBean
        DevExchangeWebClientFactory devExchangeWebClientFactory;
        @MockBean
        HealthCheckableDiscoveryService healthCheckableDiscoveryService;
        @MockBean(name = "apiConversionService")
        ConversionService conversionService;
    }

}

