package com.capitalone.identity.identitybuilder.policycore.fileupload.rest.config;

import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;

import com.capitalone.identity.identitybuilder.policycore.fileupload.model.DevXPublicKeyServiceResponse;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class FileUploadSpringConfig {

	@Value("${security.oauth2.client.clientId}")
	private String clientId;

	@Value("${security.oauth2.client.clientSecret}")
	private String clientSecret;

	@Value("${security.oauth2.client.accessTokenUri}")
	private String accessTokenUri;

	@Value("${security.oauth2.client.grant-type}")
	private String grantType;

	@Value("${identitybuilder.policycore.fileupload.publickey.ttl.seconds:900}")
	private Long publicKeyMapTtl;
	
	
	@SuppressWarnings("deprecation")
	@Bean(name = "oAuthRestTemplate")
	public OAuth2RestOperations oAuthRestTemplate() {
		AccessTokenRequest atr = new DefaultAccessTokenRequest();
		return new OAuth2RestTemplate(resource(), new DefaultOAuth2ClientContext(atr));
	}

	@SuppressWarnings("deprecation")
	@Bean
	protected OAuth2ProtectedResourceDetails resource() {
		ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
		resource.setAccessTokenUri(accessTokenUri);
		resource.setClientId(clientId);
		resource.setClientSecret(clientSecret);
		resource.setGrantType(grantType);
		resource.setAuthenticationScheme(AuthenticationScheme.header);
		return resource;
	}

	@Bean(name = "retryVirusScanTemplate")
	public RetryTemplate retryVirusScanTemplate() {
		RetryTemplate retryTemplate = new RetryTemplate();
		FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
		fixedBackOffPolicy.setBackOffPeriod(15);
		retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3);
		retryTemplate.setRetryPolicy(retryPolicy);
		return retryTemplate;
	}

	@Bean(name = "multipartPublicKeysMap")
	  public PassiveExpiringMap<String, DevXPublicKeyServiceResponse> publicKeysMap() {
	    return new PassiveExpiringMap(publicKeyMapTtl, TimeUnit.SECONDS);
	  }

}
