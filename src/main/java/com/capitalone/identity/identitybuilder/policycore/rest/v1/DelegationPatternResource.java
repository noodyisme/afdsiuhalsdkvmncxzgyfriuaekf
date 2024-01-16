package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.annotations.logging.Profile;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;
import io.swagger.annotations.Api;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Resources in this class can be used to implement the 'delegation pattern'
 * Domains should perform the following actions to implement this pattern effectively
 * <ol>
 *     <li>Set {@code server.servlet.context-path=/} to support requests forwarded by the Gateway from '/masterbuilder-2-0-web' base context</li>
 *     <li>Migrate from {@link com.capitalone.identity.identitybuilder.policycore.rest.v1.PolicyResource} to {@link DomainResource}, which has '/domain-web' prefix hardcoded to continue support of private domain requests</li>
 *     <li>Migrate from {@link com.capitalone.identity.identitybuilder.policycore.fileupload.rest.v1.FileUploadResource} to {@link FileUploadService}, which has '/domain-web' prefix hardcoded to continue support of private domain requests</li>
 *     <li>Finally, add {@link JourneyResource}, which has '/masterbuilder-2-0-web' prefix hardcoded to receive requests forwarded from MB 2.0 endpoint</li>
 * </ol>
 */
public class DelegationPatternResource {

    private DelegationPatternResource() {
    }

    @Path("/masterbuilder-2-0-web/identity/workflow-management")
    @Api(value = "/masterbuilder-2-0-web/identity/workflow-management")
    @Produces({"application/vnd.com.capitalone.api+v2+json", MediaType.APPLICATION_JSON})
    @Consumes({"application/vnd.com.capitalone.api+v2+json", MediaType.APPLICATION_JSON})
    public static final class JourneyResource extends com.capitalone.identity.identitybuilder.policycore.rest.v1.PublicPolicyResource {
    }

    @Path("/domain-web/private/189898/identity/domain/services")
    @Api(value = "/domain-web/private/189898/identity/domain/services")
    @Produces({"application/vnd.com.capitalone.api+v1+json", MediaType.APPLICATION_JSON})
    @Consumes({"application/vnd.com.capitalone.api+v1+json", MediaType.APPLICATION_JSON})
    public static class DomainResource extends com.capitalone.identity.identitybuilder.policycore.rest.v1.PolicyResource {
    }

    @Log
    @Profile
    @Component
    @Singleton
    @Path("/domain-web/private/189898/identity/domain/services")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({"application/vnd.com.capitalone.api+v1+json", MediaType.APPLICATION_JSON})
    @ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
    public static class DomainFileUploadResource extends com.capitalone.identity.identitybuilder.policycore.fileupload.rest.v1.FileUploadResource {
    }
}
