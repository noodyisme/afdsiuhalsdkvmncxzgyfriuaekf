package com.capitalone.identity.identitybuilder.policycore.model;

import org.springframework.http.HttpHeaders;

public class DevExchangeResponse {
	
	private String dxResponse;
	
	private String dxResponseAudit;
	
	private int httpStatus;
	
	private HttpHeaders httpHeaders;

	public String getDxResponse() {
		return dxResponse;
	}

	public void setDxResponse(String dxResponse) {
		this.dxResponse = dxResponse;
	}

	public String getDxResponseAudit() {
		return dxResponseAudit;
	}

	public void setDxResponseAudit(String dxResponseAudit) {
		this.dxResponseAudit = dxResponseAudit;
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(int httpStatus) {
		this.httpStatus = httpStatus;
	}
	
	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}
	
	public void setHttpHeaders(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}
	
	
	

}


/*
 * Copyright 2018 Capital One Financial Corporation All Rights Reserved.
 * 
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */
