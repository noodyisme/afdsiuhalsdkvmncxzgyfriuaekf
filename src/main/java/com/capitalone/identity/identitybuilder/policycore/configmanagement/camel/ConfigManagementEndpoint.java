package com.capitalone.identity.identitybuilder.policycore.configmanagement.camel;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;

import java.net.URI;
import java.util.*;

@UriEndpoint(
        firstVersion = "1.0-SNAPSHOT",
        scheme = "policy-configuration",
        title = "PolicyConfigurationEndpoint",
        syntax = "policy-configuration:property/nameOfProperty?outputHeaderName=outputHeader")
public class ConfigManagementEndpoint extends DefaultEndpoint {

    public static final String SCHEME = "policy-configuration";
    public static final String POLICY_ID_HEADER_KEY = "policyId";
    private static final String OUTPUT_HEADER_NAME_KEY = "outputHeaderName";

    @UriParam()
    private String headerName;

    @UriParam()
    private String policyId;

    private ConfigManagementService configManagementService;

    public ConfigManagementEndpoint(String endpointUri,
                                    Component component,
                                    ConfigManagementService configManagementService) {
        super(endpointUri, component);
        this.configManagementService = configManagementService;
    }

    @Override
    public void configureProperties(Map<String, Object> options) {
        headerName = (String) options.get(OUTPUT_HEADER_NAME_KEY);

        if (headerName == null) {
            String msg = String.format("Missing query parameter '%s' is needed to instruct policy-configuration " +
                    "component which header to place the property into.", OUTPUT_HEADER_NAME_KEY);
            throw new IllegalArgumentException(msg);
        }

        policyId = (String) options.get(POLICY_ID_HEADER_KEY);

        if(!getEndpointUri().startsWith("policy-configuration://property/")){
            throw new UnsupportedOperationException("Config Management URI calls must start with 'policy-configuration:" +
                    "property/'");
        }
    }

    @Override
    public Producer createProducer() {
        final URI uri = URI.create(getEndpointUri());
        final ConfigManagementModel configManagementModel = configManagementService.getPolicyConfiguration(policyId);
        if (configManagementModel != null) {
            final String[] pathElements = uri.getPath().split("/");
            return new ConfigManagementProducer(this, configManagementModel,
                    pathElements[pathElements.length - 1], headerName);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Policy Configuration not found for policy path '%s' [header=%s, resolved endpoint=%s]",
                    uri.getPath(), uri.getQuery(), getEndpointUri()
            ));
        }
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("The ConfigManagementEndpoint endpoint doesn't support consumers.");
    }

    public String getName() {
        return headerName;
    }

    public void setName(String name) {
        this.headerName = name;
    }

    @Override
    public boolean isLenientProperties() {
        return true;
    }

    @Override
    public boolean isSingletonProducer() {
        return true;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConfigManagementEndpoint that = (ConfigManagementEndpoint) o;
        return Objects.equals(headerName, that.headerName) &&
                Objects.equals(policyId, that.policyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), headerName, policyId);
    }
}
