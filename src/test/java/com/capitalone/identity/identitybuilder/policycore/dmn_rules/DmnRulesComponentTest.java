package com.capitalone.identity.identitybuilder.policycore.dmn_rules;

import com.capitalone.identity.identitybuilder.policycore.dmnrules.DMNRulesCollectProcessor;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.DMNRulesProcessor;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.DmnRulesEndpoint;
import org.apache.camel.*;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CamelSpringBootTest
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = DmnRulesComponentTestConfig.class)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=dev",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=decisionengine/no_op_policy",
})
class DmnRulesComponentTest {

    private static final String URI_BASE = "DmnRulesComponent";

    @Autowired
    private CamelContext context;

    @Test
    void defaultEndpoint() {
        DmnRulesEndpoint endpoint = context.getEndpoint(String.format("%s://legacyParse", URI_BASE), DmnRulesEndpoint.class);
        Processor processor = endpoint.getProcessor();
        assertTrue(processor instanceof DMNRulesProcessor);
        assertFalse(((DMNRulesProcessor) processor).isWriteToBody());
        // changed to true, as remaining (://legacyParse) is present
        assertTrue(((DMNRulesProcessor) processor).isUseLegacyMapResultParsing());
    }

    @Test
    void writeToBodyEndpoint() {
        DmnRulesEndpoint endpoint = context.getEndpoint(String.format("%s:writeToBody", URI_BASE), DmnRulesEndpoint.class);
        Processor processor = endpoint.getProcessor();
        assertTrue(processor instanceof DMNRulesProcessor);
        assertTrue(((DMNRulesProcessor) processor).isWriteToBody());
        assertFalse(((DMNRulesProcessor) processor).isUseLegacyMapResultParsing());
    }

    @Test
    void collectEndpoint() {
        DmnRulesEndpoint endpoint = context.getEndpoint(String.format("%s:collectToBody", URI_BASE), DmnRulesEndpoint.class);
        assertTrue(endpoint.getProcessor() instanceof DMNRulesCollectProcessor);
    }

    @Test
    void collectLegacyThrowsError() {
        assertThrows(ResolveEndpointFailedException.class,
                () -> context.getEndpoint(URI_BASE + ":legacyParse:collectToBody"));
    }

    @Test
    void unknownEndpointThrowsError() {
        String uri = URI_BASE + ":" + UUID.randomUUID().toString();
        assertThrows(ResolveEndpointFailedException.class, () -> context.getEndpoint(uri));
    }

    /**
     * TODO: delete when audit endpoints are fully removed
     */
    @Test
    void auditEndpointThrowsNoError() {
        DmnRulesEndpoint endpoint = context.getEndpoint(String.format("%s:audit", URI_BASE), DmnRulesEndpoint.class);
        assertFalse(endpoint.getProcessor() instanceof DMNRulesCollectProcessor);
    }


    @Test
    void legacyMapEndpoint() {
        List<String> legacyEndpoints = Arrays.asList(
                URI_BASE + ":legacyParse",
                URI_BASE + ":legacyParse:writeToBody"
        );

        for (String endpoint : legacyEndpoints) {
            DmnRulesEndpoint legacyEndpoing = context.getEndpoint(endpoint, DmnRulesEndpoint.class);
            Processor processor = legacyEndpoing.getProcessor();
            assertTrue(processor instanceof DMNRulesProcessor);
            assertTrue(((DMNRulesProcessor) processor).isUseLegacyMapResultParsing());
        }

    }

}
