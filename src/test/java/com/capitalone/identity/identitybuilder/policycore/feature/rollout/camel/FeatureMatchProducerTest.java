package com.capitalone.identity.identitybuilder.policycore.feature.rollout.camel;

import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.FailedToCreateRouteException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;

import static com.capitalone.identity.identitybuilder.policycore.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootContextAwareTest(
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.feature.rollout.*"))
@UseAdviceWith
class FeatureMatchProducerTest {
    public static final String MATCH_DATA_PARAMETER_ROUTE = "feature-match:match?dataParameter=inputHeaderX&outputHeaderName=outputHeaderNameY";
    public static final String MATCH_DATA_PARAMETER_GLOBAL_OVERRIDE_FORMAT = "feature-match:match?globalOverride=%s&dataParameter=inputHeaderX&outputHeaderName=outputHeaderNameY";
    private static final String TEST_RUNTIME_DATA_INPUT_HEADER_KEY = "inputHeaderX";
    private static final String TEST_LOADTIME_DATA_INPUT_HEADER_KEY = "inputHeaderZ";
    private static final String TEST_DATA_OUTPUT_HEADER_KEY = "outputHeaderNameY";

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @NotNull
    private Exchange initializeTestExchange(String destinationRoute) throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(destinationRoute)
        );
        camelContext.start();
        return new DefaultExchange(camelContext);
    }

    private Object parseTestInputObject(String rawInputParam) throws JsonProcessingException {
        if (rawInputParam == null) return rawInputParam;
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(rawInputParam, Object.class);
    }

    @Test
    @DirtiesContext
    void testUnknownInputClassShouldThrowException() throws Exception {
        Exchange exchange = initializeTestExchange(MATCH_DATA_PARAMETER_ROUTE);
        exchange.getIn().setHeader(TEST_RUNTIME_DATA_INPUT_HEADER_KEY, new Object());

        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        IllegalArgumentException exception = processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT, IllegalArgumentException.class);
        assertNotNull(exception);
        assertNull(processedExchange.getMessage().getHeader(TEST_DATA_OUTPUT_HEADER_KEY, String.class));
        assertTrue(exception.getMessage().contains("must be a map or list of maps"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"input\": \"test\"}",
            "[{\"input\": \"test\"}]",
            "{\"criteria\": \"list::foo,bar,baz\"}",
            "{\"input\": \"test\", \"criteria\": null}",
            "{\"input\": \"test\", \"criteria\": \"xyz::foo,bar,baz\"}",
    })
    @DirtiesContext
    void testRouteWithMalformedRuntimeInputDateParameterShouldThrowException(String testInputRaw) throws Exception {
        Object testInputParam = parseTestInputObject(testInputRaw);
        Exchange exchange = initializeTestExchange(MATCH_DATA_PARAMETER_ROUTE);
        exchange.getIn().setHeader(TEST_RUNTIME_DATA_INPUT_HEADER_KEY, testInputParam);

        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        IllegalArgumentException exception = processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT, IllegalArgumentException.class);
        assertNotNull(exception);
        assertNull(processedExchange.getMessage().getHeader(TEST_DATA_OUTPUT_HEADER_KEY, String.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"input\": \"foo\", \"criteria\": \"list::foo,bar,baz\"}",
            // blank input passes if criteria allows
            "{\"input\": \"\", \"criteria\": \"regex::^()$\"}",
            "{\"input\": \"01\", \"criteria\": \"preformatted::50-percentRollout\"}",
    })
    @DirtiesContext
    void testRouteProcessesRuntimeInputsExpectedTrueResult(String testInputRaw) throws Exception {
        Object testInputParam = parseTestInputObject(testInputRaw);
        Exchange exchange = initializeTestExchange(MATCH_DATA_PARAMETER_ROUTE);
        exchange.getIn().setHeader(TEST_RUNTIME_DATA_INPUT_HEADER_KEY, testInputParam);

        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        assertNull(processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT));
        Boolean result = exchange.getIn().getHeader(TEST_DATA_OUTPUT_HEADER_KEY, Boolean.class);
        assertNotNull(result);
        assertTrue(result);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "[]",
            "[{\"input\": \"fo\", \"criteria\": \"list::foo,bar,baz\"}]",
            "{\"input\": \"fo\", \"criteria\": \"list::foo,bar,baz\"}",
            "[{\"input\": \"foo\", \"criteria\": \"list::foo,bar,baz\"}, {\"input\": \"fo\", \"criteria\": \"list::foo,bar,baz\"}]",
            "[{\"input\": \"fo\", \"criteria\": \"list::foo,bar,baz\"}, {\"input\": \"foo\", \"criteria\": \"list::foo,bar,baz\"}]",
            // null input evaluates to false
            "[{\"input\": \"foo\", \"criteria\": \"list::foo,bar,baz\"}, {\"input\": null, \"criteria\": \"list::foo,bar,baz\"}]",
            "{\"input\": null, \"criteria\": \"regex::^()$\"}",
            "{\"input\": null, \"criteria\": \"preformatted::match-all\"}",
    })
    @NullSource
    @DirtiesContext
    void testRouteProcessesInputsExpectedFalseResult(String testInputRaw) throws Exception {
        Object testInputParam = parseTestInputObject(testInputRaw);
        Exchange exchange = initializeTestExchange(MATCH_DATA_PARAMETER_ROUTE);
        exchange.getIn().setHeader(TEST_RUNTIME_DATA_INPUT_HEADER_KEY, testInputParam);

        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        assertNull(processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT));
        Boolean result = exchange.getIn().getHeader(TEST_DATA_OUTPUT_HEADER_KEY, Boolean.class);
        assertNotNull(result);
        assertFalse(result);
    }

    @ParameterizedTest
    @CsvSource({
            "inputSrc=inputHeaderZ&criteria=list::foo,foo,true",
            "inputSrc=inputHeaderZ&criteria=list::foo,bar,false",
            "inputSrc=inputHeaderZ&criteria=list::bar&inputSrc=inputHeaderZ&criteria=regex::^(bar)$,bar,true",
            "inputSrc=inputHeaderZ&criteria=list::bar&inputSrc=inputHeaderZ&criteria=regex::^(bar)$,bar,true",
            "inputSrc=inputHeaderZ&criteria=list::bar&inputSrc=inputHeaderZ&criteria=list::foo&globalOverride=DISABLED,bar,false",
            "inputSrc=inputHeaderZ&criteria=list::bar&inputSrc=inputHeaderZ&criteria=list::foo,bar,false",
            "inputSrc=inputHeaderZ&criteria=list::bar&inputSrc=inputHeaderZ&criteria=list::foo&globalOverride=ALWAYS_ALLOWED,bar,true",
            "inputSrc=inputHeaderZ&criteria=preformatted::match-all,bar,true",
            "inputSrc=inputHeaderZ&criteria=preformatted::5-percentRollout,01,true",
            "inputSrc=inputHeaderZ&criteria=preformatted::5-percentRollout,99,false",
            "dataParameter=inputHeaderY,baz,false",
            "dataParameter=inputHeaderY&inputSrc=inputHeaderZ&criteria=list::bar,baz,false",
    })
    @DirtiesContext
    void testRouteProcessesLoadTimeCriteriaExpectedResult(String query, String input, Boolean expectedMatch) throws Exception {
        String testRoute = "feature-match:match?" + query + "&outputHeaderName=" + TEST_DATA_OUTPUT_HEADER_KEY;
        Exchange exchange = initializeTestExchange(testRoute);
        exchange.getIn().setHeader(TEST_LOADTIME_DATA_INPUT_HEADER_KEY, input);

        String runtimeInput = "[{\"input\": \"" + input + "\", \"criteria\": \"regex::^(baz)$\"}]";
        exchange.getIn().setHeader(TEST_RUNTIME_DATA_INPUT_HEADER_KEY, runtimeInput);

        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        assertNull(processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT));
        Boolean result = exchange.getIn().getHeader(TEST_DATA_OUTPUT_HEADER_KEY, Boolean.class);
        assertNotNull(result);
        assertEquals(expectedMatch, result);
    }

    @ParameterizedTest
    @CsvSource({
            "foo,list::bar,ALWAYS_ALLOWED,true",
            "foo,list::foo,ALWAYS_ALLOWED,true",
            "foo,malformed::bar,ALWAYS_ALLOWED,true",
            "foo,list::bar,DISABLED,false",
            "foo,list::foo,DISABLED,false",
            "foo,malformed::foo,DISABLED,false",
    })
    @DirtiesContext
    void testGlobalOverride(String input, String rawCriteria, String globalOverride, Boolean expectedResult) throws Exception {
        Map<String, String> objInput = new HashMap<String, String>() {{
            put("input", input);
            put("criteria", rawCriteria);
        }};

        Exchange exchange = initializeTestExchange(String.format(MATCH_DATA_PARAMETER_GLOBAL_OVERRIDE_FORMAT, globalOverride));
        exchange.getIn().setHeader(TEST_RUNTIME_DATA_INPUT_HEADER_KEY, objInput);

        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        assertNull(processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT));

        Boolean result = exchange.getIn().getHeader(TEST_DATA_OUTPUT_HEADER_KEY, Boolean.class);
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "feature-match:match?inputSrc=headerX&outputHeaderName=headerXYZ",
            "feature-match:match?inputSrc=headerY&criteria=list::foo,bar&inputSrc=headerY&outputHeaderName=headerXYZ",
            "feature-match:match?criteria=list::foo,bar&outputHeaderName=headerXYZ",
            "feature-match:match?inputSrc=headerY&criteria=list::foo,bar&criteria=list::bar&outputHeaderName=headerXYZ",
    })
    @DirtiesContext
    void testEndpointWithUnbalancedMatchParamsFailsCreation(String testInput) {
        String msg = assertThrows(FailedToCreateRouteException.class, () -> initializeTestExchange(testInput)).getMessage();
        assertTrue(msg.contains("count of 'inputSrc' params"));
        assertTrue(msg.contains("does not match count of 'criteria' params"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "feature-match:23132141?outputHeaderName=headerXYZ",
            "feature-match://match",
            "feature-match:match?globalOverride=ALWAYS_ALLOWED&globalOverride=DISABLED",
    })
    @DirtiesContext
    void testEndpointWithInvalidSignatureFailsCreation(String testInput) {
        String msg = assertThrows(FailedToCreateRouteException.class, () -> initializeTestExchange(testInput)).getMessage();
        assertTrue(msg.contains(testInput.substring(testInput.indexOf(":"))));
    }

    @Configuration
    public static class ContextConfig {

        @SneakyThrows
        @Bean
        public FeatureMatchComponent featureComponent() {
            return new FeatureMatchComponent();
        }

        @SneakyThrows
        @Bean
        public CamelContext createCamelContext(ApplicationContext applicationContext) {
            CamelContext camelContext = new SpringCamelContext(applicationContext);
            camelContext.addComponent("feature-match", featureComponent());
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from(DIRECT_START).routeId(TEST_ROUTE)
                            .process(Exchange::getIn).id(PROCESSOR_NODE)
                            .to(DIRECT_END);
                }
            });
            camelContext.start();
            return camelContext;
        }

        @Bean
        public ProducerTemplate producerTemplate(CamelContext camelContext) {
            return camelContext.createProducerTemplate();
        }
    }

}
