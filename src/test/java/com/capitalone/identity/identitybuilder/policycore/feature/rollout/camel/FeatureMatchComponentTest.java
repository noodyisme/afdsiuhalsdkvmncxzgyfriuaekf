package com.capitalone.identity.identitybuilder.policycore.feature.rollout.camel;

import com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria.GlobalOverride;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.util.URISupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootContextAwareTest(
        contextConfigClasses = FeatureMatchComponentTest.ContextConfig.class,
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.feature.rollout.*"))
class FeatureMatchComponentTest {

    @Autowired
    FeatureMatchComponent component;

    @Autowired
    CamelContext camelContext;

    @Test
    void testMinimalEndpointSignature() throws Exception {
        String testInput = "feature-match://match?outputHeaderName=headerXYZ";
        URI uri = URI.create(testInput);
        Map<String, Object> stringObjectMap = URISupport.parseParameters(uri);
        String remaining = uri.getHost();

        FeatureMatchEndpoint endpoint = (FeatureMatchEndpoint) component.createEndpoint(testInput, remaining, stringObjectMap);
        assertEquals("headerXYZ", endpoint.getOutputHeaderName());
        assertEquals(GlobalOverride.DEFAULT, endpoint.getGlobalOverride());


        assertThrows(IllegalArgumentException.class, () -> endpoint.setInputSrc(""));
        assertThrows(IllegalArgumentException.class, () -> endpoint.setInputSrc((String) null));

        assertThrows(IllegalArgumentException.class, () -> endpoint.setOutputHeaderName(""));
        assertThrows(IllegalArgumentException.class, () -> endpoint.setOutputHeaderName(null));

        assertThrows(IllegalArgumentException.class, () -> endpoint.setDataParameter(""));
        assertThrows(IllegalArgumentException.class, () -> endpoint.setDataParameter(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "feature-match://match?dataParameter=headerABC&outputHeaderName=headerXYZ",
            // whitespace testing
            "feature-match://match?dataParameter=headerABC++++&outputHeaderName=headerXYZ",
            "feature-match://match?dataParameter=++headerABC&outputHeaderName=++headerXYZ++++",
            // global override w/ whitespace test
            "feature-match://match?dataParameter=headerABC&outputHeaderName=headerXYZ&globalOverride=++DEFAULT++",
    })
    void testEndpointWithRuntimeMatchArguments(String testInput) throws Exception {
        URI uri = URI.create(testInput);
        Map<String, Object> stringObjectMap = URISupport.parseParameters(uri);
        String remaining = uri.getHost();

        FeatureMatchEndpoint endpoint = (FeatureMatchEndpoint) component.createEndpoint(testInput, remaining, stringObjectMap);
        assertEquals("headerABC", endpoint.getDataParameter());
        assertEquals("headerXYZ", endpoint.getOutputHeaderName());
        assertEquals(GlobalOverride.DEFAULT, endpoint.getGlobalOverride());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "feature-match://match?inputSrc=headerXYZ&criteria=list::test&outputHeaderName=headerXYZ",
            // whitespace test
            "feature-match://match?inputSrc=++headerXYZ++&criteria=++list::test++&outputHeaderName=headerXYZ",
            // global override w/ whitespace test
            "feature-match://match?inputSrc=++headerXYZ++&criteria=++list::test++&outputHeaderName=headerXYZ&globalOverride=++DEFAULT++",
    })
    void testEndpointWithLoadTimeMatchArguments(String testInput) throws Exception {
        URI uri = URI.create(testInput);
        Map<String, Object> stringObjectMap = URISupport.parseParameters(uri);
        String remaining = uri.getHost();

        FeatureMatchEndpoint endpoint = (FeatureMatchEndpoint) component.createEndpoint(testInput, remaining, stringObjectMap);
        List<MatchArgumentHolder> matchParameters = endpoint.getQueryBasedMatchParameters();

        assertNotNull(matchParameters);
        assertFalse(matchParameters.isEmpty());
        assertEquals("headerXYZ", matchParameters.get(0).getInput());
        assertTrue(matchParameters.get(0).getCriteria().isMatch("test"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "feature-match://match?globalOverride=ALWAYS_ALLOWED&outputHeaderName=headerXYZ",
            "feature-match://match?globalOverride=DISABLED&outputHeaderName=headerXYZ",
            "feature-match://match?globalOverride=DEFAULT&outputHeaderName=headerXYZ",
            "feature-match://match?globalOverride=DEFAULT&outputHeaderName=headerXYZ",
    })
    void testEndpointWithGlobalOverrideParam(String testInput) throws Exception {
        URI uri = URI.create(testInput);
        Map<String, Object> stringObjectMap = URISupport.parseParameters(uri);
        Object expectedGlobalOverride = stringObjectMap.get("globalOverride");
        String remaining = uri.getHost();

        FeatureMatchEndpoint endpoint = (FeatureMatchEndpoint) component.createEndpoint(testInput, remaining, stringObjectMap);
        assertEquals(expectedGlobalOverride, endpoint.getGlobalOverride().toString());
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "feature-match://match?inputSrc=headerX&criteria=list::foo,bar&outputHeaderName=headerXYZ",
            "feature-match://match?inputSrc=headerX&criteria=list::foo,bar&inputSrc=headerY&criteria=list::bar&outputHeaderName=headerXYZ",
    })
    void testEndpointWithInputOutputSet(String testInput) throws Exception {
        URI uri = URI.create(testInput);
        Map<String, Object> stringObjectMap = URISupport.parseParameters(uri);
        String remaining = uri.getHost();

        FeatureMatchEndpoint endpoint = (FeatureMatchEndpoint) component.createEndpoint(testInput, remaining, stringObjectMap);
        MatchArgumentHolder resultArgument = endpoint.getQueryBasedMatchParameters().get(0);
        assertEquals("headerX", resultArgument.getInput());
        assertTrue(resultArgument.getCriteria().isMatch("bar"));
    }

    @Configuration
    public static class ContextConfig {

        @Bean
        FeatureMatchComponent featureMatchComponent() {
            return new FeatureMatchComponent();
        }

        @Bean
        public CamelContext createCamelContext(ApplicationContext applicationContext) {
            CamelContext camelContext = new SpringCamelContext(applicationContext);
            camelContext.addComponent("feature-match", featureMatchComponent());
            camelContext.start();
            return camelContext;
        }
    }
}
