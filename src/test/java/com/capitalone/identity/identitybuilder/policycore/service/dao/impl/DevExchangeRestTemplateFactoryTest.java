package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.chassis.engine.consumerservices.common.servicelocator.EndpointLocator;
import com.capitalone.chassis.engine.consumerservices.common.servicelocator.model.EndpointProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
public class DevExchangeRestTemplateFactoryTest {
	static private DevExchangeRestTemplateFactory devExchangeRestTemplateFactory;

	static OAuth2ClientContext oAuth2ClientContext;

	static EndpointLocator endpointLocator;

	@BeforeEach
	public void setUp() {
		oAuth2ClientContext = Mockito.mock(OAuth2ClientContext.class);
		endpointLocator = Mockito.mock(EndpointLocator.class);

		devExchangeRestTemplateFactory = new DevExchangeRestTemplateFactory(oAuth2ClientContext, endpointLocator);
		devExchangeRestTemplateFactory.init();
	}

	@Test
	public void testCreate() {assertNotNull(devExchangeRestTemplateFactory);}

	@Test
	public void reuseRestTemplate() {
		RestTemplate restTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("govid-sendalert", "dx");
		RestTemplate secondRestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("govid-sendalert", "dx");

		assertEquals(restTemplate.hashCode(), secondRestTemplate.hashCode());
	}

	@Test
	public void reuseRestTemplateForPartnerGateway() {
		RestTemplate restTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("prove-trustv2", "pdx");
		RestTemplate secondRestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("prove-trustv2", "pdx");

		assertEquals(restTemplate.hashCode(), secondRestTemplate.hashCode());
	}

	@Test
	public void servicesDoNotShareRestTemplate() {
		RestTemplate govIdRestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("govid-sendalert", "dx");
		RestTemplate randomRestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("random-other", "dx");

		assertNotEquals(govIdRestTemplate.hashCode(), randomRestTemplate.hashCode());
	}

	@Test
	public void servicesDoNotShareRestTemplateforPartnerGateway() {
		RestTemplate govIdRestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("prove-trustv2", "pdx");
		RestTemplate randomRestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("govid-sendalert", "pdx");

		assertNotEquals(govIdRestTemplate.hashCode(), randomRestTemplate.hashCode());
	}

	@Test
	public void servicesDoNotShareRestTemplateforPartnerGatewayAndInternalGateway() {
		RestTemplate govIdRestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("govid-sendalert", "dx");
		RestTemplate randomRestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate("govid-sendalert", "pdx");

		assertNotEquals(govIdRestTemplate.hashCode(), randomRestTemplate.hashCode());
	}

	@Test
	public void endpointTimeoutsAreRetrievedWhenConfigured() {
		String configuredServiceName = "govid";
		EndpointProperties mockedEndpointProperties = Mockito.mock(EndpointProperties.class);
		lenient().when(endpointLocator.getEndpointProperties(eq(configuredServiceName))).thenAnswer((InvocationOnMock invocation) -> Optional.of(mockedEndpointProperties));

		devExchangeRestTemplateFactory.getDevExchangeRestTemplate("not-configured", "dx");
		verify(mockedEndpointProperties, never()).getConnectTimeout();
		verify(mockedEndpointProperties, never()).getRequestTimeout();

		devExchangeRestTemplateFactory.getDevExchangeRestTemplate("govid", "dx");
		verify(mockedEndpointProperties, atLeastOnce()).getConnectTimeout();
		verify(mockedEndpointProperties, atLeastOnce()).getRequestTimeout();
	}
}
