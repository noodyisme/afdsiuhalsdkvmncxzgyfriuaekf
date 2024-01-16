package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.chassis.engine.dfs.emitter.core.support.AuditClientHelper;
import com.capitalone.chassis.engine.model.audit.ExceptionData;
import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.chassis.engine.model.response.ResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;

import java.util.Optional;

class ChassisStatusCodeResolverTest {

    /**
     * Method Inputs/Indirect inputs
     */
    private RequestContext requestContext;
    private ResponseData responseData;
    private Exception exception;
    private ExceptionData exceptionData;

    /**
     * Chassis Dependencies
     */
    private AuditClientHelper auditClientHelper;
    private ConversionService conversionService;

    /**
     * Object under test
     */
    private ChassisStatusCodeResolver chassisStatusCodeResolver;

    @BeforeEach
    void setup() {
        // Initialize method/indirect inputs
        requestContext = Mockito.mock(RequestContext.class);
        responseData = Mockito.mock(ResponseData.class);
        exception = Mockito.mock(Exception.class);
        exceptionData = Mockito.mock(ExceptionData.class);

        // Initialize object under test
        auditClientHelper = Mockito.mock(AuditClientHelper.class);
        conversionService = Mockito.mock(ConversionService.class);
        chassisStatusCodeResolver = new ChassisStatusCodeResolver(auditClientHelper, conversionService);

        // Default Stub for response data
        Mockito.when(requestContext.getResponseData()).thenReturn(responseData);
    }

    @Test
    void exceptionNotPresentResolveHttpStatusSuccess() {
        // Arrange
        Mockito.lenient().when(auditClientHelper.resolveHttpStatusCode(Optional.empty(), responseData))
                .thenReturn(200);

        // Act
        final Integer result = chassisStatusCodeResolver.resolveHttpStatusCode(requestContext, Optional.empty());

        // Assert
        Assertions.assertEquals(200, result);
    }

    @Test
    void exceptionPresentResolveHttpStatusSuccess() {
        // Arrange
        Mockito.when(conversionService.convert(exception, ExceptionData.class)).thenReturn(exceptionData);
        Mockito.when(auditClientHelper.resolveHttpStatusCode(Optional.of(exceptionData), responseData)).thenReturn(404);

        // Act
        final Integer result = chassisStatusCodeResolver.resolveHttpStatusCode(requestContext, Optional.of(exception));

        // Assert
        Assertions.assertEquals(404, result);
    }

    @Test
    void exceptionConverterNotFound() {
        // Arrange
        Mockito.when(conversionService.convert(exception, ExceptionData.class)).thenThrow(ConverterNotFoundException.class);

        //Act
        final Integer result = chassisStatusCodeResolver.resolveHttpStatusCode(requestContext, Optional.of(exception));

        //Assert
        Assertions.assertEquals(500, result);

    }
}