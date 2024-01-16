package com.capitalone.identity.identitybuilder.policycore.fileupload.service.dao.impl;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.annotations.logging.Profile;
import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.VirusScanResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.util.FileuploadUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Profile
@Log
@Component
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class VirusScanServiceDao {

	private static final String APPLICATION_JSON_V_1 = "application/json; v=1";

	private static final String CLEAN = "CLEAN";

	private final Logger logger = LogManager.getLogger(getClass());

	@Value("${identitybuilder.policycore.fileupload.virusscan.service.enabled:true}")
	private boolean virusScanEnabled;

	@Value("${identitybuilder.policycore.fileupload.virusscan.url}")
	private String virusScanURL;

	RetryTemplate retryVirusScanTemplate;

	private final OAuth2RestOperations restTemplate;

	private final FileuploadUtil fileuploadUtil;

	private final WebClient webClient;

	private final Boolean webclientEnabled;


	public VirusScanServiceDao(WebClient webClient, @Qualifier("oAuthRestTemplate") OAuth2RestOperations restTemplate, FileuploadUtil fileuploadUtil,
							   @Qualifier("retryVirusScanTemplate") RetryTemplate retryVirusScanTemplate,
							   @Value("${identity.identitybuilder.policycore.service.webclient.enabled:false}") Boolean webclientEnabled,
							   @Value("${identitybuilder.policycore.feature.prerelease.enabled:false}") Boolean prereleaseEnabled) {
		this.webClient = webClient;
		this.restTemplate = restTemplate;
		this.fileuploadUtil = fileuploadUtil;
		this.retryVirusScanTemplate = retryVirusScanTemplate;
		this.webclientEnabled = webclientEnabled && prereleaseEnabled;
	}

	public boolean isAffected(FileSystemResource fs) {
		boolean isAffected = false;
		isAffected = scanImageWithVirusScan(fs);
		logger.info("File-VirusScan: scanned Document File Result {}", isAffected);
		return isAffected;
	}

	private boolean scanImageWithVirusScan(FileSystemResource fs) {
		boolean isAffected = false;
		VirusScanResponse virusScanResponse = null;
		if (virusScanEnabled) {
			virusScanResponse = scanImage(fs);
			isAffected = validateVirusScanResult(virusScanResponse);
		} else {
			logger.info("File-VirusScan: Skipping Virus scan as the feature is not enabled {}", virusScanEnabled);
		}

		return isAffected;
	}

	private boolean validateVirusScanResult(VirusScanResponse virusScanResponse) {
		boolean isAffected = false;
		String scanStatus = null;
		if (null != virusScanResponse && null != virusScanResponse.getMalwareScanStatus()) {
			scanStatus = virusScanResponse.getMalwareScanStatus();
			logger.info("File-VirusScan: Verified with clean status : {} ", scanStatus);
			if (!CLEAN.equalsIgnoreCase(scanStatus)) {
				logger.error("File-VirusScan: Verified with infected status : {} ", scanStatus);
				isAffected = true;
			}
		} else {
			logger.error("File-VirusScan: Verified with unknown status. Virus Scan Response: {}", virusScanResponse);
		}
		return isAffected;
	}

	private VirusScanResponse scanImage(FileSystemResource fs) {

		MultiValueMap<String, Object> multipartFile = null;
		VirusScanResponse virusScanResponse = null;
		ResponseEntity<VirusScanResponse> responseEntity;

		try {
			multipartFile = new LinkedMultiValueMap<>();
			multipartFile.add("file", fs);
			HttpHeaders headers = getHttpHeaders();
			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartFile, headers);
			if(Boolean.TRUE.equals(webclientEnabled)){
				responseEntity = webClient
					.post()
					.uri(virusScanURL)
					.headers(httpHeaders -> httpHeaders.addAll(headers))
					.bodyValue(multipartFile)
					.retrieve()
					.toEntity(VirusScanResponse.class)
					.block();
			}
			else{
				responseEntity = restTemplate.exchange(virusScanURL, HttpMethod.POST,
						requestEntity, VirusScanResponse.class);
			}

			virusScanResponse = responseEntity != null ? responseEntity.getBody() : null;

		} catch (RestClientException | WebClientException ex) {
			ChassisErrorCode chassisError = fileuploadUtil.getChassisSystemError("Exception while scanning files for virus");
			logger.error(String.format("Exception while scanning file for virus: %s", ex.getMessage()), ex);
			throw new ChassisSystemException(chassisError, ex, HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return virusScanResponse;
	}

	private HttpHeaders getHttpHeaders() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set(HttpHeaders.ACCEPT, APPLICATION_JSON_V_1);
		requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
		if(Boolean.TRUE.equals(webclientEnabled)){
			return requestHeaders;
		}
		requestHeaders.set("Authorization", "Bearer " + restTemplate.getAccessToken().getValue());
		return requestHeaders;
	}

	

}
