package com.capitalone.identity.identitybuilder.policycore.externalization.dynamic_loading_tests;

import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.feature.prerelease.PreReleaseGuard;
import com.capitalone.identity.identitybuilder.policycore.feature.property.ConfigManagementPropertySetter;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@ContextConfiguration(classes = {
        StartupTestConfig.class,
        PreReleaseGuard.class,
        ConfigManagementPropertySetter.class,
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "camel.springboot.routes-include-pattern=classpath:externalization/dynamic_loading/isolation_test_policy/serviceResourceDirectory/*.xml",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=qa",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=externalization/dynamic_loading/isolation_test_policy",
        "identitybuilder.policycore.feature.version-forwarder.enabled=false",
        "identitybuilder.policycore.abac-enforcement.strict=false",
        "identitybuilder.policycore.feature.strict-policy-start-mode=true",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",

})
public class IsolationTests {

    private static final Pattern compile = Pattern.compile("^[-_a-zA-Z]+:[-_a-zA-Z]+(?<patchVersion>\\d+\\.\\d+\\.\\d+).*$");

    private static String getPatchVersionFromEndpoint(String endpoint) {
        Matcher matcher = compile.matcher(endpoint);
        assertTrue(matcher.matches());
        return matcher.group("patchVersion");
    }

    @EndpointInject("mock:end")
    MockEndpoint mockEnd;

    @Autowired
    ProducerTemplate producer;

    @Autowired
    CamelContext camelContext;

    public static List<String> getPublicEntrypoints() {
        return Arrays.asList(
                "policy:isolation_test_policy_1.0.0?timeout=500",
                "policy:isolation_test_policy_1.0.0-step2?timeout=500",
                "direct:isolation_test_policy_1.0.0-schema?timeout=500",
                "direct:isolation_test_policy_1.0.0-step2-schema?timeout=500",
                "policy:isolation_test_policy_1.1.0?timeout=500",
                "policy:isolation_test_policy_1.1.0-step2?timeout=500",
                "direct:isolation_test_policy_1.1.0-schema?timeout=500",
                "direct:isolation_test_policy_1.1.0-step2-schema?timeout=500");
    }


    @BeforeEach
    void setup() {
        camelContext.start();
    }

    @ParameterizedTest
    @MethodSource("getPublicEntrypoints")
    void testConfigurationManagementIsolation(String endpoint) {
        String patchValue = getPatchVersionFromEndpoint(endpoint);
        Object config = getResultFromEndpoint(endpoint, "test.config.iso");
        assertEquals(String.format("version %s customer.code is: default-%s", patchValue, patchValue), config);
    }

    @ParameterizedTest
    @MethodSource("getPublicEntrypoints")
    void testConfigurationManagementFeaturePropertyPlaceholderIsolation(String endpoint) {
        String patchValue = getPatchVersionFromEndpoint(endpoint);
        Object config = getResultFromEndpoint(endpoint, "test.config.feature.iso");
        String expected = String.format("version %s test-feature-property-iso [property=feature-%s, config=feature-%s]",
                patchValue, patchValue, patchValue);
        assertEquals(expected, config);
    }

    @ParameterizedTest
    @MethodSource("getPublicEntrypoints")
    void testDmnIsolation(String endpoint) {
        String patchValue = getPatchVersionFromEndpoint(endpoint);

        Object result = getResultFromEndpoint(endpoint, "test.dmn.iso");

        // dmn
        Map<String, String> dmn = (Map<String, String>) result;
        assertEquals(patchValue, dmn.get("policy"));
        assertEquals(patchValue + "-1", dmn.get("decisionNode1"));
        assertEquals(patchValue + "-2", dmn.get("decisionNode2"));
    }


    @ParameterizedTest
    @MethodSource("getPublicEntrypoints")
    void testEnrichIsolation(String endpoint) {
        String patchValue = getPatchVersionFromEndpoint(endpoint);

        Object result = getResultFromEndpoint(endpoint, "test.enrich.iso");

        // enrich
        Map<String, String> enrich = (Map<String, String>) result;
        assertEquals(patchValue, enrich.get("versionHeader"));
        assertEquals("isolation_test_policy", enrich.get("policyHeader"));

        // enrich w/ public schema route
        result = getResultFromEndpoint(endpoint, "test.enrichPublic.iso");
        Map<String, String> enrich2 = (Map<String, String>) result;
        assertEquals("isolation_test_policy-stepNone-schema", enrich2.get("policy"));
        assertEquals(patchValue, enrich2.get("version"));
    }

    @ParameterizedTest
    @MethodSource("getPublicEntrypoints")
    void testServiceBundledIsolation(String endpoint) {
        String patchValue = getPatchVersionFromEndpoint(endpoint);

        Object result = getResultFromEndpoint(endpoint, "test.serviceBundled.iso");

        // service route
        assertEquals(String.format("policy %s Hello Route Bundled With Service!", patchValue), result);

    }

    /**
     * This is a heavyweight test because pollEnrich dirties the context. Limiting to two cases.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "policy:isolation_test_policy_1.0.0?timeout=500",
            "policy:isolation_test_policy_1.1.0?timeout=500",})
    @DirtiesContext
    @SneakyThrows
    void testPollEnrichIsolation(String endpoint) {

        Object result = getResultFromEndpoint(endpoint, "test.pollEnrich.iso");

        // pollingEnrich (File) asserts
        File assert_1_1_file = new File("src/test/resources/externalization/dynamic_loading/isolation_test_policy/serviceResourceDirectory/pollEnrichTestFile.txt");
        GenericFile body3 = (GenericFile) result;
        assertTrue(FileUtils.contentEquals((File) body3.getFile(), assert_1_1_file));

    }

    private Object getResultFromEndpoint(String endpoint, String businessEvent) {

        String patchValue = getPatchVersionFromEndpoint(endpoint);

        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "isolation_test_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION_PATCH, patchValue);
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, businessEvent);

        int start = mockEnd.getReceivedCounter();
        producer.sendBodyAndHeaders(endpoint, new HashMap<>(), headers);
        int end = mockEnd.getReceivedCounter();
        List<Object> results = mockEnd.getReceivedExchanges().subList(start, end).stream().map(exchange -> exchange.getMessage().getBody()).collect(Collectors.toList());
        return results.get(0);
    }

}
