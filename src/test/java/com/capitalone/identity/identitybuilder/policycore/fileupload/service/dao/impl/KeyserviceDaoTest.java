package com.capitalone.identity.identitybuilder.policycore.fileupload.service.dao.impl;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.DevXPublicKeyServiceResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.util.FileuploadUtil;
import com.capitalone.identity.identitybuilder.policycore.service.util.JWTKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyserviceDaoTest {

	private static final String productId = "t2v-ivr-product";

	private static final String ANY_STRING_YOU_WANT = "any string you want";
	
	private String krpServiceUri = "https://api-it.cloud.capitalone.com";

	@Mock
	private OAuth2RestOperations krpRestTemplate;

	@Mock
	private OAuth2AccessToken oAuth2AccessToken;
	
	@Mock
	private RestClientException restException;

	@Mock
	private WebClientException webClientException;
	
	@Mock
	private ResponseEntity<DevXPublicKeyServiceResponse> responseEntity;
	
	@Mock
	private FileuploadUtil fileuploadUtil;

	@Mock
	private WebClient webClient;

	@Mock
	private WebClient.RequestBodySpec requestBodyMock;

	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriMock;

	@Mock
	private WebClient.ResponseSpec responseMock;
	
	private DevXPublicKeyServiceResponse devXPublicKeyServiceResponse;

	private List<JWTKey> keys = new ArrayList<JWTKey>();

	private JWTKey key;

	@BeforeEach
	public void setup() {
		createJWTObject();
		createDevXPublicKeyServiceResponse();
		lenient().when(krpRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
		lenient().when(oAuth2AccessToken.getValue()).thenReturn(ANY_STRING_YOU_WANT);
		lenient().when(webClient.get()).thenReturn(requestHeadersUriMock);
		lenient().when(requestHeadersUriMock.uri(anyString())).thenReturn(requestBodyMock);
		lenient().when(requestBodyMock.headers(any())).thenReturn(requestBodyMock);
		lenient().when(requestBodyMock.retrieve()).thenReturn(responseMock);
		lenient().when(krpRestTemplate.exchange(anyString(), eq(HttpMethod.GET), isA(HttpEntity.class),
				eq(DevXPublicKeyServiceResponse.class))).thenReturn(responseEntity);
		lenient().when(responseMock.toEntity(eq(DevXPublicKeyServiceResponse.class))).thenReturn(Mono.just(responseEntity));
		lenient().when(responseEntity.getBody()).thenReturn(devXPublicKeyServiceResponse);
	}

	private void createDevXPublicKeyServiceResponse() {
		devXPublicKeyServiceResponse = new DevXPublicKeyServiceResponse();
		devXPublicKeyServiceResponse.setKeys(keys);
	}

	private void createJWTObject() {
		key = new JWTKey();
		key.setX("ARVFtBDLtPaMj8_EYqeVQcpjX4LA0I3dVX_vxuy51k9835B8zJKt-34akXvvGdiMVcFmY2b7n1W0LGLrUWkQwwyo");
		key.setY("AXp5vQgFbFn8_AMpBoKK7o1AMZXAExh0hPdARdTBrOSrZ941Fdb4IKlgOqnMWsLoX_3wiFVjke1YRX6plijB8oI7");
		key.setAlg("ECDH-ES");
		key.setProductId(productId);
		key.setExp("16748714756");
		key.setKid("a597297b-6149-4132-8092-e30173d29910");
		keys.add(key);
	}

	private KeyserviceDao createKeyServiceDAO(boolean webclientEnabled){
		return new KeyserviceDao(webClient, krpRestTemplate, fileuploadUtil, webclientEnabled, webclientEnabled);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void getDevxPublicKeyTest(boolean webclientEnabled) {
		KeyserviceDao keyserviceDao = createKeyServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(keyserviceDao, "krpServiceUri", krpServiceUri);

		assertNotNull(keyserviceDao.getDevxPublicKey(productId));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void getDevxPublicKey_withNullResponseEntityTest(boolean webclientEnabled) {
		KeyserviceDao keyserviceDao = createKeyServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(keyserviceDao, "krpServiceUri", krpServiceUri);
		when(responseEntity.getBody()).thenReturn(null);

		assertThrows(ChassisSystemException.class, () -> keyserviceDao.getDevxPublicKey(productId));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void getDevxPublicKey_withNullKeysResponseTest(boolean webclientEnabled) {
		KeyserviceDao keyserviceDao = createKeyServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(keyserviceDao, "krpServiceUri", krpServiceUri);
		devXPublicKeyServiceResponse.setKeys(null);

		assertThrows(ChassisSystemException.class, () -> keyserviceDao.getDevxPublicKey(productId));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void getDevxPublicKey_withEmptyResponseTest(boolean webclientEnabled) {
		KeyserviceDao keyserviceDao = createKeyServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(keyserviceDao, "krpServiceUri", krpServiceUri);
		devXPublicKeyServiceResponse.setKeys(new ArrayList<>());

		assertThrows(ChassisSystemException.class, () -> keyserviceDao.getDevxPublicKey(productId));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testDevxPublicKeyThrows_restClientErrorException(boolean webclientEnabled) {
		KeyserviceDao keyserviceDao = createKeyServiceDAO(webclientEnabled);
		ReflectionTestUtils.setField(keyserviceDao, "krpServiceUri", krpServiceUri);
		lenient().when(krpRestTemplate.exchange(anyString(), eq(HttpMethod.GET), isA(HttpEntity.class),
				eq(DevXPublicKeyServiceResponse.class))).thenThrow(restException);
		lenient().when(responseMock.toEntity(eq(DevXPublicKeyServiceResponse.class))).thenThrow(webClientException);

		assertThrows(ChassisSystemException.class, () -> keyserviceDao.getDevxPublicKey(productId));
	}
}
