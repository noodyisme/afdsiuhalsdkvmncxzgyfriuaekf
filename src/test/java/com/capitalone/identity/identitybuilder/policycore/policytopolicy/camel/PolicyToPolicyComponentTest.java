package com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel;

import com.capitalone.api.security.AuthenticationResponseMarshaller;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.PolicyValidator;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventLogger;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventSystemLogger;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyResponse;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyStatus;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.PolicyEvaluatedAuditor;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyServiceExecutor;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import org.apache.camel.*;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.Serializable;
import java.util.*;

import static com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil.CACHE_KEY;
import static com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService.FEATURE_FLAG_VERSION_FORWARDER_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@CamelSpringBootTest
@SpringBootApplication
@ContextConfiguration(classes = {
        PolicyToPolicyComponentTest.StartupTestConfig.class
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=dev",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=policytopolicy",
        FEATURE_FLAG_VERSION_FORWARDER_KEY + "=false",
        "identitybuilder.policycore.feature.abac-enforcement.enabled=true",
        "policy.service.logs.fieldstofilter=none",
        "chassis.app.name=masterbuilder-2-0",
        "chassis.app.code=test",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",
})
class PolicyToPolicyComponentTest {

    @Autowired
    CamelContext camelContext;
    @EndpointInject("policy:parent_policy_A_0.0.0?timeout=500")
    ProducerTemplate producer;
    @EndpointInject("policy:child_policy_B_0.0.0?timeout=500")
    ProducerTemplate producerB;
    @EndpointInject("mock:end")
    MockEndpoint mockEnd;
    @MockBean
    PolicyServiceExecutor policyServiceExecutor;
    @MockBean
    PolicyValidator policyValidator;
    @MockBean
    PolicyVersionService policyVersionService;
    @MockBean
    PolicyEvaluatedAuditor policyEvaluatedAuditor;
    @Mock
    Message message;
    @Mock
    PolicyService.ExecutedPolicyResponse executedPolicyResponse;

    @BeforeEach
    void setup() {
        camelContext.start();
    }

    @Test
    void createEndpoint() throws Exception {
        String uri = "P2P://tenant/businessUnit/policyName/policyVersion";
        String remaining = "tenant/businessUnit/policyName/policyVersion";

        PolicyToPolicyComponent policy = new PolicyToPolicyComponent();
        PolicyToPolicyEndpoint endpoint = (PolicyToPolicyEndpoint) policy.createEndpoint(uri, remaining, new HashMap<>());

        assertEquals(uri, endpoint.getEndpointUri());
        assertEquals("tenant", endpoint.getTenant());
        assertEquals("businessUnit", endpoint.getBusinessUnit());
        assertEquals("policyName", endpoint.getPolicyName());
        assertEquals("policyVersion", endpoint.getPolicyVersion());
    }

    @Test
    void invalidEndpoint() throws Exception {
        String uri = "P2P://tenant/businessUnit/policyName";
        String remaining = "tenant/businessUnit/policyName";

        PolicyToPolicyComponent policy = new PolicyToPolicyComponent();
        Throwable throwable = assertThrows(PolicyToPolicyException.class, () -> policy.createEndpoint(uri, remaining, new HashMap<>()));
        assertEquals("The P2P component must use the full policy name followed by either a major or minor " +
                "version. The syntax is \"P2P:tenant/businessUnit/policyName/policyVersion\"", throwable.getMessage());
    }

    @Test
    @DirtiesContext
    void sendPolicyToPolicyRequest() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("policyVersion", "1.0");
        exchange.setProperty("policyName", "test");
        exchange.setIn(message);
        when(message.getHeader(PolicyConstants.HEADER_BUSINESS_EVENT)).thenReturn("testing");
        PolicyInfo.Patch minor = getMockPolicyInfo("name", new ConfigStoreItemInfo("O/b/c/1.0/process/a.xml", "a"));
        when(policyVersionService.getPolicyVersion(anyString(), anyString())).thenReturn(minor);
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "parent_policy_A");
        headers.put(PolicyConstants.HEADER_POLICYVERSION_PATCH, "0.0.0");
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("x-journey-policy", "test");
        Set<String> childPolicySet = new HashSet<>();
        String name = "policy:name_1.0.0";
        childPolicySet.add(name);
        when(policyValidator.getChildPolicies()).thenReturn(childPolicySet);
        when(message.getHeader(PolicyConstants.HEADER_DXHEADERS)).thenReturn(stringMap);
        when(message.getBody()).thenReturn(stringMap);
        when(message.getHeader(CACHE_KEY)).thenReturn("test");
        when(executedPolicyResponse.getResult()).thenReturn(new PolicyService.Result.PolicySuccess(PolicyResponse.builder().policyStatus(PolicyStatus.SUCCESS).results(stringMap).build()));
        when(policyServiceExecutor.executePolicyWithAuditing(any(), any())).thenReturn(executedPolicyResponse);
        producer.send(exchange);
        Map<Serializable, Serializable> result = (Map<Serializable, Serializable>) mockEnd.getReceivedExchanges().get(0).getMessage().getBody();
        assertEquals(stringMap, result);
    }

    @Test
    @DirtiesContext
    void sendPolicyToPolicyError() {
        Exchange exchange = new DefaultExchange(camelContext);
        Exchange result = producer.send(exchange);
        assertNotNull(result.getException());
        assertEquals(PolicyToPolicyException.class, result.getException().getClass());
    }

    @Test
    @DirtiesContext
    void callChildPolicyDirectly() {
        Exchange exchange = new DefaultExchange(camelContext);
        producerB.send(exchange);
        String result = (String) mockEnd.getReceivedExchanges().get(0).getMessage().getBody();
        assertEquals("testing", result);
    }

    @Configuration
    @ComponentScan(
            basePackages = {
                    "com.capitalone.identity.identitybuilder.client",
                    "com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic",
            },
            basePackageClasses = {SchemaManager.class, ConfigManagementService.class}
    )
    public static class StartupTestConfig {

        @Bean
        AuthenticationResponseMarshaller myResponseMarshaller() {
            return (httpServletRequest, httpServletResponse, o) -> "";
        }

        @Bean
        RuntimeUpdateEventLogger testLogger() {
            return new RuntimeUpdateEventSystemLogger();
        }

        @Bean
        OperationalAuditor operationalAuditor() {
            return new OperationalAuditor(null);
        }

    }

    private EntityInfo.Policy getMockPolicyInfo(String path, ConfigStoreItemInfo... items) {
        PolicyDefinition mock = new PolicyDefinition(path, "1.0");
        return new EntityInfo.Policy(mock, new HashSet<>(Arrays.asList(items)));
    }
}