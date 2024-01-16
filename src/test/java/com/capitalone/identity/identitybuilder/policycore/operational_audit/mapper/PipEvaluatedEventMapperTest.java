package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalStatus;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Optional;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.AuditTestConstants.DX_POLICY_INFO;
import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.AuditTestConstants.DX_SCHEME;
import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.AuditTestConstants.DX_SERVICE_NAME;
import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.AuditTestConstants.DX_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class PipEvaluatedEventMapperTest {
    private ObjectMapper objectMapper;
    private Optional<DevExchangeRequest> devExchangeRequestOptional;
    private Optional<DevExchangeResponse> devExchangeResponseOptional;
    private Optional<HttpStatus> httpStatusOptional;

    private PipEvaluatedEventMapper pipEvaluatedEventMapper;

    @BeforeEach
    void setUp(){
      objectMapper = Mockito.mock(ObjectMapper.class);
      devExchangeRequestOptional = Optional.of(Mockito.mock(DevExchangeRequest.class));
      devExchangeResponseOptional = Optional.of(Mockito.mock(DevExchangeResponse.class));
      httpStatusOptional = Optional.of(Mockito.mock(HttpStatus.class));

      Mockito.when(devExchangeResponseOptional.get().getDxResponseAudit()).thenReturn("test_json");
    }

    @Test
    void testBuildWithEntitySpecificAttributes() {
      pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

      // Mock behavior
      Mockito.when(devExchangeRequestOptional.get().getServiceName()).thenReturn(DX_SERVICE_NAME);
      Mockito.when(devExchangeRequestOptional.get().getPolicyInfo()).thenReturn(DX_POLICY_INFO);
      Mockito.when(devExchangeRequestOptional.get().getHttpMethod()).thenReturn(HttpMethod.POST);
      Mockito.when(devExchangeRequestOptional.get().getUri()).thenReturn(Mockito.mock(URI.class));
      Mockito.when(devExchangeRequestOptional.get().getUri().toString()).thenReturn(DX_URI);
      Mockito.when(devExchangeRequestOptional.get().getScheme()).thenReturn(DX_SCHEME);

      // Call method
      OperationalEvent result = pipEvaluatedEventMapper.buildWithEntitySpecificAttributes();

      // Assertions
      assertEquals(DX_SERVICE_NAME, result.getEventEntityName());
      assertEquals(DX_POLICY_INFO, result.getPipMetadata().getPolicyInfo());
      assertEquals(HttpMethod.POST.toString(), result.getPipMetadata().getHttpMethod());
      assertEquals(DX_URI, result.getPipMetadata().getEndpointTargetUri());
      assertEquals(DX_SCHEME, result.getPipMetadata().getEndpointScheme());
    }

    @Test
    void testToOperationalEventOutcomeNoHttpStatus() {
      // Arrange
      httpStatusOptional = Optional.empty();
      pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

      // Act
      OperationalEventOutcome result = pipEvaluatedEventMapper.toOperationalEventOutcome();

      // Assert
      assertEquals(OperationalStatus.FAILURE.name(), result.getResult());
      assertEquals(OperationalAuditConstants.UNDEFINED, result.getSeverity());
    }

    @Test
    void testToOperationalEventOutcome2xxHttpStatus() {
      // Arrange
      httpStatusOptional = Optional.of(HttpStatus.OK);
      pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

      // Act
      OperationalEventOutcome result = pipEvaluatedEventMapper.toOperationalEventOutcome();

      // Assert
      assertEquals(OperationalStatus.SUCCESS.name(), result.getResult());
      assertEquals("200", result.getSeverity());
    }

  @Test
  void testToOperationalEventOutcome4xxHttpStatus() {
    // Arrange
    httpStatusOptional = Optional.of(HttpStatus.BAD_REQUEST);
    pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

    // Act
    OperationalEventOutcome result = pipEvaluatedEventMapper.toOperationalEventOutcome();

    // Assert
    assertEquals(OperationalStatus.INVALID.name(), result.getResult());
    assertEquals("400", result.getSeverity());
  }

  @Test
  void testToOperationalEventOutcomeOtherHttpStatus() {
    // Arrange
    httpStatusOptional = Optional.of(HttpStatus.INTERNAL_SERVER_ERROR);
    pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

    // Act
    OperationalEventOutcome result = pipEvaluatedEventMapper.toOperationalEventOutcome();

    // Assert
    assertEquals(OperationalStatus.FAILURE.name(), result.getResult());
    assertEquals("500", result.getSeverity());
  }

  @Test
  void testToOperationalErrorHttpStatusNotPresent() {
    // Arrange
    httpStatusOptional = Optional.empty();
    pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

    // Act
    Optional<OperationalError> result = pipEvaluatedEventMapper.toOperationalError();

    // Assert
    assertEquals(Optional.empty(), result);
  }

  @Test
  void testToOperationalErrorDevExchangeResponseNotPresent() {
    // Arrange
    devExchangeResponseOptional = Optional.empty();
    pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

    // Act
    Optional<OperationalError> result = pipEvaluatedEventMapper.toOperationalError();

    // Assert
    assertEquals(Optional.empty(), result);
  }

  @Test
  void testToOperationalErrorSuccessfulHttpStatus() {
    // Arrange
    httpStatusOptional = Optional.of(HttpStatus.OK);
    pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

    // Act
    Optional<OperationalError> result = pipEvaluatedEventMapper.toOperationalError();

    // Assert
    assertEquals(Optional.empty(), result);
  }

  @Test
  @SneakyThrows
  void testToOperationalErrorInvalidJson() {
    // Arrange
    Mockito.when(objectMapper.readValue(anyString(), any(Class.class))).thenThrow(JsonProcessingException.class);
    pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

    // Act
    Optional<OperationalError> result = pipEvaluatedEventMapper.toOperationalError();

    // Assert
    assertTrue(result.isPresent());
    assertEquals("test_json", result.get().getDeveloperText());
  }

  @Test
  @SneakyThrows
  void testToOperationalErrorValidDownstreamErrorInfo() {
    // Arrange
    PipEvaluatedEventMapper.DownstreamErrorInfo downstreamErrorInfo = new PipEvaluatedEventMapper.DownstreamErrorInfo("test_error_id", "test_error_text");
    Mockito.when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(downstreamErrorInfo);
    pipEvaluatedEventMapper = new PipEvaluatedEventMapper(objectMapper, devExchangeRequestOptional, devExchangeResponseOptional, httpStatusOptional);

    // Act
    Optional<OperationalError> result = pipEvaluatedEventMapper.toOperationalError();

    // Assert
    assertTrue(result.isPresent());
    assertEquals("test_error_id", result.get().getErrorId());
    assertEquals("test_error_text", result.get().getText());
  }

  @Test
  void testFactoryCreateWithNonNullValues() {
    // Arrange
    Mockito.when(devExchangeResponseOptional.get().getHttpStatus()).thenReturn(200);
    PipEvaluatedEventMapper.Factory factory = new PipEvaluatedEventMapper.Factory();

    // Act
    PipEvaluatedEventMapper result = factory.create(devExchangeRequestOptional.get(), devExchangeResponseOptional.get());

    // Assert
    assertNotNull(result);
  }


}
