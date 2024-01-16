package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.model.exception.ChassisHTTPStatusMessageConstants;
import com.capitalone.chassis.engine.model.response.ResponseData;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated_Publisher;
import com.capitalone.identity.identitybuilder.policycore.service.HealthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.http.HttpStatus;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.net.HttpURLConnection;

@Named
@Log
@Path("/health")
@Api(value = "/health")
public class HealthResource {

    @Inject
    ResponseData respData;

    @Inject
    HealthService healthService;

    @Inject
    HealthCheckEvaluated_Publisher healthCheckPublisher;

    @GET
    @ApiOperation(
            value = "Health Check",
            notes = "ELB Health Check",
            response = ResponseData.class)

    @ApiResponses({@ApiResponse(code = HttpURLConnection.HTTP_NO_CONTENT,
            message = ChassisHTTPStatusMessageConstants.SUCCESS_MESSAGE),
            @ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST,
                    message = ChassisHTTPStatusMessageConstants.BAD_REQUEST_MESSAGE),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED,
                    message = ChassisHTTPStatusMessageConstants.UNAUTHORIZED_MESSAGE),
            @ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN,
                    message = ChassisHTTPStatusMessageConstants.FORBIDDEN_MESSAGE),
            @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = ChassisHTTPStatusMessageConstants.NOT_FOUND_MESSAGE),
            @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = ChassisHTTPStatusMessageConstants.INTERNAL_SERVER_ERROR_MESSAGE)})
    public void healthCheck(@Context HttpServletRequest request) {
        if (!healthService.isReady()) {
            // Chassis seems to prevent less used code the code below will be a 404
            respData.setStatusPreference(HttpStatus.SC_FAILED_DEPENDENCY);
            healthCheckPublisher.publishEvent(new HealthCheckEvaluated(respData.getStatusPreference()));
            return;
        }
        request.setAttribute("NO_ACCESS_LOG", "true");
        respData.setStatusPreference(HttpStatus.SC_OK);
        healthCheckPublisher.publishEvent(new HealthCheckEvaluated(respData.getStatusPreference()));
    }
}
