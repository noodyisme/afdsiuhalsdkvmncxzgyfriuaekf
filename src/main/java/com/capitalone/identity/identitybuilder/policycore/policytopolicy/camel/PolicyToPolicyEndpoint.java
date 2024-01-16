package com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel;


import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.PolicyValidator;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyServiceExecutor;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@UriEndpoint(
        firstVersion = "1.0-SNAPSHOT",
        scheme = "P2P",
        title = "PolicyToPolicyEndpoint",
        syntax = "P2P:tenant/businessUnit/policyName/policyVersion")
public class PolicyToPolicyEndpoint extends DefaultEndpoint {

    @UriPath(label = "common")
    private String tenant;
    @UriPath(label = "common")
    private String businessUnit;
    @UriPath(label = "common")
    private String policyName;
    // policy version can be either the full major.minor version or just the major version (ex: 1.0, 2, 2.1)
    @UriPath(label = "common")
    private String policyVersion;
    private final PolicyServiceExecutor policyServiceExecutor;
    private final PolicyValidator policyValidator;
    private final PolicyVersionService policyVersionService;
    private final String fieldsToFilter;
    private final String appName;
    private final String applicationId;

    public PolicyToPolicyEndpoint(String endpointUri, Component component, PolicyServiceExecutor policyServiceExecutor, PolicyValidator policyValidator, //NOSONAR
                                  PolicyVersionService policyVersionService, String fieldsToFilter, String appName, String applicationId) {
        super(endpointUri, component);
        this.policyServiceExecutor = policyServiceExecutor;
        this.policyValidator = policyValidator;
        this.policyVersionService = policyVersionService;
        this.fieldsToFilter = fieldsToFilter;
        this.appName = appName;
        this.applicationId = applicationId;
    }


    @Override
    public Producer createProducer() {
        return new PolicyToPolicyProducer(this, policyServiceExecutor, policyValidator, policyVersionService,
                fieldsToFilter, appName, applicationId);
    }

    @Override
    public PolicyToPolicyComponent getComponent() {
        return (PolicyToPolicyComponent) super.getComponent();
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
        throw new UnsupportedOperationException("The PolicyToPolicyEndpoint endpoint doesn't support consumers.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean isLenientProperties() {
        return false;
    }

}
