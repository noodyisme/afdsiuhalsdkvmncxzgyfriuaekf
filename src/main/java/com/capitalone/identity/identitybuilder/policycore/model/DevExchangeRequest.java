package com.capitalone.identity.identitybuilder.policycore.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;
/**
 *  This Request class is created to facilate auditlogging of DAO calls
 * @author peg304 - 
 * @since  1.0
 */
public class DevExchangeRequest {
	private HttpHeaders httpHeaders;

	private HttpMethod httpMethod;

	private Object requestBody;
	
	private String requestBodyAudit;
	
	private String serviceName;

	private URI uri;

	private String policyInfo;

	/**
	 * Scheme is used to identify type of gateway like internal gateway or any one of the external gateways.
	 */
	private String scheme;

	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public Object getRequestBody() {
		return requestBody;
	}

	public String getRequestBodyAudit() {
		return requestBodyAudit;
	}

	public String getServiceName() {
		return serviceName;
	}

	public URI getUri() {
		return uri;
	}

	public String getScheme() { return scheme; }

	public void setHttpHeaders(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	public void setRequestBody(Object requestBody) {
		this.requestBody = requestBody;
	}

	public void setRequestBodyAudit(String requestBodyAudit) {
		this.requestBodyAudit = requestBodyAudit;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public void setScheme(String scheme) { this.scheme = scheme; }

	public String getPolicyInfo() {
		return policyInfo;
	}

	public void setPolicyInfo(String policyInfo) {
		this.policyInfo = policyInfo;
	}

	@Override
	public String toString() {
	    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@Override
	public boolean equals(Object obj) {
	    return EqualsBuilder.reflectionEquals(this, obj, false);
	}

	@Override
	public int hashCode() {
	   return HashCodeBuilder.reflectionHashCode(this, false);
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
