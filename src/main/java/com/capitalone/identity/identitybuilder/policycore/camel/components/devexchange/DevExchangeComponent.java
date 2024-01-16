package com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.service.dao.DevExchangeDAO;
import org.apache.camel.Endpoint;
import org.apache.camel.PropertyInject;
import org.apache.camel.spi.Metadata;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Represents the component that manages {@link DevExchangeEndpoint}.
 */
public class DevExchangeComponent extends DefaultComponent {
	// The pattern for the URI after the component prefix (<serviceName>:http[s]://<path>)
	@SuppressWarnings("squid:S4784")
	private static final Pattern PATTERN = Pattern.compile(".+:https?:\\/\\/.+");

    @Metadata(label = "configuration", description = "DevExchange DAO")
    @Autowired
    private DevExchangeDAO devExchangeDAO;

    @Metadata(label = "configuration", description = "Logging fields to filter")
    @PropertyInject("logs.fieldstofilter")
    private String fieldsToFilter;

    @Metadata(label = "configuration", description = "Partner access token URI")
    @Value("${security.oauth2.client.partnerAccessTokenUri:}")
    private String partnerAccessTokenUri;

    @Value("${identitybuilder.policycore.feature.mock-mode.url}")
    private String mockBaseUrl;

    @Value("${identitybuilder.policycore.feature.mock-mode.enabled: false}")
    private boolean mockFeatureEnabled;

    @Value("${ENVIRONMENT_NAME:UNK}")
    private String environmentName;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        DevExchangeEndpoint devExchangeEndpoint = new DevExchangeEndpoint(uri, this);

        if (!PATTERN.matcher(remaining).matches()) {
        	throw new IllegalArgumentException(String.format("Invalid uri %s - must be \"dx:<serviceName>:http[s]://<path>\"", uri));
        }

        String serviceName = StringHelper.before(remaining, ":");
        if (serviceName.equals("pdx") && (null == this.partnerAccessTokenUri || "".equals(this.partnerAccessTokenUri))) {
            throw new ChassisSystemException("Property value for security.oauth2.client.partnerAccessTokenUri must exist" +
                    "to use pdx component.");
        }

        devExchangeEndpoint.setServiceName(serviceName);
        String targetUrl = StringHelper.after(remaining, ":");

        devExchangeEndpoint.setTargetUrl(targetUrl);
        setProperties(devExchangeEndpoint, parameters);

        if (!parameters.isEmpty()) {
            // use only what remains and at this point parameters that have been used have been removed
            // without overwriting any query parameters set via queryParameters endpoint option
            final Map<String, Object> queryParameters = new LinkedHashMap<>(parameters);
            final Map<String, Object> existingQueryParameters = URISupport.parseQuery(devExchangeEndpoint.getQueryParameters());
            queryParameters.putAll(existingQueryParameters);

            final String remainingParameters = URISupport.createQueryString(queryParameters);
            devExchangeEndpoint.setQueryParameters(remainingParameters);
        }

        devExchangeEndpoint.setParameters(parameters);

        return devExchangeEndpoint;
    }

    public DevExchangeDAO getDevExchangeDAO() {
        return devExchangeDAO;
    }

    public void setDevExchangeDAO(DevExchangeDAO devExchangeDAO) {
        this.devExchangeDAO = devExchangeDAO;
    }

    public String getFieldsToFilter() {
        return fieldsToFilter;
    }

    public void setFieldsToFilter(String fieldsToFilter) {
        this.fieldsToFilter = fieldsToFilter;
    }

    public String getPartnerAccessTokenUri() { return partnerAccessTokenUri; }

    public void setPartnerAccessTokenUri(String partnerAccessTokenUri) { this.partnerAccessTokenUri = partnerAccessTokenUri; }

    public boolean isMockFeatureEnabled() { return !environmentName.toLowerCase(Locale.ROOT).contains("prod") && mockFeatureEnabled; }

    public void setMockFeatureEnabled(boolean mockFeatureEnabled) { this.mockFeatureEnabled = mockFeatureEnabled; }

    public String getMockBaseUrl() { return mockBaseUrl; }

    public void setMockBaseUrl(String mockBaseUrl) { this.mockBaseUrl = mockBaseUrl; }

    public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }

}

/*
 * Copyright 2018 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */
