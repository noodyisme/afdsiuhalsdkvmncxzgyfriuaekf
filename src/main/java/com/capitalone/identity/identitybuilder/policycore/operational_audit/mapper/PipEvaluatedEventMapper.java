package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.pip.OperationalPipMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalExecutionContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil.appendNonCuREHeaders;

@AllArgsConstructor
@Log4j2
public class PipEvaluatedEventMapper implements OperationalEventMapper {

    private final @NonNull ObjectMapper objectMapper;
    private final @NonNull Optional<DevExchangeRequest> devExchangeRequestOptional;
    private final @NonNull Optional<DevExchangeResponse> devExchangeResponseOptional;
    private final @NonNull Optional<HttpStatus> httpStatusOptional;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {

        // Initialize builders
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final OperationalPipMetadata.OperationalPipMetadataBuilder pipMetadataBuilder =
                OperationalPipMetadata.builder();
        final List<OperationalSupplementalAttribute> supplementalAttributes = new ArrayList<>();
        final OperationalExecutionContext.OperationalExecutionContextBuilder executionContextBuilder =
                OperationalExecutionContext.builder();

        // Audit request-driven metadata
        devExchangeRequestOptional.ifPresent(devExchangeRequest -> {
            operationalEventBuilder.eventEntityName(devExchangeRequest.getServiceName());
            pipMetadataBuilder.policyInfo(devExchangeRequest.getPolicyInfo());
            pipMetadataBuilder.httpMethod(devExchangeRequest.getHttpMethod().toString());
            pipMetadataBuilder.endpointServiceName(devExchangeRequest.getServiceName());
            pipMetadataBuilder.endpointTargetUri(devExchangeRequest.getUri().toString());
            pipMetadataBuilder.endpointScheme(devExchangeRequest.getScheme());

            // Include any non-CuRE headers in supplemental metadata and/or execution context
            Optional.ofNullable(devExchangeRequest.getHttpHeaders()).ifPresent(
                    httpHeaders -> appendNonCuREHeaders(httpHeaders, executionContextBuilder));
        });

        // Audit response-driven metadata
        devExchangeResponseOptional.ifPresent(devExchangeResponse -> {
                operationalEventBuilder.protocolResponseDetail(devExchangeResponse.getDxResponseAudit());
                pipMetadataBuilder.responseHttpStatus(Integer.toString(devExchangeResponse.getHttpStatus()));
        });

        return operationalEventBuilder.pipMetadata(pipMetadataBuilder.build())
                .executionContext(executionContextBuilder.build())
                .supplementalMetadata(supplementalAttributes)
                .build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if (!httpStatusOptional.isPresent()) {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, OperationalAuditConstants.UNDEFINED);
        }
        final HttpStatus httpStatus = httpStatusOptional.get();
        final String httpStatusCodeString = String.valueOf(httpStatus.value());
        if (httpStatus.is2xxSuccessful()) {
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, httpStatusCodeString);
        } else if (httpStatus.is4xxClientError()) {
            return OperationalEventOutcome.of(OperationalStatus.INVALID, httpStatusCodeString);
        } else {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, httpStatusCodeString);
        }
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        if (!httpStatusOptional.isPresent() || !devExchangeResponseOptional.isPresent()) {
            return Optional.empty();
        }
        final HttpStatus httpStatus = httpStatusOptional.get();
        if (httpStatus.is2xxSuccessful()) {
            return Optional.empty();
        }
        final DevExchangeResponse devExchangeResponse = devExchangeResponseOptional.get();
        final String responseAuditString = devExchangeResponse.getDxResponseAudit();
        final DownstreamErrorInfo downstreamErrorInfo;
        try {
            downstreamErrorInfo = objectMapper.readValue(responseAuditString, DownstreamErrorInfo.class);
        } catch (final Exception e) {
            log.info("DevExchangeResponse dxResponse unexpected error format during operational audit: {}",
                    responseAuditString);
            return Optional.of(OperationalError.builder()
                    .developerText(responseAuditString)
                    .build());
        }
        return Optional.of(OperationalError.builder()
                .errorId(downstreamErrorInfo.getId())
                .text(downstreamErrorInfo.getText())
                .developerText(responseAuditString)
                .build());
    }




    @Getter
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DownstreamErrorInfo {
        String id;
        String text;
    }



    public static class Factory implements OperationalEventMapper.AspectEventFactory {

        private static final ObjectMapper objectMapper = new ObjectMapper();

        @NonNull
        @Override
        public PipEvaluatedEventMapper create(final Object request, final Object response) {
            final Optional<DevExchangeResponse> responseOptional = Optional.ofNullable((DevExchangeResponse) response);
            return new PipEvaluatedEventMapper(
                    objectMapper,
                    Optional.ofNullable((DevExchangeRequest) request),
                    responseOptional,
                    responseOptional.map(DevExchangeResponse::getHttpStatus).map(HttpStatus::valueOf));
        }
    }

}
