package com.capitalone.identity.identitybuilder.policycore.fileupload.onestream;

import lombok.Data;

import java.io.Serializable;

@Data
public class CureFileuploadRawPayload implements Serializable {

	private static final long serialVersionUID = 1L;

	private String cureEventName;
	private String cureEventResult;
	private String cureBusinessApplication;
	private String cureCorrelationId;
	private String identityCustomerIpAddress;
	private String identityChannelType;
	private String identityProducerApplicationName;
	private String fileId;
	private String productId;
	private String virusScanResult;
	private boolean isFileUploadProcessSuccess;
	private boolean isFileEncrypted;
	private long fileSize;
	private int errorCode;
	private String errorDeveloperText;
	private int httpStatusCode;

	public CureFileuploadRawPayload() {
	}

	public CureFileuploadRawPayload(String cureCorrelationId, String identityCustomerIpAddress, //NOSONAR
			String identityChannelType, String fileId, String productId, long fileSize, String virusScanResult, boolean isFileEncrypted,
			boolean isFileUploadProcessSuccess, int errorCode, String errorDeveloperText, int httpStatusCode) {
		super();
		this.cureCorrelationId = cureCorrelationId;
		this.identityCustomerIpAddress = identityCustomerIpAddress;
		this.identityChannelType = identityChannelType;
		this.fileId = fileId;
		this.productId = productId;
		this.fileSize = fileSize;
		this.virusScanResult = virusScanResult;
		this.isFileEncrypted = isFileEncrypted;
		this.isFileUploadProcessSuccess = isFileUploadProcessSuccess;
		this.errorCode = errorCode;
		this.errorDeveloperText = errorDeveloperText;
		this.httpStatusCode = httpStatusCode;
	}

	public CureFileuploadRawPayload(String cureCorrelationId, String identityCustomerIpAddress,
			String identityChannelType, String productId, long fileSize, int errorCode, String errorDeveloperText,
			int httpStatusCode) { //NOSONAR
		super();
		this.cureCorrelationId = cureCorrelationId;
		this.identityCustomerIpAddress = identityCustomerIpAddress;
		this.identityChannelType = identityChannelType;
		this.productId = productId;
		this.fileSize = fileSize;
		this.errorCode = errorCode;
		this.errorDeveloperText = errorDeveloperText;
		this.httpStatusCode = httpStatusCode;
	}

}