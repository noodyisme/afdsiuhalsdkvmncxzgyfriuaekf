package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.chassis.engine.model.response.ResponseData;
import com.capitalone.identity.identitybuilder.policycore.model.APIResponse;
import com.capitalone.identity.identitybuilder.policycore.model.JourneyPolicyResponse;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyMetadata;
import com.capitalone.identity.identitybuilder.policycore.model.PublicAPIRequest;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

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
public class PublicPolicyResource {
    public static final String INVALID_CCID = "Missing client correlation Id in the header";
    public static final String INVALID_IP_ADDRESS = "Invalid or missing Customer-IP-Address in header";
    public static final String INVALID_CHANNEL_TYPE = "Missing channel type in the header";
    public static final String CHANNEL_TYPE = "Channel-Type";
    public static final String CLIENT_CORRELATION_ID = "Client-Correlation-Id";
    public static final String CUSTOMER_IP_ADDRESS = "Customer-Ip-Address";
    public static final int BAD_REQUEST_STATUS_CODE = Response.Status.BAD_REQUEST.getStatusCode();
    public static final String INVALID_POLICY_NAME_EXCEPTION = "policyName query parameter is required";
    public static final String INVALID_POLICY_VERSION_EXCEPTION = "policyVersion query parameter is required";
    public static final String CHASSIS_ERROR_CODE_MISSING_CLIENT_CORRELATION_ID = "202189";
    public static final String CHASSIS_ERROR_CODE_MISSING_CHANNEL_TYPE = "202191";
    public static final String CHASSIS_ERROR_CODE_MISSING_CUSTOMER_IP_ADDRESS = "201892";

    // Note: This is managed by Jersey which doesn't understand constructor injection.
    @Inject
    ResponseData respData;

    @Autowired
    private Environment env;

    @Inject
    private PolicyService policyService;


    @POST
    @Path("/initiate-policy")
    public Response executeByQuery(@BeanParam PublicAPIRequest publicApiRequest, Map<String, Serializable> requestBody, @Context HttpHeaders headers) {
        return execute(publicApiRequest, requestBody, headers);
    }

    @POST
    @Path("/initiate-policy/{policyName}/{policyVersion}")
    public Response executeByPath(@BeanParam PublicAPIRequest publicApiRequest, @PathParam("policyName") String policyName, @PathParam("policyVersion") String policyVersion, Map<String, Serializable> requestBody, @Context HttpHeaders headers) {
        publicApiRequest = publicApiRequest.toBuilder()
                .policyName(policyName)
                .policyVersion(policyVersion)
                .build();
        return execute(publicApiRequest, requestBody, headers);
    }


    @GET
    @Path("/policy-definitions")
    public Response getPolicyMetadataByQuery(@QueryParam("policyName") String policyName, @QueryParam("policyVersion") String version, @QueryParam("step") String stepName, @Context HttpHeaders headers) {
        return getPolicyMetadata(policyName, version, stepName, headers);
    }

    @GET
    @Path("/policy-definitions/{policyName}/{policyVersion}")
    public Response getPolicyMetadataByPath(@PathParam("policyName") String policyName, @PathParam("policyVersion") String version, @Context HttpHeaders headers) {
        return getPolicyMetadata(policyName, version, null, headers);
    }

    @GET
    @Path("/policy-definitions/{policyName}/{policyVersion}/{step}")
    public Response getPolicyMetadataByPathWithStep(@PathParam("policyName") String policyName, @PathParam("policyVersion") String version, @PathParam("step") String stepName, @Context HttpHeaders headers) {
        return getPolicyMetadata(policyName, version, stepName, headers);
    }

    public Response getPolicyMetadata(@QueryParam("policyName") String policyName, @QueryParam("policyVersion") String version, @QueryParam("step") String stepName, @Context HttpHeaders headers) {
        validateParameters(policyName, version);
        validateHeaders(headers);
        PolicyMetadata policyMetaData = policyService.getPolicyMetadata(policyName, version, stepName);
        respData.setStatusPreference(Response.Status.OK.getStatusCode());
        return Response.status(Response.Status.OK).entity(policyMetaData).build();
    }


    Response execute(@BeanParam PublicAPIRequest publicApiRequest, Map<String, Serializable> requestBody, @Context HttpHeaders headers) {
        validateParameters(publicApiRequest.getPolicyName(), publicApiRequest.getPolicyVersion());
        validateHeaders(headers);
        APIResponse.APISuccessResponse policyCoreResponse = (APIResponse.APISuccessResponse) policyService.createProcess(
                PublicAPIRequest.toAPIRequest(publicApiRequest), requestBody);

        JourneyPolicyResponse journeyPolicyResponse = new JourneyPolicyResponse(policyCoreResponse);
        respData.setStatusPreference(Response.Status.OK.getStatusCode());
        return Response.status(Response.Status.OK).entity(journeyPolicyResponse).build();
    }

    private void validateParameters(String policyName, String policyVersion) {
        if (StringUtils.isBlank(policyName)) {
            throw new ChassisBusinessException(INVALID_POLICY_NAME_EXCEPTION);
        }

        if (StringUtils.isBlank(policyVersion)) {
            throw new ChassisBusinessException(INVALID_POLICY_VERSION_EXCEPTION);
        }
    }

    protected void validateHeaders(HttpHeaders headers){
        boolean headerValidationEnabled = Boolean.parseBoolean(env.getProperty("identitybuilder.policycore.feature.public-endpoint-header-validation.enabled"));
        if(!headerValidationEnabled){
            return;
        }

        InetAddressValidator validator = InetAddressValidator.getInstance();
        if(headers.getRequestHeader(CLIENT_CORRELATION_ID)==null || StringUtils.isBlank(headers.getRequestHeader(CLIENT_CORRELATION_ID).get(0))){
            throw new ChassisBusinessException(new ChassisErrorCode(CHASSIS_ERROR_CODE_MISSING_CLIENT_CORRELATION_ID), INVALID_CCID, BAD_REQUEST_STATUS_CODE);
        }

        if(headers.getRequestHeader(CHANNEL_TYPE)==null || StringUtils.isBlank(headers.getRequestHeader(CHANNEL_TYPE).get(0))){
            throw new ChassisBusinessException(new ChassisErrorCode(CHASSIS_ERROR_CODE_MISSING_CHANNEL_TYPE), INVALID_CHANNEL_TYPE, BAD_REQUEST_STATUS_CODE);
        }

        if(headers.getRequestHeader(CUSTOMER_IP_ADDRESS)==null || !validator.isValid(headers.getRequestHeader(CUSTOMER_IP_ADDRESS).get(0))){
            throw new ChassisBusinessException(new ChassisErrorCode(CHASSIS_ERROR_CODE_MISSING_CUSTOMER_IP_ADDRESS), INVALID_IP_ADDRESS, BAD_REQUEST_STATUS_CODE);
        }
    }

}

