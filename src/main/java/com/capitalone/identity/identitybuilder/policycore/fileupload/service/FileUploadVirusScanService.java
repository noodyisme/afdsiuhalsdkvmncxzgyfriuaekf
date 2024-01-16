package com.capitalone.identity.identitybuilder.policycore.fileupload.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import com.capitalone.identity.identitybuilder.policycore.fileupload.service.dao.impl.VirusScanServiceDao;

@Component
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class FileUploadVirusScanService {

	@Autowired
	private VirusScanServiceDao scanDao;

	/**
	 * This method is responsible for scaning the document for virus
	 * 
	 * @param FileSystemResource
	 *            contains file information
	 * 
	 * @return boolean value
	 */
	public boolean isVirusAffected(FileSystemResource fs) {
		return scanDao.isAffected(fs);
	}

}
