package com.capitalone.identity.identitybuilder.policycore.fileupload.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;

import com.capitalone.identity.identitybuilder.policycore.fileupload.service.dao.impl.VirusScanServiceDao;

@ExtendWith(MockitoExtension.class)
public class FileUploadVirusScanServiceTest {
	
	@InjectMocks
	private FileUploadVirusScanService fileUploadVirusScanService;
	
	@Mock 
	private VirusScanServiceDao scanDao;
	
	@Mock
	private FileSystemResource fs;

	@Test
	public void isVirusAffected_test() {
		when(scanDao.isAffected(fs)).thenReturn(false);
		assertNotNull(fileUploadVirusScanService.isVirusAffected(fs));
	}

}
