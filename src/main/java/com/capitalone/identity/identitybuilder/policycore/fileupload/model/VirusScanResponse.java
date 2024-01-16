package com.capitalone.identity.identitybuilder.policycore.fileupload.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class VirusScanResponse implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String definitionDate;
	private int totalBytesRead;
	private String fileName;
	private String status;
	private String malwareScanStatus;
}
