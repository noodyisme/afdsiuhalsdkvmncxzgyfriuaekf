package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.fileupload.onestream.CureFileuploadRawPayload;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FileUploadMapperTest {

    // Expected outputs
    private OperationalEventOutcome outcomeExpected;

    private Optional<CureFileuploadRawPayload> cureFileuploadRawPayloadOptional;

    // Object under test
    private FileUploadEventMapper fileUploadEventMapper;

    @BeforeEach
    void setup() {
        CureFileuploadRawPayload cureFileuploadRawPayload = new CureFileuploadRawPayload();
        cureFileuploadRawPayload.setFileId("testFileId");
        cureFileuploadRawPayload.setCureCorrelationId("testCorrelationId");
        cureFileuploadRawPayload.setIdentityChannelType("web");
        cureFileuploadRawPayload.setIdentityCustomerIpAddress("127.0.0.1");
        cureFileuploadRawPayload.setIdentityProducerApplicationName("testApp");
        cureFileuploadRawPayload.setProductId("testProduct");
        cureFileuploadRawPayload.setVirusScanResult("success");
        cureFileuploadRawPayload.setFileSize(1024);
        cureFileuploadRawPayload.setFileEncrypted(false);
        cureFileuploadRawPayload.setFileUploadProcessSuccess(true);
        cureFileuploadRawPayload.setHttpStatusCode(200);
        cureFileuploadRawPayloadOptional = Optional.of(cureFileuploadRawPayload);
        // Initialize default happy path expected outputs
        outcomeExpected = OperationalEventOutcome.of(OperationalStatus.SUCCESS, String.valueOf(200));

        initFileUploadEventMapper();
    }

    @Test
    void buildSupplementalAttributeListSuccess() {
        // Arrange
        final List<OperationalSupplementalAttribute> supplementalAttributeListExpected = new ArrayList<>();
        supplementalAttributeListExpected.add(new OperationalSupplementalAttribute("file_id", "testFileId"));
        supplementalAttributeListExpected.add(new OperationalSupplementalAttribute("product_id", "testProduct"));
        supplementalAttributeListExpected.add(new OperationalSupplementalAttribute("virus_scan_result", "success"));
        supplementalAttributeListExpected.add(new OperationalSupplementalAttribute("is_file_upload_process_success", "true"));
        supplementalAttributeListExpected.add(new OperationalSupplementalAttribute("is_file_encrypted", "false"));
        supplementalAttributeListExpected.add(new OperationalSupplementalAttribute("file_size", "1024"));

        // Act
        final OperationalEvent operationalEvent = fileUploadEventMapper.buildWithEntitySpecificAttributes();

        // Assert
        assertEquals(supplementalAttributeListExpected, operationalEvent.getSupplementalMetadata());
    }

    @Test
    void buildExecutionContextSuccess() {
        // Arrange
        final OperationalExecutionContext.OperationalExecutionContextBuilder executionContextBuilderExpected = OperationalExecutionContext.builder();
        OperationalExecutionContext executionContextExpected = executionContextBuilderExpected.channelType("web").build();

        // Act
        final OperationalEvent operationalEvent = fileUploadEventMapper.buildWithEntitySpecificAttributes();

        // Assert
        assertEquals(executionContextExpected, operationalEvent.getExecutionContext());
    }

    @ParameterizedTest
    @ValueSource(ints = {200})
    void operationalOutcomeSuccess(final int statusCode) {
        outcomeExpected = OperationalEventOutcome.of(OperationalStatus.SUCCESS, String.valueOf(statusCode));
        cureFileuploadRawPayloadOptional.get().setHttpStatusCode(statusCode);
        initFileUploadEventMapper();

        // Act
        final OperationalEventOutcome outcomeActual = fileUploadEventMapper.toOperationalEventOutcome();

        // Assert
        assertEquals(outcomeExpected, outcomeActual);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 409})
    void operationalOutcomeInvalid(final int statusCode) {
        outcomeExpected = OperationalEventOutcome.of(OperationalStatus.INVALID, String.valueOf(statusCode));
        cureFileuploadRawPayloadOptional.get().setHttpStatusCode(statusCode);
        initFileUploadEventMapper();

        // Act
        final OperationalEventOutcome outcomeActual = fileUploadEventMapper.toOperationalEventOutcome();

        // Assert
        assertEquals(outcomeExpected, outcomeActual);
    }

    @ParameterizedTest
    @ValueSource(ints = {500})
    void operationalOutcomeFailure(final int statusCode) {
        outcomeExpected = OperationalEventOutcome.of(OperationalStatus.FAILURE, String.valueOf(statusCode));
        cureFileuploadRawPayloadOptional.get().setHttpStatusCode(statusCode);
        initFileUploadEventMapper();

        // Act
        final OperationalEventOutcome outcomeActual = fileUploadEventMapper.toOperationalEventOutcome();

        // Assert
        assertEquals(outcomeExpected, outcomeActual);
    }

    @Test
    void operationalErrorWithHttpSuccess() {
        // Act
        final Optional<OperationalError> operationalErrorActual = fileUploadEventMapper.toOperationalError();
        final Optional<OperationalError> operationalErrorExpected = Optional.empty();

        // Assert
        assertEquals(operationalErrorExpected, operationalErrorActual);
    }

    @Test
    void operationalErrorWithHttpError() {
        // Arrange
        cureFileuploadRawPayloadOptional.get().setHttpStatusCode(400);
        cureFileuploadRawPayloadOptional.get().setFileUploadProcessSuccess(false);
        cureFileuploadRawPayloadOptional.get().setErrorDeveloperText("Key not found");
        cureFileuploadRawPayloadOptional.get().setErrorCode(200002);
        initFileUploadEventMapper();
        final OperationalError operationalErrorExpected = OperationalError.builder()
                .text("Unable to process File Upload request")
                .developerText("Key not found")
                .errorId("200002")
                .build();

        // Act
        final Optional<OperationalError> operationalErrorActual = fileUploadEventMapper.toOperationalError();

        // Assert
        assertEquals(Optional.of(operationalErrorExpected), operationalErrorActual);
    }

    @Test
    void createFromFactorySuccess() {
        // Arrange
        FileUploadEventMapper.Factory factory = new FileUploadEventMapper.Factory();

        // Act
        final OperationalEventMapper operationalEventMapper = factory.create(cureFileuploadRawPayloadOptional.get(), null);

        // Assert
        assertInstanceOf(FileUploadEventMapper.class, operationalEventMapper);
    }

    private void initFileUploadEventMapper() {
        fileUploadEventMapper = new FileUploadEventMapper(cureFileuploadRawPayloadOptional);
    }

}