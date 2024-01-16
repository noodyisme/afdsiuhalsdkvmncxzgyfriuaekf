package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.chassis.engine.consumerservices.common.servicelocator.EndpointLocator;
import com.capitalone.chassis.engine.consumerservices.common.servicelocator.model.EndpointProperties;
import com.newrelic.api.agent.Trace;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @deprecated
 */
@Component
@Deprecated
public class DevExchangeRestTemplateFactory {

	@Value("${security.oauth2.client.clientId}")
	private String clientId;

	@Value("${security.oauth2.client.clientSecret}")
	private String clientSecret;

	@Value("${security.oauth2.client.accessTokenUri}")
	private String accessTokenUri;

	@Value("${security.oauth2.client.partnerAccessTokenUri:}")
	private String partnerAccessTokenUri;

	@Value("${chassis.servicelocator.default.connectTimeout}")
	private int connectionTimeout;

	@Value("${chassis.servicelocator.default.requestTimeout}")
	private int requestTimeout;

	@Value("${httpclient.maxconnection.perroute:500}")
	private int maxConnPerRoute;

	@Value("${httpclient.maxconnection.total:500}")
	private int maxConnTotal;

	private EndpointLocator chassisEndpointlocator;

	private final OAuth2ClientContext oAuth2ClientContext;
	private ClientCredentialsResourceDetails clientCredentialsResourceDetails;
	private ClientCredentialsResourceDetails partnerClientCredentialsResourceDetails;

	private Map<String, OAuth2RestTemplate> requestFactoryMap;
	private CloseableHttpClient closeableHttpClient;

	public DevExchangeRestTemplateFactory(OAuth2ClientContext oAuth2ClientContext, @Qualifier("chassisFileServiceLocator") EndpointLocator chassisEndpointlocator) {
		this.oAuth2ClientContext = oAuth2ClientContext;
		this.chassisEndpointlocator = chassisEndpointlocator;
	}

	@PostConstruct
	void init() {
		requestFactoryMap = new HashMap<>();
		closeableHttpClient = HttpClients.custom().setMaxConnPerRoute(maxConnPerRoute)
				.setMaxConnTotal(maxConnTotal)
				.build();

		this.clientCredentialsResourceDetails = new ClientCredentialsResourceDetails();
		this.clientCredentialsResourceDetails.setId("1");
		this.clientCredentialsResourceDetails.setClientId(clientId);
		this.clientCredentialsResourceDetails.setClientSecret(clientSecret);
		this.clientCredentialsResourceDetails.setAccessTokenUri(accessTokenUri);

		this.partnerClientCredentialsResourceDetails = new ClientCredentialsResourceDetails();
		this.partnerClientCredentialsResourceDetails.setId("2");
		this.partnerClientCredentialsResourceDetails.setClientId(clientId);
		this.partnerClientCredentialsResourceDetails.setClientSecret(clientSecret);
		this.partnerClientCredentialsResourceDetails.setAccessTokenUri(partnerAccessTokenUri);
	}


	public OAuth2RestTemplate getDevExchangeRestTemplate(String serviceName, String scheme) {
		String key = scheme+"-"+serviceName;
		OAuth2RestTemplate restTemplate = requestFactoryMap.get(key);
		if (restTemplate != null) {
			return restTemplate;
		}

		restTemplate = createRestTemplateForService(serviceName, scheme);
		requestFactoryMap.put(key, restTemplate);
		return restTemplate;
	}


	private OAuth2RestTemplate createRestTemplateForService(String serviceName, String scheme) {
		// TODO: ClientCredentialsResourceDetails is deprecated and instead we should move to WebClient approach once
		//  Chassis supports the use of WebClient https://github.cloud.capitalone.com/chassis-framework/chassis-spring-boot-starters/pull/444
		if("pdx".equalsIgnoreCase(scheme) && "".equals(this.partnerAccessTokenUri)){
			throw new IllegalArgumentException("Add the missing application property security.oauth2.client.partnerAccessTokenUri");
		}
		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate("dx".equalsIgnoreCase(scheme)?clientCredentialsResourceDetails:partnerClientCredentialsResourceDetails, oAuth2ClientContext);
		restTemplate.setRequestFactory(createRequestFactory(serviceName));
		return restTemplate;
	}

	@Trace
	private HttpComponentsClientHttpRequestFactory createRequestFactory(String serviceName) {
		Optional<EndpointProperties> epProperties = chassisEndpointlocator
				.getEndpointProperties(serviceName);
		if (epProperties.isPresent()) {
			connectionTimeout = epProperties.get().getConnectTimeout();
			requestTimeout = epProperties.get().getRequestTimeout();
		}

		// Setting the pool per service name allows the connection and request timeouts to be set at the service level as
		// our clients requested
		HttpComponentsClientHttpRequestFactory requestFactory =
				new HttpComponentsClientHttpRequestFactory(closeableHttpClient);
		requestFactory.setReadTimeout(requestTimeout);
		requestFactory.setConnectTimeout(connectionTimeout);
		return requestFactory;
	}
}

