package com.capitalone.identity.identitybuilder.policycore.fileupload.service;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.fileupload.model.DevXPublicKeyServiceResponse;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.dao.impl.KeyserviceDao;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.util.FileuploadUtil;
import com.capitalone.identity.identitybuilder.policycore.service.util.CryptoUtil;
import com.capitalone.identity.identitybuilder.policycore.service.util.JWTKey;
import com.nimbusds.jose.JOSEException;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileUploadEncryptionKeyServiceTest {

	private static final String ENCRYPTED_STRING = "EncryptedString";

	private static final String T2V_IVR_PRODUCT = "t2v-ivr-product";

	private static final String ANY_STRING_YOU_WANT = "any string you want";

	private static final String T2V_IVR_PRODUCT_INVALID = "invalid";

	private FileUploadEncryptionKeyService fileUploadEncryptionKeyService;

	private Map<String, DevXPublicKeyServiceResponse> synchronizedKeysMap;

	private PassiveExpiringMap<String, DevXPublicKeyServiceResponse> publicKeysMap;

	private DevXPublicKeyServiceResponse devXPublicKeyServiceResponse;

	private List<JWTKey> keys = new ArrayList<JWTKey>();

	private JWTKey key;

	private KeyserviceDao keyserviceDao;

	@Spy
	private FileuploadUtil fileuploadUtil;

	@Spy
	private OAuth2RestOperations krpRestTemplate;

	@Spy
	private OAuth2AccessToken oAuth2AccessToken;

	@Spy
	private ResponseEntity<DevXPublicKeyServiceResponse> responseEntity = new ResponseEntity<>(HttpStatus.OK);

	@Mock
	WebClient webClient;

	@Mock
	private WebClient.RequestBodySpec requestBodyMock;

	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriMock;

	@Mock
	private WebClient.ResponseSpec responseMock;

	private String krpServiceUri = "https://api-it.cloud.capitalone.com";

	@BeforeEach
	public void setup() {
		createJWTObject();
		createDevXPublicKeyServiceResponse();
		publicKeysMap = new PassiveExpiringMap<String, DevXPublicKeyServiceResponse>();
		publicKeysMap.put(T2V_IVR_PRODUCT, devXPublicKeyServiceResponse);
		synchronizedKeysMap = Collections.synchronizedMap(publicKeysMap);
		fileUploadEncryptionKeyService = new FileUploadEncryptionKeyService(publicKeysMap);
		lenient().when(krpRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
		lenient().when(oAuth2AccessToken.getValue()).thenReturn(ANY_STRING_YOU_WANT);
		lenient().when(webClient.get()).thenReturn(requestHeadersUriMock);
		lenient().when(requestHeadersUriMock.uri(anyString())).thenReturn(requestBodyMock);
		lenient().when(requestBodyMock.headers(any())).thenReturn(requestBodyMock);
		lenient().when(requestBodyMock.retrieve()).thenReturn(responseMock);
		lenient().when(krpRestTemplate.exchange(anyString(), eq(HttpMethod.GET), isA(HttpEntity.class),
				eq(DevXPublicKeyServiceResponse.class))).thenReturn(responseEntity);
		lenient().when(responseMock.toEntity(eq(DevXPublicKeyServiceResponse.class))).thenReturn(Mono.just(responseEntity));
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
		key.setProductId(T2V_IVR_PRODUCT);
		key.setExp("16748714756");
		key.setKid("a597297b-6149-4132-8092-e30173d29910");
		keys.add(key);
	}

	@Test
	public void testEncryptDocument() throws IllegalArgumentException, JOSEException {
		byte[] data = ANY_STRING_YOU_WANT.getBytes();
		assertNotNull(fileUploadEncryptionKeyService.encryptDocument(T2V_IVR_PRODUCT, data));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testEncryptDocument_withInvalidProductId(boolean webclientEnabled) throws IllegalArgumentException, JOSEException {
		keyserviceDao = new KeyserviceDao(webClient, krpRestTemplate, fileuploadUtil, false, false);
		KeyserviceDao keyserviceDaoSpy = Mockito.spy(keyserviceDao);
		setTestFields(keyserviceDaoSpy, webclientEnabled);
		byte[] data = ANY_STRING_YOU_WANT.getBytes();
		when(responseEntity.getBody()).thenReturn(null);

		assertThrows(ChassisSystemException.class,
				() -> fileUploadEncryptionKeyService.encryptDocument(T2V_IVR_PRODUCT_INVALID, data));
	}

	@SuppressWarnings("deprecation")
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testEncryptDocument_withNullPublicKey(boolean webclientEnabled) throws IllegalArgumentException, JOSEException {
		devXPublicKeyServiceResponse.setKeys(null);
		keyserviceDao = new KeyserviceDao(webClient, krpRestTemplate, fileuploadUtil, webclientEnabled, webclientEnabled);
		KeyserviceDao keyserviceDaoSpy = Mockito.spy(keyserviceDao);
		setTestFields(keyserviceDaoSpy, webclientEnabled);
		byte[] data = ANY_STRING_YOU_WANT.getBytes();
		when(responseEntity.getBody()).thenReturn(devXPublicKeyServiceResponse);

		assertThrows(ChassisSystemException.class,
				() -> fileUploadEncryptionKeyService.encryptDocument(T2V_IVR_PRODUCT, data));
	}

	@SuppressWarnings("deprecation")
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testEncryptDocument_withEmptyPublicKey(boolean webclientEnabled) throws IllegalArgumentException, JOSEException {
		devXPublicKeyServiceResponse.setKeys(new ArrayList<>());
		keyserviceDao = new KeyserviceDao(webClient, krpRestTemplate, fileuploadUtil, webclientEnabled, webclientEnabled);
		KeyserviceDao keyserviceDaoSpy = Mockito.spy(keyserviceDao);
		setTestFields(keyserviceDaoSpy, webclientEnabled);
		byte[] data = ANY_STRING_YOU_WANT.getBytes();
		when(responseEntity.getBody()).thenReturn(devXPublicKeyServiceResponse);

		assertThrows(ChassisSystemException.class,
				() -> fileUploadEncryptionKeyService.encryptDocument(T2V_IVR_PRODUCT, data));
	}

	@SuppressWarnings("deprecation")
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testEncryptDocument_withEmptyPublicKeyExpiry(boolean webclientEnabled) throws IllegalArgumentException, JOSEException {
		key.setExp("1605569217");
		keyserviceDao = new KeyserviceDao(webClient, krpRestTemplate, fileuploadUtil, webclientEnabled, webclientEnabled);
		KeyserviceDao keyserviceDaoSpy = Mockito.spy(keyserviceDao);
		setTestFields(keyserviceDaoSpy, webclientEnabled);
		byte[] data = ANY_STRING_YOU_WANT.getBytes();
		when(responseEntity.getBody()).thenReturn(devXPublicKeyServiceResponse);

		assertNotNull(fileUploadEncryptionKeyService.encryptDocument(T2V_IVR_PRODUCT, data));
	}

	@SuppressWarnings("deprecation")
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testEncryptDocument_whenEncrptionUtilThrowsError(boolean webclientEnabled) throws IllegalArgumentException, JOSEException {
		keyserviceDao = new KeyserviceDao(webClient, krpRestTemplate, fileuploadUtil, webclientEnabled, webclientEnabled);
		KeyserviceDao keyserviceDaoSpy = Mockito.spy(keyserviceDao);
		setTestFields(keyserviceDaoSpy, webclientEnabled);
		byte[] data = ANY_STRING_YOU_WANT.getBytes();
		when(responseEntity.getBody()).thenReturn(devXPublicKeyServiceResponse);
		MockedStatic<CryptoUtil> mockStatic = Mockito.mockStatic(CryptoUtil.class);
		when(CryptoUtil.encryptEcdh(Mockito.any(), Mockito.any())).thenThrow(IllegalArgumentException.class);

		assertThrows(ChassisSystemException.class, () -> fileUploadEncryptionKeyService.encryptDocument(null, data));
		mockStatic.close();

	}

	private void setTestFields(KeyserviceDao keyserviceDao, boolean webclientEnabled){
		ReflectionTestUtils.setField(fileUploadEncryptionKeyService, "keyserviceDao", keyserviceDao);
		ReflectionTestUtils.setField(keyserviceDao, "krpRestTemplate", krpRestTemplate);
		ReflectionTestUtils.setField(keyserviceDao, "krpServiceUri", krpServiceUri);
		ReflectionTestUtils.setField(keyserviceDao, "fileuploadUtil", fileuploadUtil);
		ReflectionTestUtils.setField(fileUploadEncryptionKeyService, "fileuploadUtil", fileuploadUtil);
	}

}
