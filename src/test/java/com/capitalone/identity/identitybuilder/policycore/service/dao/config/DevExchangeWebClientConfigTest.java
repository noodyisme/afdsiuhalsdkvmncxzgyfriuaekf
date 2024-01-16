package com.capitalone.identity.identitybuilder.policycore.service.dao.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class DevExchangeWebClientConfigTest {
    @InjectMocks
    DevExchangeWebClientConfig devExchangeWebClientConfig;

    @Test
    public void testDXWebClientCreation(){
        WebClient webClient = devExchangeWebClientConfig.devexWebClient(
                new InMemoryReactiveClientRegistrationRepository(ClientRegistration
                        .withRegistrationId("masterbuilder-client")
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .clientId("masterbuilder")
                        .tokenUri("tokenUri")
                        .build()), "masterbuilder-2");
        assertNotNull(webClient);
    }

    @Test
    public void testPDXWebClientCreation(){
        WebClient webClient = devExchangeWebClientConfig.pdxWebClient(
                new InMemoryReactiveClientRegistrationRepository(ClientRegistration
                        .withRegistrationId("masterbuilder-client")
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .clientId("masterbuilder")
                        .tokenUri("tokenUri")
                        .build()), "masterbuilder-2");
        assertNotNull(webClient);
    }

    @Test
    public void reactiveClientRegistrationRepository(){

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

        ReflectionTestUtils.setField(devExchangeWebClientConfig, "partnerAccessTokenUri", "https://www.partner-example.com/oauth/token");

        ReactiveClientRegistrationRepository reactiveClientRegistrationRepository = devExchangeWebClientConfig.reactiveClientRegistrationRepository(oAuth2ClientProperties);
        assertNotNull(reactiveClientRegistrationRepository);
    }

    @Test
    public void testInvalidPartnerURI() {
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

        ReflectionTestUtils.setField(devExchangeWebClientConfig, "partnerAccessTokenUri", "invalidURIScheme");
        assertThrows(IllegalArgumentException.class, () -> devExchangeWebClientConfig.reactiveClientRegistrationRepository(oAuth2ClientProperties));
    }

}
