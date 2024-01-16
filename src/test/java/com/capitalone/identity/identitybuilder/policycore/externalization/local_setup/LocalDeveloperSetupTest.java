package com.capitalone.identity.identitybuilder.policycore.externalization.local_setup;

import com.capitalone.identity.identitybuilder.model.ScanRequest;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.externalization.EntityUpdateService;
import com.capitalone.identity.identitybuilder.policycore.externalization.dynamic_loading_tests.DynamicUpdateTests;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadEventVisitor;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import com.capitalone.identity.identitybuilder.polling.ScanRequester;
import org.apache.camel.CamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@CamelSpringBootTest
@SpringBootTest
@ContextConfiguration(classes = LocalDeveloperSetupTest.LocalDeveloperSetupConfig.class)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=true",
        "csc.client-environment=prod",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=" + LocalDeveloperSetupTest.POLICY_ROOT,
        "identitybuilder.policycore.feature.version-forwarder.enabled=true",
        "identitybuilder.policycore.abac-enforcement.strict=false",
        "identitybuilder.policycore.feature.strict-policy-start-mode=true",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",
        "chassis.spring.boot.starter.sdp.enabled=false",
        "chassis.spring.boot.starter.DFS.sdp.enabled=false",
        "chassis.spring.boot.starter.native.oath.enabled=false"
})

public class LocalDeveloperSetupTest {
    static final String POLICY_ROOT = "externalization/local_setup";

    @Autowired
    CamelContext camelContext;

    @Autowired
    ScanRequester testScanRequester;

    @Autowired @Qualifier("mockEventLogger")
    EntityLoadEventVisitor eventLogger;

    @BeforeEach
    void setup() {
        camelContext.start();
    }

    /**
     * The purpose of this test is to verify that there are no DynamicUpdate scanning sort order errors
     * when the config store source root is organized like a policy developers local directory structure
     * and not the formalized S3 bucket namespace.
     */
    @Test
    void testLocalFileRepoScansWithoutError() {

        ((DynamicUpdateTests.TestScanRequester) testScanRequester).publisher.tryEmitNext(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));
        verify(eventLogger, times(0)).onNonEntityError(any());
    }

    @ComponentScan(
            basePackages = {
                    "com.capitalone.identity.identitybuilder.client",
                    "com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic",
                    "com.capitalone.identity.identitybuilder.policycore.decisionengine",
                    "com.capitalone.identity.identitybuilder.decisionengine",
                    "com.capitalone.identity.identitybuilder.policycore.externalization",
                    "com.capitalone.identity.identitybuilder.policycore.service.dmn"

            },
            basePackageClasses = { SchemaManager.class, ConfigManagementService.class, PolicyAccessRegistry.class,
                    EntityUpdateService.class, PolicyVersionService.class},
            excludeFilters = @ComponentScan.Filter(value = CamelSpringBootTest.class, type = FilterType.ANNOTATION)
    )
    public static class LocalDeveloperSetupConfig {

        @Bean (name = "mockEventLogger")
        EntityLoadEventVisitor loadEventVisitor() {
            return Mockito.mock(EntityLoadEventVisitor.class);
        }

        @Bean
        protected CamelContext camelContext(ApplicationContext applicationContext) {
            CamelContext camelContext = new SpringCamelContext(applicationContext);
            camelContext.adapt(ModelCamelContext.class)
                    .setDataFormats(Collections.singletonMap("jackson", new JsonDataFormat()));
            camelContext.build();
            return camelContext;
        }

        @Bean
        ScanRequester testScanRequester() {
            return new DynamicUpdateTests.TestScanRequester();
        }
    }
}
