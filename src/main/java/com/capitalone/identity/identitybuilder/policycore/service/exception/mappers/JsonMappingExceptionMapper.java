package com.capitalone.identity.identitybuilder.policycore.service.exception.mappers;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.converters.error.ChassisErrorCodeToErrorResponseConverter;
import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;

import com.fasterxml.jackson.databind.JsonMappingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.*;


/**
 * Overwrites an ExceptionMapper by the same name in {@link org.glassfish.jersey.jackson.internal.jackson.jaxrs.base}
 * Maps exception response with an {@link ErrorResponse} as the entity. Using the ErrorResponse class allows us to
 * take advantage of the chassis-framework team's built-in error suppression.
 * Priority annotation is used to set this ExceptionMapper to overwrite the one in the Jersey-Jackson package.
 */
@Provider
@Singleton
@Priority(Priorities.ENTITY_CODER)
@Log
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException>{
    private static final Logger LOG = LoggerFactory.getLogger(JsonMappingExceptionMapper.class);
    private ChassisErrorCodeToErrorResponseConverter converter;
    @Inject
    JsonMappingExceptionMapper(ChassisErrorCodeToErrorResponseConverter converter){
        this.converter = converter;
    }

    @Override
    public Response toResponse(JsonMappingException exception) {
        LOG.warn("JsonMappingExceptionMapper caught the following exception: ", exception);

        ChassisErrorCode chassisErrorCode = new ChassisErrorCode(INVALID_JSON_ENTITY_ID,
                INVALID_JSON_ENTITY_TEXT, INVALID_JSON_ENTITY_DEV_TEXT);
        ErrorResponse errorResponse = converter.convert(chassisErrorCode);

        return Response.status(HttpStatus.BAD_REQUEST.value()).entity(errorResponse).type(MediaType.APPLICATION_JSON).build();
    }
}