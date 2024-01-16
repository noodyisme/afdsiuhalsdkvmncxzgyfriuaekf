package com.capitalone.identity.identitybuilder.policycore.camel.components.jwt;

import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService;
import lombok.EqualsAndHashCode;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;

/*
Examples :
        <to uri="crypto-jwt:generate" />
        <to uri="crypto-jwt:validate?srcTokenHeader=customerIDToken" />
        <to uri="crypto-jwt:extract?srcTokenHeader=customerIDToken&amp;fieldsToExtract=&amp;validateProductId=false" />
        <to uri="crypto-jwt:extract?srcTokenHeader=customerIDToken&amp;fieldsToExtract=iss,customerAppDataRefKey" />
        <to uri="crypto-jwt:extract?srcTokenHeader=customerIDToken&amp;fieldsToExtract=" />
        <to uri="crypto-jwt:extract?srcTokenHeader=customerIDToken&amp;srcTokenPath=$.token&amp;fieldsToExtract=" />
        <to uri="crypto-jwt:validate?srcTokenPath=$.token" />
        <to uri="crypto-jwt:extract?srcTokenPath=$.token&amp;fieldsToExtract=iss,customerAppDataRefKey"/>
        <to uri="crypto-jwt:extract?srcTokenPath=$.token&amp;fieldsToExtract="/>
 */
@UriEndpoint(
        firstVersion = "01.24.SNAPSHOT",
        scheme = "crypto-jwt",
        title = "JwtEndpoint",
        syntax="crypto-jwt:operation:params")
@EqualsAndHashCode(callSuper = true)
public class JwtEndpoint extends DefaultEndpoint {

    private static final String NO_CONSUMER_SUPPORT_ERROR_MESSAGE = "JwtComponent does not support consumers";
    private final JwsService jwsService;
    private final JwtOperation jwtOperation;

    @UriParam
    private String srcTokenHeader;

    @UriParam
    private String srcTokenPath;

    @UriParam
    private String fieldsToExtract;

    @UriParam
    private Boolean validateProductId;

    public JwtEndpoint(String uri, JwtComponent component, JwtOperation jwtOperation, JwsService jwsService) {
        super(uri, component);
        this.jwsService = jwsService;
        this.jwtOperation = jwtOperation;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new JwtProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException(NO_CONSUMER_SUPPORT_ERROR_MESSAGE);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getSrcTokenPath() {
        return srcTokenPath;
    }

    @ManagedAttribute
    public void setSrcTokenPath(String srcTokenPath) {
        this.srcTokenPath = srcTokenPath;
    }

    public String getSrcTokenHeader() {
        return srcTokenHeader;
    }

    public String getFieldsToExtract() {
        return fieldsToExtract;
    }

    public Boolean getValidateProductId() {
        return validateProductId;
    }

    @ManagedAttribute
    public void setSrcTokenHeader(String srcTokenHeader) {
        this.srcTokenHeader = srcTokenHeader;
    }

    @ManagedAttribute
    public void setFieldsToExtract(String fieldsToExtract) {
        this.fieldsToExtract = fieldsToExtract;
    }

    @ManagedAttribute
    public void setValidateProductId(Boolean validateProductId) {
        this.validateProductId = validateProductId;
    }

    public JwsService getJwsService() {
        return jwsService;
    }

    protected JwtOperation getJwtOperation() {
        return jwtOperation;
    }

}
