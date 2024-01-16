package com.capitalone.identity.identitybuilder.policycore.fileupload.rest.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.capitalone.identity.identitybuilder.policycore.fileupload.rest.config.FileUploadSpringConfig;

@ExtendWith(MockitoExtension.class)
public class FileUploadSpringConfigTest {

	private String clientId = "1234";

	private final String clientSecret = "1234";

	private final String accessTokenUri = "https://sample.com";

	private final String grantType = "grant";

	private final long publicKeyMapTtl = 6000000L;

	@InjectMocks
	private FileUploadSpringConfig fileUploadSpringConfig;

	@BeforeEach
	public void setup() {
		ReflectionTestUtils.setField(fileUploadSpringConfig, "clientId", clientId);
		ReflectionTestUtils.setField(fileUploadSpringConfig, "clientSecret", clientSecret);
		ReflectionTestUtils.setField(fileUploadSpringConfig, "accessTokenUri", accessTokenUri);
		ReflectionTestUtils.setField(fileUploadSpringConfig, "grantType", grantType);
		ReflectionTestUtils.setField(fileUploadSpringConfig, "publicKeyMapTtl", publicKeyMapTtl);
	}

	@Test
	public void testOAuthRestTemplate() {
		assertNotNull(fileUploadSpringConfig.oAuthRestTemplate());
	}
	
	@Test
	public void testPublicKeysMap() {
		assertNotNull(fileUploadSpringConfig.publicKeysMap());
	}
	
	@Test
	public void testRetryVirusScanTemplate() {
		assertNotNull(fileUploadSpringConfig.retryVirusScanTemplate());
	}
	

}
