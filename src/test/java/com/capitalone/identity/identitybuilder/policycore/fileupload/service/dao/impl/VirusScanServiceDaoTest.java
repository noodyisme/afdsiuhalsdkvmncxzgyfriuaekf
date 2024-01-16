package com.capitalone.identity.identitybuilder.policycore.fileupload.service.dao.impl;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.VirusScanResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.util.FileuploadUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VirusScanServiceDaoTest {

	private static final String ANY_STRING_YOU_WANT = "any string you want";

	private String virusScanURL = "https://api-it.cloud.capitalone.com";
	
	private boolean virusScanEnabled = true;

	@Mock
	private OAuth2RestOperations scanRestTemplate;

	@Mock
	private RetryTemplate retryVirusScanTemplate;

	@Mock
	private OAuth2AccessToken oAuth2AccessToken;

	@Mock
	private RestClientException restException;

	@Mock
	private WebClientException webClientException;

	@Mock
	private ResponseEntity<VirusScanResponse> responseEntity;
	
	@Mock
	private FileuploadUtil fileuploadUtil;

	@Mock
	private WebClient webClient;
	@Mock
	private WebClient.RequestBodyUriSpec requestBodyUriMock;
	@Mock
	private WebClient.RequestBodySpec requestBodyMock;
	@Mock
	private WebClient.RequestHeadersSpec requestHeadersMock;
	@Mock
	private WebClient.ResponseSpec responseMock;

	private VirusScanResponse virusScanResponse;

	@Mock
	private FileSystemResource fs;
	
	
	@BeforeEach
	public void setup() {
		virusScanResponse = setResponse();
		lenient().when(scanRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
		lenient().when(oAuth2AccessToken.getValue()).thenReturn(ANY_STRING_YOU_WANT);
		lenient().when(responseEntity.getBody()).thenReturn(virusScanResponse);
		lenient().when(webClient.post()).thenReturn(requestBodyUriMock);
		lenient().when(requestBodyUriMock.uri(anyString())).thenReturn(requestBodyMock);
		lenient().when(requestBodyMock.headers(any())).thenReturn(requestBodyMock);
		lenient().when(requestBodyMock.bodyValue(any())).thenReturn(requestHeadersMock);
		lenient().when(requestHeadersMock.header(any(),any())).thenReturn(requestHeadersMock);
		lenient().when(requestHeadersMock.retrieve()).thenReturn(responseMock);
		lenient().when(scanRestTemplate.exchange(anyString(), eq(HttpMethod.POST), isA(HttpEntity.class),
				eq(VirusScanResponse.class))).thenReturn(responseEntity);
		lenient().when(responseMock.toEntity(eq(VirusScanResponse.class))).thenReturn(Mono.just(responseEntity));
		lenient().when(responseEntity.getBody()).thenReturn(virusScanResponse);
		
	}

	private VirusScanServiceDao createVirusScanServiceDAO(boolean webclientEnabled){
		return new VirusScanServiceDao(webClient, scanRestTemplate, fileuploadUtil, retryVirusScanTemplate, webclientEnabled, webclientEnabled);
	}
	
	@Test
	void testIsAffected_notenabled() {
		VirusScanServiceDao virusScanServiceDao = createVirusScanServiceDAO(false);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanEnabled",false);
		assertFalse(virusScanServiceDao.isAffected(fs));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testIsAffected_false(boolean webclientEnabled) {
		VirusScanServiceDao virusScanServiceDao = createVirusScanServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanURL", virusScanURL);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanEnabled",virusScanEnabled);

		assertFalse(virusScanServiceDao.isAffected(fs));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testIsAffected_true(boolean webclientEnabled) {
		VirusScanServiceDao virusScanServiceDao = createVirusScanServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanURL", virusScanURL);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanEnabled",virusScanEnabled);
		virusScanResponse.setMalwareScanStatus("INFECTED");

		assertTrue(virusScanServiceDao.isAffected(fs));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testIsAffected_unknown(boolean webclientEnabled) {
		VirusScanServiceDao virusScanServiceDao = createVirusScanServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanURL", virusScanURL);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanEnabled",virusScanEnabled);
		virusScanResponse.setMalwareScanStatus("INFECTED");
		lenient().when(responseEntity.getBody()).thenReturn(null);

		assertFalse(virusScanServiceDao.isAffected(fs));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testIsAffectedThrowsException(boolean webclientEnabled) {
		VirusScanServiceDao virusScanServiceDao = createVirusScanServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanURL", virusScanURL);
		ReflectionTestUtils.setField(virusScanServiceDao, "virusScanEnabled",virusScanEnabled);
		lenient().when(scanRestTemplate.exchange(anyString(), eq(HttpMethod.POST), isA(HttpEntity.class),
				eq(VirusScanResponse.class))).thenThrow(restException);
		lenient().when(responseMock.toEntity(eq(VirusScanResponse.class))).thenThrow(webClientException);


		assertThrows(ChassisSystemException.class, () -> virusScanServiceDao.isAffected(fs));
	}

	private VirusScanResponse setResponse() {
		virusScanResponse = new VirusScanResponse();
		virusScanResponse.setDefinitionDate("definitionDate");
		virusScanResponse.setFileName("fileName");
		virusScanResponse.setMalwareScanStatus("CLEAN");
		virusScanResponse.setStatus("status");
		virusScanResponse.setTotalBytesRead(1234);
		return virusScanResponse;
	}

}
