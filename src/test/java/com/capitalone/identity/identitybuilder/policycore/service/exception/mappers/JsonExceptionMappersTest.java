package com.capitalone.identity.identitybuilder.policycore.service.exception.mappers;

import com.capitalone.chassis.engine.converters.error.ChassisErrorCodeToErrorResponseConverter;
import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.fasterxml.jackson.core.JsonParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;

import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
public class JsonExceptionMappersTest {

    @Mock
    ChassisErrorCodeToErrorResponseConverter errorCodeToErrorResponseConverter;

    @InjectMocks
    private JsonMappingExceptionMapper mappingExceptionMapper = new JsonMappingExceptionMapper(errorCodeToErrorResponseConverter);

    @InjectMocks
    private JsonProcessingExceptionMapper processingExceptionMapper = new JsonProcessingExceptionMapper(errorCodeToErrorResponseConverter);

    @InjectMocks
    private JsonParseExceptionMapper parseExceptionMapper = new JsonParseExceptionMapper(errorCodeToErrorResponseConverter);

    String message = "stack trace of error";
    @Mock
    JsonParser p;

    @Mock
    private JsonMappingException mappingException = new JsonMappingException(p, message);

    @Mock
    private JsonParseException parseException = new JsonParseException(p, message);

    private final JsonProcessingException processingException = Mockito.mock(JsonProcessingException.class);

    int status = Response.Status.BAD_REQUEST.getStatusCode();
    String id = INVALID_JSON_ENTITY_ID;
    String text = INVALID_JSON_ENTITY_TEXT;

    String devText = INVALID_JSON_ENTITY_DEV_TEXT;

    ErrorResponse errorResponse = new ErrorResponse(INVALID_JSON_ENTITY_ID, INVALID_JSON_ENTITY_TEXT, INVALID_JSON_ENTITY_DEV_TEXT, null, null);

    @BeforeEach
    public void init(){
        Mockito.when(errorCodeToErrorResponseConverter.convert(Mockito.any())).thenReturn(errorResponse);
    }


    public JsonExceptionMappersTest() {
    }


    @Test
    public void testToResponseJsonMappingException() {

        Response response = mappingExceptionMapper.toResponse(mappingException);
        ErrorResponse responseBody = (ErrorResponse) response.getEntity();

        assertEquals(status, response.getStatus());
        assertEquals(ErrorResponse.class, response.getEntity().getClass());
        assertEquals(text, responseBody.getText());
        assertEquals(devText, responseBody.getDeveloperText());
        assertEquals(id, responseBody.getId());
    }

    @Test
    public void testToResponseJsonParseException() {

        Response created = parseExceptionMapper.toResponse(parseException);
        ErrorResponse responseBody = (ErrorResponse) created.getEntity();

        assertEquals(status, created.getStatus());
        assertEquals(ErrorResponse.class, created.getEntity().getClass());
        assertEquals(text, responseBody.getText());
        assertEquals(devText, responseBody.getDeveloperText());
        assertEquals(id, responseBody.getId());

    }

    @Test
    public void testToResponeJsonProcessingException() {
        Response created = processingExceptionMapper.toResponse(processingException);

        ErrorResponse responseBody = (ErrorResponse) created.getEntity();

        assertEquals(status, created.getStatus());
        assertEquals(ErrorResponse.class, created.getEntity().getClass());
        assertEquals(text, responseBody.getText());
        assertEquals(devText, responseBody.getDeveloperText());
        assertEquals(id, responseBody.getId());
    }


}