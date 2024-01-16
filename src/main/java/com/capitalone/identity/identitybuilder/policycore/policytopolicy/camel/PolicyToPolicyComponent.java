package com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel;

import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.PolicyValidator;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyServiceExecutor;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@org.apache.camel.spi.annotations.Component("P2P")
public class PolicyToPolicyComponent extends DefaultComponent {

    @Autowired
    private PolicyServiceExecutor policyServiceExecutor;

    @Autowired
    private PolicyValidator policyValidator;

    @Autowired
    private PolicyVersionService policyVersionService;

    @Value("${policy.service.logs.fieldstofilter:}")
    private String fieldsToFilter;

    @Value("${chassis.app.name}")
    private String appName;

    @Value("${chassis.app.code}")
    private String applicationId;

    /**
     * creates the endpoint for the policy to policy camel component. Sets the tenant, business unit, policy name, and
     * policy version for the endpoint
     *
     * @param  uri the full URI of the policy to policy endpoint (ex: P2P:tenant/businessUnit/policyName/policyVersion)
     * @param  remaining the remaining endpoint after "P2P:"
     * @param  parameters a Map of parameters for the endpoint
     * @return a {@link PolicyToPolicyEndpoint}
     */
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        PolicyToPolicyEndpoint policyToPolicyEndpoint = new PolicyToPolicyEndpoint(uri, this, policyServiceExecutor,
                policyValidator, policyVersionService, fieldsToFilter, appName, applicationId);

        String[] values = remaining.split("/");

        if(values.length == 4){
            policyToPolicyEndpoint.setTenant(values[0]);
            policyToPolicyEndpoint.setBusinessUnit(values[1]);
            policyToPolicyEndpoint.setPolicyName(values[2]);
            policyToPolicyEndpoint.setPolicyVersion(values[3]);
        } else {
            throw new PolicyToPolicyException("The P2P component must use the full policy name followed by either a " +
                    "major or minor version. The syntax is \"P2P:tenant/businessUnit/policyName/policyVersion\"");
        }

        setProperties(policyToPolicyEndpoint, parameters);

        return policyToPolicyEndpoint;
    }
}
