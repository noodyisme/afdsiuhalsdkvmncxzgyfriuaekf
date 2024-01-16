package com.capitalone.identity.identitybuilder.policycore.fileupload.service.dao.impl;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.annotations.logging.Profile;
import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.DevXPublicKeyServiceResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.util.FileuploadUtil;
import com.capitalone.identity.identitybuilder.policycore.service.util.JWTKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
@Log
@Profile
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class KeyserviceDao {

	@Value("${identitybuilder.policycore.fileupload.krpService.url}")
	private String krpServiceUri;

	private final Boolean webclientEnabled;

	private final FileuploadUtil fileuploadUtil;

	private final WebClient webClient;
	private final OAuth2RestOperations krpRestTemplate;

	private static final String AUTHORIZATION = "Authorization";

	private final Logger logger = LogManager.getLogger(getClass()); // sonar fix

	private static final String PRODUCTID = "productId";

	public KeyserviceDao(WebClient webClient, @Qualifier("oAuthRestTemplate") OAuth2RestOperations krpRestTemplate, FileuploadUtil fileuploadUtil,
						 @Value("${identity.identitybuilder.policycore.service.webclient.enabled:false}") Boolean webclientEnabled,
						 @Value("${identitybuilder.policycore.feature.prerelease.enabled:false}") Boolean prereleaseEnabled) {

		this.webClient = webClient;
		this.krpRestTemplate = krpRestTemplate;
		this.fileuploadUtil = fileuploadUtil;
		this.webclientEnabled = webclientEnabled && prereleaseEnabled;
	}

	/**
	 * This method is responsible for fetching the publicKey from Keyservice api
	 * 
	 * @param productId
	 * @return DevXPublicKeyServiceResponse contains complete JWT object
	 */
	public DevXPublicKeyServiceResponse getDevxPublicKey(String productId) {

		DevXPublicKeyServiceResponse kprResponse = null;

		try {
			HttpHeaders headers = buildHttpHeaders();
			HttpEntity entity = new HttpEntity(headers);
			UriComponentsBuilder publicKeyURL = getPublicKeyServiceUrl(productId);

			ResponseEntity<DevXPublicKeyServiceResponse> responseEntity;

			if(Boolean.TRUE.equals(webclientEnabled)) {
				responseEntity = webClient
					.get()
					.uri(publicKeyURL.toUriString())
					.headers(httpHeaders -> httpHeaders.addAll(headers))
					.retrieve()
					.toEntity(DevXPublicKeyServiceResponse.class)
					.block();
			}
			else
			{
				responseEntity = krpRestTemplate
						.exchange(publicKeyURL.toUriString(), HttpMethod.GET, entity, DevXPublicKeyServiceResponse.class);
			}

			final DevXPublicKeyServiceResponse entityBody = responseEntity != null ? responseEntity.getBody() : null;
			final List<JWTKey> keys = entityBody != null ? entityBody.getKeys() : null;

			if (entityBody != null && keys != null && !keys.isEmpty()) {
				logger.info("Response from KRP API: {} ", entityBody);
				kprResponse = entityBody;

			} else {
				logger.error("Empty Response from KRP API: {} ", entityBody);
				ChassisErrorCode rootErrorCode = fileuploadUtil.getChassisSystemError("Response from KRP API was empty. ProductId may be invalid or KRP API unavailable.");
				throw new ChassisSystemException(rootErrorCode, HttpStatus.BAD_REQUEST.value());
			}
		} catch (RestClientException | WebClientException ex) {
			ChassisErrorCode rootErrorCode = fileuploadUtil.getChassisSystemError("Exception occured at KPR API");
			logger.error(String.format("Exception occurred while attempting to retrieve Public Key from KRP API: %s", ex.getMessage()), ex);
			throw new ChassisSystemException(rootErrorCode, ex, HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return kprResponse;
	}

	private UriComponentsBuilder getPublicKeyServiceUrl(String productId) {
		return getKeyServiceUrl(productId);
	}

	private UriComponentsBuilder getKeyServiceUrl(String productId) {
		return UriComponentsBuilder.fromHttpUrl(krpServiceUri).queryParam(PRODUCTID, productId);
	}

	private HttpHeaders buildHttpHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		headers.add("Accept", "application/json;v=1");
		if(Boolean.TRUE.equals(webclientEnabled)) {
			return headers;
		}
		headers.set(AUTHORIZATION, "Bearer " + krpRestTemplate.getAccessToken().getValue());
		return headers;

	}

}
