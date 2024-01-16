package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.model.response.ResponseData;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/private/189898/identity/domain/services")
@Api(value = "/identity/services/execute")
@Produces({"application/vnd.com.capitalone.api+v1+json", MediaType.APPLICATION_JSON})
@Consumes({"application/vnd.com.capitalone.api+v1+json", MediaType.APPLICATION_JSON})
public class PolicyResource {

    // Note: This is managed by Jersey which doesn't understand constructor injection.
    @Inject
    ResponseData respData;

    @Inject
    private PolicyService policyService;

    @POST
    @Path("/execute/{policy}/{version}")
    public Response createProcess(@BeanParam APIRequest apiRequest, ExecutePolicyRequest request) {
        APIResponse response = policyService.createProcess(apiRequest, request.getPolicyParameters());
        respData.setStatusPreference(Response.Status.OK.getStatusCode());
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/execute/{policy}/{version}")
    public Response getPolicyMetadata(@BeanParam ExecutionContext executionContext, @PathParam("policy") String policyName, @PathParam("version") String version, @QueryParam("step") String step) {
        PolicyMetadata response = policyService.getPolicyMetadata(policyName, version, step);
        respData.setStatusPreference(Response.Status.OK.getStatusCode());
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @PATCH
    @Path("/execute/{resourceId}")
    public Response resumeProcess(@BeanParam APIRequest apiRequest, ExecutePolicyRequest request) {
        APIResponse response = policyService.resumeProcess(apiRequest, request.getPolicyParameters());
        respData.setStatusPreference(Response.Status.OK.getStatusCode());
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/execute/{resourceId}")
    public Response getProcessMetadata(@BeanParam ExecutionContext executionContext, @PathParam("resourceId") String resourceId) {
        ProcessMetadata response = policyService.getProcessMetadata(resourceId);
        respData.setStatusPreference(Response.Status.OK.getStatusCode());
        return Response.status(Response.Status.OK).entity(response).build();
    }
}
