package com.capitalone.identity.identitybuilder.policycore.externalization.dynamic_loading_tests;

import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.feature.prerelease.PreReleaseGuard;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@ContextConfiguration(classes = {
        StartupTestConfig.class,
        PreReleaseGuard.class,
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=qa",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=" + WireTapRouteTest.POLICY_ROOT,
        "identitybuilder.policycore.feature.version-forwarder.enabled=false",
        "identitybuilder.policycore.abac-enforcement.strict=false",
        "identitybuilder.policycore.feature.strict-policy-start-mode=true",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",

})
public class WireTapRouteTest {
    static final String POLICY_ROOT = "externalization/dynamic_loading/wiretap_test_policy";

    @Autowired
    ProducerTemplate producer;

    // Mock endpoints for direct and wiretap routes
    @EndpointInject("mock:result")
    private MockEndpoint mockResult;
    @EndpointInject("mock:wiretapRoute")
    private MockEndpoint mockWireTapRoute;

    @Autowired
    CamelContext camelContext;

    @Test
    void testCamelRouteWithWiretap() throws InterruptedException {
        camelContext.start();
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "wiretap_test_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION_PATCH, "1.0.0");

        int start = mockResult.getReceivedCounter();
        int startWireTap = mockWireTapRoute.getReceivedCounter();
        producer.sendBodyAndHeaders("policy:wiretap_test_policy_1.0.0?timeout=500", new HashMap<>(), headers);
        Thread.sleep(1000);
        int end = mockResult.getReceivedCounter();
        int endWireTap = mockWireTapRoute.getReceivedCounter();

        // Verify both direct and wiretap routes invoked
        assertEquals(0, start);
        assertEquals(0, startWireTap);
        assertEquals(1, end);
        assertEquals(1, endWireTap);
    }


}
