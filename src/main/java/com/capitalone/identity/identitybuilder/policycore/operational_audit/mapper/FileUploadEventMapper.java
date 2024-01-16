package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.fileupload.onestream.CureFileuploadRawPayload;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalExecutionContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Log4j2
public class FileUploadEventMapper implements OperationalEventMapper {
    private static final String UNABLE_TO_PROCESS_FILE_UPLOAD_REQUEST = "Unable to process File Upload request";

    private final @NonNull Optional<CureFileuploadRawPayload> cureFileuploadRawPayloadOptional;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {

        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final OperationalExecutionContext.OperationalExecutionContextBuilder executionContextBuilder = OperationalExecutionContext.builder();
        final List<OperationalSupplementalAttribute> supplementalAttributeList = new ArrayList<>();
        cureFileuploadRawPayloadOptional.ifPresent(cureFileuploadRawPayload -> {
            Optional.ofNullable(cureFileuploadRawPayload.getIdentityCustomerIpAddress()).ifPresent(operationalEventBuilder::clientIpAddress);
            Optional.ofNullable(cureFileuploadRawPayload.getIdentityChannelType()).ifPresent(executionContextBuilder::channelType);
            Optional.ofNullable(cureFileuploadRawPayload.getFileId()).ifPresent(fileId -> supplementalAttributeList.add(new OperationalSupplementalAttribute("file_id",
                   fileId)));
            Optional.ofNullable(cureFileuploadRawPayload.getProductId()).ifPresent(productId -> supplementalAttributeList.add(new OperationalSupplementalAttribute("product_id",
                    productId)));
            Optional.ofNullable(cureFileuploadRawPayload.getVirusScanResult()).ifPresent(scanResult -> supplementalAttributeList.add(
                    new OperationalSupplementalAttribute("virus_scan_result", cureFileuploadRawPayload.getVirusScanResult())));
            Optional.ofNullable(cureFileuploadRawPayload.isFileUploadProcessSuccess()).ifPresent(isSuccess -> supplementalAttributeList.add(
                    new OperationalSupplementalAttribute("is_file_upload_process_success", String.valueOf(isSuccess))));
            Optional.ofNullable(cureFileuploadRawPayload.isFileEncrypted()).ifPresent(isEncrypted -> supplementalAttributeList.add(
                    new OperationalSupplementalAttribute("is_file_encrypted", String.valueOf(isEncrypted))));
            Optional.ofNullable(cureFileuploadRawPayload.getFileSize()).ifPresent(text -> supplementalAttributeList.add(
                    new OperationalSupplementalAttribute("file_size", String.valueOf(cureFileuploadRawPayload.getFileSize()))));
        });

        return operationalEventBuilder
                .executionContext(executionContextBuilder.build())
                .supplementalMetadata(supplementalAttributeList)
                .build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if (!cureFileuploadRawPayloadOptional.isPresent() || !Optional.ofNullable(cureFileuploadRawPayloadOptional.get().getHttpStatusCode()).isPresent()) {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, OperationalAuditConstants.UNDEFINED);
        }
        final int httpStatus = cureFileuploadRawPayloadOptional.get().getHttpStatusCode();
        final String httpStatusCodeString = String.valueOf(httpStatus);
        if (httpStatus >= 200 && httpStatus < 300) {
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, httpStatusCodeString);
        } else if (httpStatus >= 400 && httpStatus < 500) {
            return OperationalEventOutcome.of(OperationalStatus.INVALID, httpStatusCodeString);
        } else {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, httpStatusCodeString);
        }
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        if (!cureFileuploadRawPayloadOptional.isPresent() || !Optional.ofNullable(cureFileuploadRawPayloadOptional.get().getHttpStatusCode()).isPresent()) {
            return Optional.empty();
        }
        final int httpStatus = cureFileuploadRawPayloadOptional.get().getHttpStatusCode();
        if (httpStatus >= 200 && httpStatus < 300) {
            return Optional.empty();
        }
        final CureFileuploadRawPayload cureFileuploadRawPayload = cureFileuploadRawPayloadOptional.get();
        return Optional.of(OperationalError.builder()
                .errorId(String.valueOf(cureFileuploadRawPayload.getErrorCode()))
                .text(UNABLE_TO_PROCESS_FILE_UPLOAD_REQUEST)
                .developerText(cureFileuploadRawPayload.getErrorDeveloperText())
                .build());
    }

    public static class Factory implements AspectEventFactory {

        @NonNull
        @Override
        public FileUploadEventMapper create(final Object request, final Object response) {
            return new FileUploadEventMapper(
                    Optional.ofNullable((CureFileuploadRawPayload) request));
        }
    }

}
