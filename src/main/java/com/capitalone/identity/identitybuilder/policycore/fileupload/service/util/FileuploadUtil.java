package com.capitalone.identity.identitybuilder.policycore.fileupload.service.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;

@Component
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class FileuploadUtil {
	
	private static final String INTERNAL_SERVER_ERROR = "200003";
	
	
	public ChassisErrorCode getChassisSystemError(String text) {
		ChassisErrorCode apiErrorCode = new ChassisErrorCode();
		apiErrorCode.setId(INTERNAL_SERVER_ERROR);
		apiErrorCode.setDeveloperText(text);
		return apiErrorCode;
	}

}
