package com.capitalone.identity.identitybuilder.policycore.fileupload.service.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileuploadUtilTest {
	
	@InjectMocks
	private FileuploadUtil fileuploadUtil;
	
	

	@Test
	void testChassisErrorCode() {
		assertNotNull(fileuploadUtil.getChassisSystemError("ANY_ERROR_TEXT"));

	}

}
