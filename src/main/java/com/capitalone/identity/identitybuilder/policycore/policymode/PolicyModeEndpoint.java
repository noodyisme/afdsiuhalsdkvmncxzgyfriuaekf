package com.capitalone.identity.identitybuilder.policycore.policymode;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.configmanagement.MatchingStrategies;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.DefaultProducer;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@UriEndpoint(
        firstVersion = "1.38-SNAPSHOT",
        scheme = "policy-mode",
        title = "PolicyModeEndpoint",
        syntax = "policy-mode:operationEnvironment/policyOperationMode")
public class PolicyModeEndpoint extends DefaultEndpoint {
    public static final String SCHEME = "policy-mode";
    public static final String POLICY_ID_HEADER_KEY = "policyId";
    public static final String NON_PROD_MOCK_MODE = "non-prod/policy-core.mockMode";
    public static final String MOCK_MODE_PROPERTY_NAME = "policy-core.mockMode";

    private final ConfigManagementService configManagementService;
    private final boolean mockFeatureEnabled;

    @UriParam()
    private String policyId;

    public PolicyModeEndpoint(String endpointUri, Component component, ConfigManagementService configManagementService,
                              boolean mockFeatureEnabled, String environmentName) {
        super(endpointUri, component);
        this.configManagementService = configManagementService;
        this.mockFeatureEnabled = !environmentName.toLowerCase(Locale.ROOT).contains("prod") && mockFeatureEnabled;
    }

    /**
     * We override equals to distinguish between endpoints that have the
     * following different settings:
     * <ul>
     * <li>Mock Feature Enabled</li>
     * <li>Policy ID</li>
     * </ul>
     * <p>
     * Plus, Sonarcube will complain if we don't.
     *
     * @param object the endpoint to compare this object with
     */
    @Override
    public boolean equals(Object object) {
        if (super.equals(object)) {
            PolicyModeEndpoint that = (PolicyModeEndpoint) object;
            return this.mockFeatureEnabled == that.mockFeatureEnabled &&
                    Objects.equals(this.policyId, that.policyId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mockFeatureEnabled, policyId);
    }

    @Override
    public void configureProperties(Map<String, Object> options) {
        policyId = (String) options.get(POLICY_ID_HEADER_KEY);

        if(!getEndpointUri().startsWith("policy-mode:")){
            throw new UnsupportedOperationException("Policy Mode URI calls must start with 'policy-mode:'");
        }
    }

    @Override
    public Producer createProducer() throws Exception {
        String[] baseUriParts = getEndpointBaseUri().split("://");
        String policyMode = baseUriParts[baseUriParts.length-1];
        ConfigManagementModel configManagementModel = configManagementService.getPolicyConfiguration(policyId);

        if(policyMode.equals(NON_PROD_MOCK_MODE)) {
            return createMockModeProducer(configManagementModel, policyMode);
        }

        throw  new UnsupportedOperationException(String.format(
                "Policy Mode provided is invalid [mode = '%s', resolved endpoint = '%s']"
                , policyMode, getEndpointUri()
        ));
    }

    private Producer createMockModeProducer(ConfigManagementModel configManagementModel, String policyMode) {
        // Use a no-op default producer if mock mode feature is not enabled
        if(!mockFeatureEnabled) {
            return new DefaultProducer(this) {
                @Override
                public void process(Exchange exchange) throws Exception {
                    // Mock Mode producer does nothing if feature is disabled
                }
            };
        }

        if(configManagementModel == null) {
            throw new UnsupportedOperationException(String.format(
                    "Policy Configuration not found for Policy Mode '%s' [resolved endpoint = '%s']"
                    , policyMode, getEndpointUri()
            ));
        }

        Optional<Object> mockModeEnabled = configManagementModel.getValue(MOCK_MODE_PROPERTY_NAME,null, MatchingStrategies.MATCH_FEATURES_ONLY);

        if(!mockModeEnabled.isPresent() || !(mockModeEnabled.get() instanceof Boolean)) {
            throw new UnsupportedOperationException(String.format(
                    "Policy Configuration is invalid for Policy Mode '%s' [expected property name: '%s'," +
                            " expected property type: boolean, resolved endpoint = '%s']"
                    , policyMode, MOCK_MODE_PROPERTY_NAME, getEndpointUri()
            ));
        }

        return new PolicyModeProducer(this, (boolean) mockModeEnabled.get());
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("The PolicyMode endpoint doesn't support consumers.");
    }

    @Override
    public boolean isLenientProperties() {
        return true;
    }

    public ConfigManagementService getConfigManagementService() {
        return configManagementService;
    }

    public boolean isMockFeatureEnabled() {
        return mockFeatureEnabled;
    }

    public String getPolicyId() {
        return policyId;
    }
}
