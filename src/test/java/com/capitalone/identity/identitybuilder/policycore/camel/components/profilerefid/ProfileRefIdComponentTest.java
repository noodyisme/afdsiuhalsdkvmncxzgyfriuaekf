package com.capitalone.identity.identitybuilder.policycore.camel.components.profilerefid;

import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.capitalone.identity.identitybuilder.policycore.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@UseAdviceWith
@CamelSpringBootContextAwareTest(
        contextConfigClasses = ProfileRefIdComponentTest.ContextConfig.class,
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.camel.components.profilerefid.*"))
public class ProfileRefIdComponentTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProfileRefIdComponent profileRefIdComponent;

    @ParameterizedTest
    @ValueSource(strings = {"cof-profile-ref-id:process", "cof-profile-ref-id:Extract"})
    void testUnsupportedConsumer(final String uri) {
        camelContext.start();
        Exception exception = assertThrows(FailedToCreateRouteException.class, () -> AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.replaceFromWith(uri)));
        assertEquals(ResolveEndpointFailedException.class, exception.getCause().getClass());
    }

    @Test
    void testValidEndpoint() throws Exception {
        camelContext.start();
        Map<String, Object> parameters = new HashMap<>();
        ProfileRefIdEndpoint profileRefIdEndpoint = (ProfileRefIdEndpoint) profileRefIdComponent.createEndpoint(PROFILE_REF_ID, "extract", parameters);
        assertEquals(ProfileRefIdOperation.valueOf("extract".toUpperCase(Locale.ROOT)), profileRefIdEndpoint.getProfileRefIdOperation());
    }

    @Configuration
    public static class ContextConfig {

        @Bean
        public ProfileRefIdComponent profileRefId() {
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
            camelContext.addComponent(PROFILE_REF_ID, profileRefId());
            return camelContext;
        }

        @Bean
        protected ProducerTemplate producerTemplate() throws Exception {
            return createCamelContext().createProducerTemplate();
        }

    }

}
