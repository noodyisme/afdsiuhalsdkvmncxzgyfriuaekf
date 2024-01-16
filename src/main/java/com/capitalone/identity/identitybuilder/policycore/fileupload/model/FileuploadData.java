package com.capitalone.identity.identitybuilder.policycore.fileupload.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class FileuploadData implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String clientCorrelationId;
	private String customerIpAddress; 
	private String channelType; 
	private String xUpstreamEnv; 
	private String productId;
	private byte[] document;
	
	
	public FileuploadData(String clientCorrelationId, String customerIpAddress,
			String channelType, String xUpstreamEnv, String productId, byte[] document) {
		this.channelType = channelType;
		this.clientCorrelationId = clientCorrelationId;
		this.customerIpAddress = customerIpAddress;
		this.document = document;
		this.productId = productId;
		this.xUpstreamEnv = xUpstreamEnv;
	}

}
