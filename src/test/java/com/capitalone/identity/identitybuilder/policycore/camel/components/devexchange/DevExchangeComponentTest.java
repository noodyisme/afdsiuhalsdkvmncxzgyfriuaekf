package com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.service.dao.DevExchangeDAO;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.util.URISupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@SuppressWarnings("serial")
@CamelSpringBootContextAwareTest(
        contextConfigClasses = DevExchangeComponentTest.ContextConfigCamel.class,
        excludeFromComponentScan = @Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange.*"))
public class DevExchangeComponentTest {
    private static final String RESPONSE_BODY = "{\"key\": \"value\"}";

    @Autowired
    CamelContext camelContext;
    @EndpointInject("mock:result")
    private MockEndpoint mockEndpoint;

    private static DevExchangeComponent devExchangeComponent;

    @BeforeAll
    public static void setComponents() {
        devExchangeComponent = new DevExchangeComponent();
    }

    @BeforeEach
    public void resetComponents() {
        devExchangeComponent.setMockBaseUrl("https://www.amazon.com");
        // Set environment Name and Mock Feature flag before each test since they are modified by testMockFeatureDisabledInProd
        devExchangeComponent.setMockFeatureEnabled(true);
        devExchangeComponent.setEnvironmentName("test");
    }

    @BeforeEach
    void setup() {
        mockEndpoint = camelContext.getEndpoint("mock:result", MockEndpoint.class);
    }
    @Test
    public void testWithParameters() throws Exception {
    	// Example parameters:
    	// uri= dx://deviceConfidence:https://heimstubs-api-ite.clouddqt.capitalone.com/heimstubs-web/identity/profiles/enhanced-validation?dxVersion=3
    	// remaining= deviceConfidence:https://heimstubs-api-ite.clouddqt.capitalone.com/heimstubs-web/identity/profiles/enhanced-validation

    	// Extra parameters not known to the endpont.
    	Map<String, Object> extraParameters = new HashMap<>();
    	extraParameters.put("bogusParam1", "bogusValue1");
    	extraParameters.put("bogusParam2", "bogusValue2");

    	// All the parameters passed to the endpoint
    	Map<String, Object> parameters = new HashMap<>();
    	parameters.put("method", "get");
    	parameters.putAll(extraParameters);

    	DevExchangeEndpoint endpoint = (DevExchangeEndpoint) devExchangeComponent.createEndpoint(
    			"dx://serviceName:https://www.google.com?method=get&bogusParam1=bogusValue1&bogusParam2=bogusValue2",
    			"serviceName:https://www.google.com",
    			parameters);
    	assertEquals("serviceName", endpoint.getServiceName());
    	assertEquals("https://www.google.com", endpoint.getTargetUrl());
        assertEquals("https://www.amazon.com", endpoint.getMockTargetUrl());

    	// The query parameters known to the endpoint should be processed and the remaining
    	// parameters set appropriately.
    	assertEquals("get", endpoint.getMethod().toLowerCase());
    	assertSame(parameters, endpoint.getParameters());
    	assertEquals(extraParameters, endpoint.getParameters());
    	// Check the query string but convert it to a map so it isn't order dependent.
    	assertEquals(extraParameters, URISupport.parseQuery(endpoint.getQueryParameters()));
    }

    @Test
    public void testWithPathInMockBaseUrl() throws Exception {
        // Set with path in mockBaseUrl
        devExchangeComponent.setMockBaseUrl("https://www.amazon.com/with/path/");

        // Extra parameters not known to the endpont.
        Map<String, Object> extraParameters = new HashMap<>();
        extraParameters.put("bogusParam3", "bogusValue3");
        extraParameters.put("bogusParam4", "bogusValue4");

        // All the parameters passed to the endpoint
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("method", "get");
        parameters.putAll(extraParameters);

        DevExchangeEndpoint endpoint = (DevExchangeEndpoint) devExchangeComponent.createEndpoint(
                "dx://serviceName:https://www.google.com?method=get&bogusParam3=bogusValue3&bogusParam4=bogusValue4",
                "serviceName:https://www.google.com",
                parameters);
        assertEquals("serviceName", endpoint.getServiceName());
        assertEquals("https://www.google.com", endpoint.getTargetUrl());
        assertEquals("https://www.amazon.com/with/path/", endpoint.getMockTargetUrl());

        // The query parameters known to the endpoint should be processed and the remaining
        // parameters set appropriately.
        assertEquals("get", endpoint.getMethod().toLowerCase());
        assertSame(parameters, endpoint.getParameters());
        assertEquals(extraParameters, endpoint.getParameters());
        // Check the query string but convert it to a map so it isn't order dependent.
        assertEquals(extraParameters, URISupport.parseQuery(endpoint.getQueryParameters()));
    }

    @Test
    public void testWithUrlSubstitutionFeature() throws Exception {

        // Extra parameters not known to the endpont.
        Map<String, Object> extraParameters = new HashMap<>();
        extraParameters.put("bogusParam5", "bogusValue5");
        extraParameters.put("bogusParam6", "bogusValue6");

        // All the parameters passed to the endpoint
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("method", "get");
        parameters.putAll(extraParameters);

        DevExchangeEndpoint endpoint = (DevExchangeEndpoint) devExchangeComponent.createEndpoint(
                "dx://serviceName:https://www.google.com/path/with/{substitution}?method=get&bogusParam5=bogusValue5&bogusParam6=bogusValue6",
                "serviceName:https://www.google.com/path/with/{substitution}",
                parameters);
        assertEquals("serviceName", endpoint.getServiceName());
        assertEquals("https://www.google.com/path/with/{substitution}", endpoint.getTargetUrl());
        assertEquals("https://www.amazon.com/path/with/{substitution}", endpoint.getMockTargetUrl());

        // The query parameters known to the endpoint should be processed and the remaining
        // parameters set appropriately.
        assertEquals("get", endpoint.getMethod().toLowerCase());
        assertSame(parameters, endpoint.getParameters());
        assertEquals(extraParameters, endpoint.getParameters());
        // Check the query string but convert it to a map so it isn't order dependent.
        assertEquals(extraParameters, URISupport.parseQuery(endpoint.getQueryParameters()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "prod",
            "Prod",
            "PROD",
            "production",
            "prod-env",
            "env_prod",
            " prod "
    })
    void testMockFeatureDisabledInProd(String environmentName) throws Exception {
        // Example parameters:
        // uri= dx://deviceConfidence:https://heimstubs-api-ite.clouddqt.capitalone.com/heimstubs-web/identity/profiles/enhanced-validation?dxVersion=3
        // remaining= deviceConfidence:https://heimstubs-api-ite.clouddqt.capitalone.com/heimstubs-web/identity/profiles/enhanced-validation

        // Extra parameters not known to the endpont.
        Map<String, Object> extraParameters = new HashMap<>();
        extraParameters.put("bogusParam1", "bogusValue1");
        extraParameters.put("bogusParam2", "bogusValue2");

        // All the parameters passed to the endpoint
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("method", "get");
        parameters.putAll(extraParameters);

        devExchangeComponent.setEnvironmentName(environmentName);
        assertFalse(devExchangeComponent.isMockFeatureEnabled());

        DevExchangeEndpoint endpoint = (DevExchangeEndpoint) devExchangeComponent.createEndpoint(
                "dx://serviceName:https://www.google.com?method=get&bogusParam1=bogusValue1&bogusParam2=bogusValue2",
                "serviceName:https://www.google.com",
                parameters);
        assertEquals("serviceName", endpoint.getServiceName());
        assertEquals("https://www.google.com", endpoint.getTargetUrl());
        assertEquals("https://www.google.com", endpoint.getMockTargetUrl());

        // The query parameters known to the endpoint should be processed and the remaining
        // parameters set appropriately.
        assertEquals("get", endpoint.getMethod().toLowerCase());
        assertSame(parameters, endpoint.getParameters());
        assertEquals(extraParameters, endpoint.getParameters());
        // Check the query string but convert it to a map so it isn't order dependent.
        assertEquals(extraParameters, URISupport.parseQuery(endpoint.getQueryParameters()));
    }

    @Test
    public void testPdxEndpointWithPartnerAccessTokenUri() throws Exception {
        // uri= dx://pdx:https://heimstubs-api-ite.clouddqt.capitalone.com/heimstubs-web/identity/profiles/enhanced-validation?dxVersion=3
        // remaining= pdx:https://heimstubs-api-ite.clouddqt.capitalone.com/heimstubs-web/identity/profiles/enhanced-validation

        // Set property value
        devExchangeComponent.setPartnerAccessTokenUri("partnerAccessTokenUri");

        // All the parameters passed to the endpoint
        Map<String, Object> parameters = new HashMap<>();

        DevExchangeEndpoint endpoint = (DevExchangeEndpoint) devExchangeComponent.createEndpoint(
                "dx://pdx:https://www.google.com",
                "pdx:https://www.google.com",
                parameters);

        assertEquals("pdx", endpoint.getServiceName());
        assertEquals("https://www.google.com", endpoint.getTargetUrl());

    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    @NullSource
    public void testPdxEndpointWithInvalidPartnerAccessTokenUri(String partnerAccessTokenUri) throws Exception {
        // uri= dx://pdx:https://heimstubs-api-ite.clouddqt.capitalone.com/heimstubs-web/identity/profiles/enhanced-validation?dxVersion=3
        // remaining= pdx:https://heimstubs-api-ite.clouddqt.capitalone.com/heimstubs-web/identity/profiles/enhanced-validation

        // Set property value
        devExchangeComponent.setPartnerAccessTokenUri(partnerAccessTokenUri);

        // All the parameters passed to the endpoint
        Map<String, Object> parameters = new HashMap<>();

        assertThrows(ChassisSystemException.class, () ->
                devExchangeComponent.createEndpoint(
                        "dx://pdx:https://www.google.com?method=get",
                        "pdx:https://www.google.com",
                        parameters));
    }

    @Test
    @DirtiesContext
    public void testNoServiceNameOrScheme() {
        assertThrows(IllegalArgumentException.class, () ->
                devExchangeComponent.createEndpoint("dx:www.google.com?method=get",	"www.google.com", Collections.emptyMap()));
    }

    @Test
    @DirtiesContext
    public void testNoServiceName() {
        assertThrows(IllegalArgumentException.class, () ->
                devExchangeComponent.createEndpoint("dx://http://www.google.com", "http://www.google.com", Collections.emptyMap()));
    }

    @Test
    @DirtiesContext
    public void testNoSchemeName() {
        assertThrows(IllegalArgumentException.class, () ->
                devExchangeComponent.createEndpoint("dx://serviceName:www.google.com", "serviceName://www.google.com", Collections.emptyMap()));
    }

    @Test
    @DirtiesContext
    public void testInvalidSchemeName() {
        assertThrows(IllegalArgumentException.class, () ->
    	devExchangeComponent.
                createEndpoint("dx://serviceName:ftp://www.google.com", "serviceName:ftp://www.google.com", Collections.emptyMap()));
    }

    @Configuration
    public static class ContextConfigCamel {
        @Bean
        protected CamelContext createCamelContext() {
            CamelContext camelContext = new DefaultCamelContext();
            DevExchangeDAO devExchangeDAO = mock(DevExchangeDAO.class);

            DevExchangeResponse dxResponse = new DevExchangeResponse();
            dxResponse.setDxResponse(RESPONSE_BODY);
            dxResponse.setHttpStatus(HttpStatus.OK.value());
            lenient().when(devExchangeDAO.invokeDxAPI(any(DevExchangeRequest.class))).thenReturn(dxResponse);

            devExchangeComponent.setDevExchangeDAO(devExchangeDAO);
            camelContext.adapt(ModelCamelContext.class).addComponent("dx", devExchangeComponent);

            return camelContext;
        }

    }
}
