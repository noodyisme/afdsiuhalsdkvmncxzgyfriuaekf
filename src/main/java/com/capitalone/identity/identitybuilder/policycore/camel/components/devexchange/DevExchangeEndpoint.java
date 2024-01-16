package com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;

/**
 * Represents a Policy endpoint.
 */
@UriEndpoint(
    firstVersion = "01.01.00.117-SNAPSHOT",
    scheme = "dx,pdx",
    title = "DevExchange",
    syntax="dx:serviceName:targetUrlOrUrlTemplate",
    label = "custom,devexchange,devex,dx,pdx")
public class DevExchangeEndpoint extends DefaultEndpoint {
	@UriPath(label = "common") @Metadata(required = true)
    private String serviceName;
    @UriPath(label = "common")
    private String targetUrl;

    @UriParam(label = "common", enums = "get,post,put,patch,options,delete", defaultValue = "post")
    private String method = "post";
    @UriParam(label = "common", defaultValue = "1")
    private String dxVersion = "1";
    @UriParam(label = "common", defaultValue = "true")
    private boolean copyClientIP = true;
    @UriParam(label = "common", defaultValue = "false")
    private boolean mobileWebToMobile = false;
    @UriParam(label = "common", defaultValue = "")
    private String multipartId = "";
    @UriParam(label = "common", defaultValue = "false")
    private boolean customMultipart;
    @UriParam(label = "producer", multiValue = true)
    private String queryParameters;
    @UriParam(label = "common", defaultValue = "false")
    private boolean versionContentType = false;

    private String mockTargetUrl;
    private Map<String, Object> parameters;

    public DevExchangeEndpoint(String uri, DevExchangeComponent component) {
        super(uri, component);
    }

    /**
     * This is overridden to avoid searching the registry for a {@link org.apache.camel.spi.RestProducerFactory}.
     * We only care about configuring our custom {@link Producer}.
     * @return a configured {@link DevExchangeProducer}
     * @throws Exception
     */
    @Override
    public Producer createProducer() throws Exception {
        return new DevExchangeProducer(this);
    }

    @Override
    public DevExchangeComponent getComponent() {
        return (DevExchangeComponent)super.getComponent();
    }

    /**
     * We're not as interested in the Consumer side of things so we'll override this method
     * to do nothing except throw an exception if we try to use it as a consumer.
     * 
     * @param processor
     * @throws UnsupportedOperationException this component does not support a consumer
     */
    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException("The DevExchange endpoint doesn't support consumers.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
    
    @Override
    public boolean isLenientProperties() {
    	return true;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) throws MalformedURLException {
        this.targetUrl = targetUrl;
        this.mockTargetUrl = targetUrl;

        // If mock mode feature is enabled, create the mock url
        if(getComponent().isMockFeatureEnabled()) {
            URL targetUrlObject = new URL(targetUrl);
            this.mockTargetUrl = UriComponentsBuilder.fromHttpUrl(getComponent().getMockBaseUrl()).path(targetUrlObject.getPath()).build().toString();
        }
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(String queryParameters) {
        this.queryParameters = queryParameters;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getDxVersion() {
        return dxVersion;
    }

    public void setDxVersion(String dxVersion) {
        this.dxVersion = dxVersion;
    }

	public boolean isCopyClientIP() {
		return copyClientIP;
	}

	public void setCopyClientIP(boolean copyClientIP) {
		this.copyClientIP = copyClientIP;
	}

	public boolean isMobileWebToMobile() {
		return mobileWebToMobile;
	}

	public void setMobileWebToMobile(boolean mobileWebToMobile) {
		this.mobileWebToMobile = mobileWebToMobile;
	}
	
	public String getMultipartId() {
		return multipartId;
	}
	
	public void setMultipartId(String multipartId) {
		this.multipartId = multipartId;
	}

    public boolean getCustomMultipart() {
        return customMultipart;
    }

    public void setCustomMultipart(boolean customMultipart) {
        this.customMultipart = customMultipart;
    }
	
	public boolean isVersionContentType() {
		return versionContentType;
	}

	public void setVersionContentType(boolean versionContentType) {
		this.versionContentType = versionContentType;
	}

    public String getMockTargetUrl() { return mockTargetUrl; }

	/**
	 * We override equals to distinguish between endpoints that have the
	 * following different settings:
	 * <ul>
	 * <li>Service name</li>
	 * <li>Target URL</li>
	 * <li>HTTP method</li>
	 * <li>API version</li>
	 * </ul>
	 * <p>
	 * Plus, Sonarcube will complain if we don't.
	 * 
	 * @param object the endpoint to compare this object with
	 */
    @Override
    public boolean equals(Object object) {
        if (super.equals(object)) {
            DevExchangeEndpoint that = (DevExchangeEndpoint) object;
            return this.serviceName.equals(that.serviceName)
                    && this.targetUrl.equals(that.targetUrl)
                    && this.method.equals(that.method)
                    && this.dxVersion.equals(that.dxVersion)
                    && this.customMultipart == that.customMultipart;
        }
        return false;
    }
    
    /**
     * When we override equals we need to override hashCode too.
     * 
     * @return a hashcode that incorporates all the values used by equals()
     */
    @Override
    public int hashCode() {
        return (super.hashCode() + serviceName.hashCode() + targetUrl.hashCode()
                + method.hashCode() + dxVersion.hashCode() + Boolean.hashCode(customMultipart)) * 29 + 1;
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