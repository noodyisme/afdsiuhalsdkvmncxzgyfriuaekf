package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import com.capitalone.chassis.engine.model.response.ResponseData;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.HealthCheckEvaluatedMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated;
import com.capitalone.identity.identitybuilder.policycore.service.HealthService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {
        HealthResourceTest.StartupTestConfig.class
})
@ExtendWith(MockitoExtension.class)
public class HealthResourceTest {

    @InjectMocks
    private HealthResource healthResource;
    @Mock
    private HealthService healthService;
    @Mock
    private HealthCheckEvaluatedMapper.HealthCheckPublisher healthCheckPublisher;
    @Spy
    private ResponseData responseData;
    @Spy
    private HttpServletRequest request;


    @Test
    public void healthCheckHealthNotReady() {
        when(healthService.isReady()).thenReturn(false);
        healthResource.healthCheck(null);
        verify(responseData).setStatusPreference(HttpStatus.SC_FAILED_DEPENDENCY);
        verify(healthCheckPublisher, times(1)).publishEvent(any(HealthCheckEvaluated.class));
    }

    @Test
    public void healthCheckHealthReady() {
        when(healthService.isReady()).thenReturn(true);
        healthResource.healthCheck(request);
        verify(request).setAttribute("NO_ACCESS_LOG", "true");
        verify(responseData).setStatusPreference(HttpStatus.SC_OK);
        verify(healthCheckPublisher, times(1)).publishEvent(any(HealthCheckEvaluated.class));
    }


    @Configuration
    public static class StartupTestConfig {
        @Bean
        OperationalAuditor operationalAuditor() {
            return new OperationalAuditor(null);
        }

    }
}