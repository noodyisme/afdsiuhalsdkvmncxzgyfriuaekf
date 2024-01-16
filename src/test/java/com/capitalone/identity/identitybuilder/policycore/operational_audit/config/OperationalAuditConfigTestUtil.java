package com.capitalone.identity.identitybuilder.policycore.operational_audit.config;

import com.capitalone.chassis.engine.dfs.emitter.core.support.AuditClientHelper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalStatus;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import java.util.Optional;

public class OperationalAuditConfigTestUtil {

    public static OperationalEventEnricher.CommonAttributes buildDefaultCommonEnrichmentInput() {
        return OperationalEventEnricher.CommonAttributes.builder()
                .eventType(OperationalEventType.POLICY_EVALUATED)
                .eventOutcome(OperationalEventOutcome.of(OperationalStatus.SUCCESS, "INFO"))
                .errorOptional(Optional.empty())
                .requestContextOptional(Optional.empty())
                .eventOperationId("1234")
                .startTimestamp(System.currentTimeMillis())
                .endTimestamp(System.currentTimeMillis() + 1000)
                .build();
    }

    public static OperationalEventEnricher.CommonAttributes buildDefaultCommonEnrichmentInputWithError() {
        return buildDefaultCommonEnrichmentInput().toBuilder()
                .errorOptional(Optional.of(OperationalError.builder()
                        .errorId("123")
                        .developerText("Hello world!")
                        .stackTrace("This is a stack trace.")
                        .text("This is text.")
                        .build()))
                .build();
    }

    @Configuration
    public static class MockedChassisConfig {
        @MockBean(name = "auditClientHelper")
        AuditClientHelper auditClientHelper;

        @MockBean(name = "conversionService")
        ConversionService conversionService;

        @Bean
        RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder();
        }
    }


}
