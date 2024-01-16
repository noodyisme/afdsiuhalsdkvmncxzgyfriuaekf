package com.capitalone.identity.identitybuilder.policycore.camel.components.profilerefid;

import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.annotation.DirtiesContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.capitalone.identity.identitybuilder.policycore.camel.components.profilerefid.ProfileRefIdOperation.EXTRACT;
import static com.capitalone.identity.identitybuilder.policycore.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@UseAdviceWith
@CamelSpringBootContextAwareTest(
        contextConfigClasses = ProfileRefIdProducerTest.ContextConfig.class,
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.camel.components.profilerefid.*"))
public class ProfileRefIdProducerTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    @DirtiesContext
    void testWithoutSrcProfileRefIdHeaderSet() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to("cof-profile-ref-id:extract")
        );

        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        Exchange responseExchange = producerTemplate.send(DIRECT_START, exchange);

        IllegalArgumentException exception = responseExchange.getProperty(Exchange.EXCEPTION_CAUGHT, IllegalArgumentException.class);
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("required camel header 'srcProfileRefId' missing"));
    }

    @DirtiesContext
    @ParameterizedTest
    @MethodSource("profileRefIds")
    void testRouteWithProfileRefId(final String profileRefId, final String ssoId) throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to("cof-profile-ref-id:extract")
        );

        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setHeader("srcProfileRefId", profileRefId);
        Exchange responseExchange = producerTemplate.send(DIRECT_START, exchange);

        final ProfileRefIdEndpoint profileRefIdEndpoint = camelContext.getEndpoint("cof-profile-ref-id:extract", ProfileRefIdEndpoint.class);

        assertEquals(EXTRACT, profileRefIdEndpoint.getProfileRefIdOperation());
        assertEquals(ssoId, responseExchange.getIn().getHeader("profileRefIdExtractedFields", Map.class).get("ssoId"));
    }

    static Stream<Arguments> profileRefIds() {
        return Stream.of(
                arguments("dfeb1d8c4c011e37dc72464dc331eae1", "dfeb1d8c4c011e37dc72464dc331eae1"),
                arguments("", null),
                arguments("ssoId=dfeb1d8c4c011e37dc72464dc331eae1", "dfeb1d8c4c011e37dc72464dc331eae1"),
                arguments("ssoId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("ssoId=", null),
                arguments("ssoId= ", null),
                arguments("escId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("escId=dfeb1d8c4c011e37dc72464dc331eae1", "dfeb1d8c4c011e37dc72464dc331eae1"),
                arguments("escId=", null),
                arguments("escId= ", null),
                arguments("id=dfeb1d8c4c011e37dc72464dc331eae1", null),
                arguments("profileType=LGIN~~sorId=49~~ssoId=dfeb1d8c4c011e37dc72464dc331eae1", "dfeb1d8c4c011e37dc72464dc331eae1"),
                arguments("profileType=LGIN~~sorId=49~~ssoId=dfeb1d8c4c.011e37dc72464dc331eae1", null),
                arguments("profileType=LGIN~~sorId=abc~~ssoId=dfeb1d8c4c011e37dc72464dc331eae1", "dfeb1d8c4c011e37dc72464dc331eae1"),
                arguments("profileType=LGIN~~sorId=49~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=financial~~sorId=WALMART~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=financial~~sorId=WALMART~~ssoId=", null),
                arguments("profileType=financial~~sorId=WALMART~~ssoId= ", null),
                arguments("profleType=financial~~sorId=WALMART~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=financial~sorId=WALMART~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=nonfinancial~~sorId=WALMART~~escId=dfeb1d8c4c011e37dc72464dc331eae1", "dfeb1d8c4c011e37dc72464dc331eae1"),
                arguments("profileType=nonfinancial~~sorId=WALMART~~escId=", null),
                arguments("profileType=nonfinancial~~sorId=WALMART~~escId= ", null),
                arguments("profileType=nonfinancial~~sorId=WALMART~~esccId=dfeb1d8c4c011e37dc72464dc331eae1", null),
                arguments("profileType=nonfinancial~~sorId=WALMART~escId=dfeb1d8c4c011e37dc72464dc331eae1", null),
                arguments("profileType=LGIN~~sorId=49~~escId=dfeb1d8c4c.011e37dc72464dc331eae1", null),
                arguments("profileType=LGIN~~sorId=abc~~escId=dfeb1d8c4c011e37dc72464dc331eae1", "dfeb1d8c4c011e37dc72464dc331eae1"),
                arguments("profileType=LGIN~~sorId=49~~escId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("accountId=12583112397~~sorId=185", null),
                arguments("accountId=10002128052~~firstSix=529107~~lastFour=9781~~sorId=7", null),
                arguments("906802cf2349da1a611208f8ed5fc4ed", "906802cf2349da1a611208f8ed5fc4ed"),
                arguments("profileType=LGIN~~escId=13281c4d270b85b85ed3d7a0d0cf08b8~~sorId=49", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8~~sorId=49", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("escId=13281c4d270b85b85ed3d7a0d0cf08b8~~profileType=LGIN~~sorId=49", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("ssoId=13281c4d270b85b85ed3d7a0d0cf08b8~~profileType=LGIN~~sorId=49", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("sorId=49~~escId=13281c4d270b85b85ed3d7a0d0cf08b8~~profileType=LGIN", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("sorId=49~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8~~profileType=LGIN", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("sorId=49~~profileType=LGIN~~escId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("sorId=WALMART~~profileType=financial~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~escId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8~~", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~escId=13281c4d270b85b85ed3d7a0d0cf08b8~~", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8~~", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("~~profileType=LGIN~~escId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("~~profileType=LGIN~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~escId=13281c4d270b85b85ed3d7a0d0cf08b8~~sorId=49~~someOtherField=helloworld", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8~~sorId=49~~someOtherField=helloworld", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("escId=13281c4d270b85b85ed3d7a0d0cf08b8~~ssoId=dfeb1d8c4c011e37dc72464dc331eae1", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~~~escId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8"),
                arguments("profileType=LGIN~~~~ssoId=13281c4d270b85b85ed3d7a0d0cf08b8", "13281c4d270b85b85ed3d7a0d0cf08b8")
        );
    }

    @Configuration
    public static class ContextConfig {

        @Bean
        public ProfileRefIdComponent profileRefIdComponent() {
            return new ProfileRefIdComponent();
        }

        public RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(DIRECT_START).routeId(TEST_ROUTE)
                            .process(Exchange::getIn).id(PROCESSOR_NODE)
                            .to(DIRECT_END);
                }
            };
        }

        @Bean
        protected CamelContext createCamelContext() throws Exception {
            CamelContext camelContext = new DefaultCamelContext();
            camelContext.addRoutes(route());
            camelContext.addComponent(PROFILE_REF_ID, profileRefIdComponent());
            return camelContext;
        }

        @Bean
        protected ProducerTemplate producerTemplate() throws Exception {
            return createCamelContext().createProducerTemplate();
        }
    }

}
