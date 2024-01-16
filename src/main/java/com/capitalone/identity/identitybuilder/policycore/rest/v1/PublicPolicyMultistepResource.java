package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.model.response.ResponseData;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Map;


@Path("/identity/workflow-management")
@Api(value = "/identity/workflow-management")
@Produces({"application/vnd.com.capitalone.api+v2+json", MediaType.APPLICATION_JSON})
@Consumes({"application/vnd.com.capitalone.api+v2+json", MediaType.APPLICATION_JSON})
public class PublicPolicyMultistepResource extends PublicPolicyResource{
    // Note: This is managed by Jersey which doesn't understand constructor injection.
    @Inject
    ResponseData respData;
    @Inject
    private PolicyService policyService;

    @POST
    @Path("/policy-processes/{resourceId}/continue-process")
    public Response resumeProcess(@BeanParam APIRequest apiRequest, Map<String, Serializable> requestBody, @Context HttpHeaders headers) {
        validateHeaders(headers);
        APIResponse.APISuccessResponse policyCoreResponse = (APIResponse.APISuccessResponse) policyService.resumeProcess(apiRequest, requestBody);
        JourneyPolicyResponse journeyResponse = new JourneyPolicyResponse(policyCoreResponse);
        respData.setStatusPreference(Response.Status.OK.getStatusCode());
        return Response.status(Response.Status.OK).entity(journeyResponse).build();
    }

    @GET
    @Path("/policy-processes/{policyProcessId}")
    public Response getProcessMetadata(@BeanParam ExecutionContext executionContext, @PathParam("policyProcessId") String policyProcessId, @Context HttpHeaders headers) {
        validateHeaders(headers);
        ProcessMetadata policyCoreResponse = policyService.getProcessMetadata(policyProcessId);
        JourneyProcessMetadata journeyProcessMetadata = new JourneyProcessMetadata(policyCoreResponse);
        respData.setStatusPreference(Response.Status.OK.getStatusCode());
        return  Response.status(Response.Status.OK).entity(journeyProcessMetadata).build();
    }
}
