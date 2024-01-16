
package com.capitalone.identity.identitybuilder.policycore.rest.config;

import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

import javax.xml.bind.JAXBContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SpringConfigTest {

	@InjectMocks
	private SpringConfig springConfig;

	@Mock
	private SchemaManager schemaManager;

	@Mock
	private JAXBContext jaxbContext;

	private static final String ERROR_MESSAGES_BASENAME = "basename";
	private static final Integer TIMEOUT = 7200;

	@BeforeEach
	void setUp() {
		System.setProperty("spring.profiles.active", "devint-test");
	}

	@AfterEach
	void tearDown() {
		System.clearProperty("spring.profiles.active");
	}

	@Test
	public void testCamelContextConfiguration() {
		CamelContext camelContext = new DefaultCamelContext();
		CamelContextConfiguration config = spy(springConfig.camelContextConfiguration());
		assertNotNull(config);
		config.beforeApplicationStart(camelContext);
		config.afterApplicationStart(camelContext);
		verify(config).beforeApplicationStart(camelContext);
		verify(config).afterApplicationStart(camelContext);
	}

	@Test
	public void testErrorMessageSource() {
		MessageSource actual = springConfig.messageSource(TIMEOUT, ERROR_MESSAGES_BASENAME);
		assertTrue(ReloadableResourceBundleMessageSource.class.isInstance(actual));
	}

	@Test
	public void testValidationMessages() {
		MessageSource actual = springConfig.validationMessages(TIMEOUT, ERROR_MESSAGES_BASENAME);
		assertTrue(ReloadableResourceBundleMessageSource.class.isInstance(actual));
	}
	
	@Test
	public void testJacksonDataFormat() {
		assertNotNull(springConfig.jacksonDataFormat(null));
	}

	@Test
	public void testWebClientCreation(){
		SpringConfig springConfig = new SpringConfig(schemaManager);
		WebClient webClient = springConfig.oauth2Client(
				new InMemoryReactiveClientRegistrationRepository(ClientRegistration
						.withRegistrationId("masterbuilder-client")
						.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
						.clientId("masterbuilder")
						.tokenUri("tokenUri")
						.build()), "gatewayURL", "masterbuilder-2");
		assertNotNull(webClient);
	}

	@Test
	public void reactiveClientRegistrationRepository(){
		SpringConfig springConfig = new SpringConfig(schemaManager);

		OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
		registration.setClientId("masterbuilder");
		registration.setClientName("masterbuilder-2");
		registration.setClientSecret("secret");
		registration.setProvider("devex");
		registration.setAuthorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());

		OAuth2ClientProperties.Provider provider = new OAuth2ClientProperties.Provider();
		provider.setAuthorizationUri("https://www.example.com/auth");
		provider.setTokenUri("https://www.example.com/oauth/token");

		OAuth2ClientProperties oAuth2ClientProperties = new OAuth2ClientProperties();
		oAuth2ClientProperties.getRegistration().put("masterbuilder-2", registration);
		oAuth2ClientProperties.getProvider().put("masterbuilder-2", provider);
		ReactiveClientRegistrationRepository reactiveClientRegistrationRepository = springConfig.reactiveClientRegistrationRepository(oAuth2ClientProperties);
		assertNotNull(reactiveClientRegistrationRepository);
	}
}

/*
 * Copyright 2020 Capital One Financial Corporation All Rights Reserved.
 * 
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */
