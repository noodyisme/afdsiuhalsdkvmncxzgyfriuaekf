package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.chassis.engine.consumerservices.common.servicelocator.EndpointLocator;
import com.capitalone.chassis.engine.consumerservices.common.servicelocator.model.EndpointProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DevExchangeWebClientFactoryTest {
	static EndpointLocator endpointLocator;
	private DevExchangeWebClientFactory devExchangeWebClientFactory;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	WebClient dxClient;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	WebClient pdxClient;

	@BeforeEach
	public void setUp() {
		endpointLocator = Mockito.mock(EndpointLocator.class);
		this.devExchangeWebClientFactory = new DevExchangeWebClientFactory(dxClient, pdxClient, endpointLocator, 7000, 7000);
		devExchangeWebClientFactory.init();
	}

	@Test
	public void testCreate() {assertNotNull(devExchangeWebClientFactory);}

	@Test
	public void reuseWebClient() {
		WebClient webClient = devExchangeWebClientFactory.getDevExchangeWebClient("govid-sendalert", "dx");
		WebClient secondWebClient = devExchangeWebClientFactory.getDevExchangeWebClient("govid-sendalert", "dx");

		assertEquals(webClient.hashCode(), secondWebClient.hashCode());
	}

	@Test
	public void reuseWebClientForPartnerGateway() {
		WebClient webClient = devExchangeWebClientFactory.getDevExchangeWebClient("prove-trustv2", "pdx");
		WebClient secondWebClient = devExchangeWebClientFactory.getDevExchangeWebClient("prove-trustv2", "pdx");

		assertEquals(webClient.hashCode(), secondWebClient.hashCode());
	}

	@Test
	public void servicesDoNotShareWebClientforPartnerGatewayAndInternalGateway() {
		WebClient govIdWebClient = devExchangeWebClientFactory.getDevExchangeWebClient("govid-sendalert", "dx");
		WebClient randomWebClient = devExchangeWebClientFactory.getDevExchangeWebClient("govid-sendalert", "pdx");

		assertNotEquals(govIdWebClient.hashCode(), randomWebClient.hashCode());
	}

	@Test
	public void endpointTimeoutsAreRetrievedWhenConfigured() {
		String configuredServiceName = "govid";
		EndpointProperties mockedEndpointProperties = Mockito.mock(EndpointProperties.class);
		lenient().when(endpointLocator.getEndpointProperties(eq(configuredServiceName))).thenAnswer((InvocationOnMock invocation) -> Optional.of(mockedEndpointProperties));

		devExchangeWebClientFactory.getDevExchangeWebClient("not-configured", "dx");
		verify(mockedEndpointProperties, never()).getConnectTimeout();
		verify(mockedEndpointProperties, never()).getRequestTimeout();

		devExchangeWebClientFactory.getDevExchangeWebClient("govid", "dx");
		verify(mockedEndpointProperties, atLeastOnce()).getConnectTimeout();
		verify(mockedEndpointProperties, atLeastOnce()).getRequestTimeout();
	}
}
