package com.capitalone.identity.identitybuilder.policycore.service.exception.mappers;

import com.capitalone.chassis.engine.converters.error.ChassisErrorCodeToErrorResponseConverter;
import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.identity.identitybuilder.policycore.service.util.SpringJerseyTest;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {JerseyExceptionMappingIntegrationTest.class})
public class JerseyExceptionMappingIntegrationTest extends SpringJerseyTest {

    @Mock
    ChassisErrorCodeToErrorResponseConverter errorCodeToErrorResponseConverter;


    @InjectMocks
    private JsonMappingExceptionMapper mappingExceptionMapper = new JsonMappingExceptionMapper(errorCodeToErrorResponseConverter);

    @InjectMocks
    private JsonProcessingExceptionMapper processingExceptionMapper = new JsonProcessingExceptionMapper(errorCodeToErrorResponseConverter);

    @InjectMocks
    private JsonParseExceptionMapper parseExceptionMapper = new JsonParseExceptionMapper(errorCodeToErrorResponseConverter);

    @Override
    protected ResourceConfig configure() {
        MockitoAnnotations.openMocks(this);
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        set(TestProperties.CONTAINER_PORT, "0");
        final ResourceConfig resourceConfig =
                new ResourceConfig()
                        .property("contextConfig", createSpringContext(getBeanMap()))
                        .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_SERVER, "WARNING")
                        .register(new AbstractBinder(){
                            @Override
                            protected void configure() {
                                bind(mappingExceptionMapper).to(JsonMappingExceptionMapper.class).ranked(1);
                                bind(processingExceptionMapper).to(JsonProcessingExceptionMapper.class).ranked(1);
                                bind(parseExceptionMapper).to(JsonParseExceptionMapper.class).ranked(1);
                            }
                        })
                        .registerClasses(getResourceClasses());

        return serverConfig(resourceConfig);
    }
    ObjectMapper objectMapper;
    ErrorResponse errorResponse = new ErrorResponse(INVALID_JSON_ENTITY_ID, INVALID_JSON_ENTITY_TEXT, INVALID_JSON_ENTITY_DEV_TEXT, null, null);


    /**
     * we use Mockito.lenient() here because there should only be one test case that does not use this stubbing.
     * Only {@link #serializeCar_validJson_Test() } will not use this stubbing so lenient stubbing is safe.
     */
    @BeforeEach
    public void init(){
        Mockito.lenient().when(errorCodeToErrorResponseConverter.convert(Mockito.any())).thenReturn(errorResponse);
    }



    @Override
    protected Set<Class<?>> getResourceClasses() {
        Set<Class<?>> classesToRegister = new HashSet<>();
        classesToRegister.add(JacksonFeature.class);
        classesToRegister.add(PostResource.class);
        classesToRegister.add(JsonParseExceptionMapper.class);
        classesToRegister.add(JsonMappingExceptionMapper.class);
        classesToRegister.add(JsonProcessingExceptionMapper.class);
        return classesToRegister;
    }



    @Path("/testJersey")

    public static class PostResource {

        ObjectMapper objectMapper = new ObjectMapper();
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/serializeCar")
        public Response serializeCar(@RequestBody String body) throws IOException {
            Car car = objectMapper.readValue(body, Car.class);
            return Response.status(Response.Status.OK).entity(car).build();
        }
    }

    @Data
    public static class Car{
        String make;
        String model;
        int year;
    }

    @BeforeEach
    void initTest(){

        objectMapper = new ObjectMapper();

        target().register(JacksonJaxbJsonProvider.class);
    }

    @Test
    public void serializeCar_validJson_Test() {
        String json = "{\"make\":\"BMW\", \"model\":\"X5\", \"year\":2004}";
        Response response = target("/testJersey/serializeCar").request().post(Entity.json(json));
        //objectMapper.readTree((InputStream) response.getEntity());
        System.out.println(response.getStatus());
        Car car = response.readEntity(Car.class);
        assertEquals(200, response.getStatus());
        assertEquals("BMW", car.getMake());
        assertEquals("X5", car.getModel());
        assertEquals(2004, car.getYear());
    }

    @Test
    public void serializeCar_invalidJson_AsList_Test() {
        String json = "[{\"make\":\"BMW\", \"model\":\"X5\", \"year\":2004}]";


        Response response = target("/testJersey/serializeCar").request().post(Entity.json(json));
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(INVALID_JSON_ENTITY_TEXT, errorResponse.getText());
        assertEquals(INVALID_JSON_ENTITY_ID, errorResponse.getId());
        assertEquals(INVALID_JSON_ENTITY_DEV_TEXT, errorResponse.getDeveloperText());
        assertTrue(errorResponse.getActions().isEmpty());
        assertTrue(errorResponse.getErrorDetails().isEmpty());
    }

    @Test
    public void serializeCar_invalidJson_ScriptString_Test(){
        String json = "<script>alert(1)</script>";

        Response response = target("/testJersey/serializeCar").request().post(Entity.json(json));
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(INVALID_JSON_ENTITY_TEXT, errorResponse.getText());
        assertEquals(INVALID_JSON_ENTITY_ID, errorResponse.getId());
        assertEquals(INVALID_JSON_ENTITY_DEV_TEXT, errorResponse.getDeveloperText());
        assertTrue(errorResponse.getActions().isEmpty());
        assertTrue(errorResponse.getErrorDetails().isEmpty());
    }

    @Test
    public void serializeCar_invalidJson_EmptyString_Test(){
        String json = "\"\"";

        Response response = target("/testJersey/serializeCar").request().post(Entity.json(json));
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(INVALID_JSON_ENTITY_TEXT, errorResponse.getText());
        assertEquals(INVALID_JSON_ENTITY_ID, errorResponse.getId());
        assertEquals(INVALID_JSON_ENTITY_DEV_TEXT, errorResponse.getDeveloperText());
        assertTrue(errorResponse.getActions().isEmpty());
        assertTrue(errorResponse.getErrorDetails().isEmpty());
    }

    @Test
    public void serializeCar_invalidJson_MissingEndBracket_Test() {
        String json_noClosing = "{\"make\":\"BMW\", \"model\":\"X5\", \"year\":2004";

        Response responseNoClosing = target("/testJersey/serializeCar").request().post(Entity.json(json_noClosing));
        ErrorResponse errorResponseNoClosing = responseNoClosing.readEntity(ErrorResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), responseNoClosing.getStatus());
        assertEquals(INVALID_JSON_ENTITY_TEXT, errorResponseNoClosing.getText());
        assertEquals(INVALID_JSON_ENTITY_ID, errorResponseNoClosing.getId());
        assertEquals(INVALID_JSON_ENTITY_DEV_TEXT, errorResponseNoClosing.getDeveloperText());
        assertTrue(errorResponseNoClosing.getActions().isEmpty());
        assertTrue(errorResponseNoClosing.getErrorDetails().isEmpty());
    }
    @Test
    public void serializeCar_invalidJson_MissingStartBracket_Test() {
        String json_noOpening = "\"make\":\"BMW\", \"model\":\"X5\", \"year\":2004}";

        Response responseNoOpening = target("/testJersey/serializeCar").request().post(Entity.json(json_noOpening));
        ErrorResponse errorResponseNoOpening = responseNoOpening.readEntity(ErrorResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), responseNoOpening.getStatus());
        assertEquals(INVALID_JSON_ENTITY_TEXT, errorResponseNoOpening.getText());
        assertEquals(INVALID_JSON_ENTITY_ID, errorResponseNoOpening.getId());
        assertEquals(INVALID_JSON_ENTITY_DEV_TEXT, errorResponseNoOpening.getDeveloperText());
        assertTrue(errorResponseNoOpening.getActions().isEmpty());
        assertTrue(errorResponseNoOpening.getErrorDetails().isEmpty());
    }

    @Test
    public void serializeCar_invalidJson_ExtraQuotationMark_Test(){
        String json = "{\"make\":\"\"BMW\", \"model\":\"X5\", \"year\":2004}";


        Response response = target("/testJersey/serializeCar").request().post(Entity.json(json));
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(INVALID_JSON_ENTITY_TEXT, errorResponse.getText());
        assertEquals(INVALID_JSON_ENTITY_ID, errorResponse.getId());
        assertEquals(INVALID_JSON_ENTITY_DEV_TEXT, errorResponse.getDeveloperText());
        assertTrue(errorResponse.getActions().isEmpty());
        assertTrue(errorResponse.getErrorDetails().isEmpty());
    }


}
