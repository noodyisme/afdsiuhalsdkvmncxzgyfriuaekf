package com.capitalone.identity.identitybuilder.policycore.service.dao.config;

import com.capitalone.identity.identitybuilder.policycore.service.dao.impl.DevExchangeDAOWebImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(value = DevExchangeDAOWebImpl.WEB_CLIENT_ENABLED_FLAG)
@ComponentScan(basePackages = "com.capitalone.identity.identitybuilder")
public class DevExchangeWebClientConfig {
    @Value("${security.oauth2.client.partnerAccessTokenUri:}")
    private String partnerAccessTokenUri;
    @Bean(name="devexchange")
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository(OAuth2ClientProperties oAuth2ClientProperties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        oAuth2ClientProperties.getRegistration().forEach((registrationId, registration) -> {
            ClientRegistration aRegistration = ClientRegistration
                    .withRegistrationId(registrationId)
                    .tokenUri(oAuth2ClientProperties.getProvider().get(registrationId).getTokenUri())
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientId(registration.getClientId())
                    .clientSecret(registration.getClientSecret())
                    .build();
            registrations.add(aRegistration);

            if(StringUtils.isNotEmpty(partnerAccessTokenUri)){
                if(!new UrlValidator().isValid(partnerAccessTokenUri)) {
                    throw new IllegalArgumentException("Invalid partnerAccessTokenUri provided: " + partnerAccessTokenUri);
                }
                String pdxId = "pdx-"+registrationId;
                ClientRegistration partnerRegistration = ClientRegistration
                        .withRegistrationId(pdxId)
                        .tokenUri(partnerAccessTokenUri)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .clientId(registration.getClientId())
                        .clientSecret(registration.getClientSecret())
                        .build();
                registrations.add(partnerRegistration);
            }
        });
        return new InMemoryReactiveClientRegistrationRepository(registrations);
    }

    @Bean("devexWebClient")
    public WebClient devexWebClient(@Qualifier("devexchange") ReactiveClientRegistrationRepository clientRegistrationRepository, @Value("${oauth.appname}") String clientRegistrationId) {
        ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService = new InMemoryReactiveOAuth2AuthorizedClientService(
                clientRegistrationRepository);
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager oauthManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, reactiveOAuth2AuthorizedClientService);
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(oauthManager);
        oauth2.setDefaultClientRegistrationId(clientRegistrationId);

        return WebClient
                .builder()
                .filter(oauth2)
                .build();
    }

    @Bean("pdxWebClient")
    public WebClient pdxWebClient(@Qualifier("devexchange") ReactiveClientRegistrationRepository clientRegistrationRepository, @Value("${oauth.appname}") String clientRegistrationId) {
        ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService = new InMemoryReactiveOAuth2AuthorizedClientService(
                clientRegistrationRepository);
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager oauthManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, reactiveOAuth2AuthorizedClientService);
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(oauthManager);
        oauth2.setDefaultClientRegistrationId("pdx-" + clientRegistrationId);

        return WebClient
                .builder()
                .filter(oauth2)
                .build();
    }
}

