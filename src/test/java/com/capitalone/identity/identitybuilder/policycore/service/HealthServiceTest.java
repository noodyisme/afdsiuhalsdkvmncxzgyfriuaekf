package com.capitalone.identity.identitybuilder.policycore.service;

import com.capitalone.identity.identitybuilder.policycore.camel.CamelLoadCompleteMonitor;
import com.capitalone.identity.identitybuilder.policycore.service.jws.DynamicJWTKeyPairManager;
import com.capitalone.identity.identitybuilder.policycore.externalization.DynamicEntityManager;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicPolicyManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthServiceTest {

    @Mock
    private CamelLoadCompleteMonitor camelLoadCompleteMonitor;

    @Mock
    private DynamicPolicyManager externalPolicyManager;

    @Mock
    private DynamicEntityManager externalEntityManager;

    @Mock
    private DynamicJWTKeyPairManager dynamicJWTKeyPairManager;

    @InjectMocks
    private HealthService healthService;

    @Test
    public void testReadyWhenLoaded() {
        when(camelLoadCompleteMonitor.isDoneLoading()).thenReturn(true);
        when(externalPolicyManager.isExternalLoadComplete()).thenReturn(true);
        when(externalEntityManager.isAccessControlLoadComplete()).thenReturn(true);
        when(dynamicJWTKeyPairManager.isKeyPairReady()).thenReturn(true);
        Assertions.assertThat(healthService.isReady()).isTrue();
    }

    @Test
    public void testReadyWhenLoaded_noEntityManager() {
        when(camelLoadCompleteMonitor.isDoneLoading()).thenReturn(true);
        when(externalPolicyManager.isExternalLoadComplete()).thenReturn(true);
        when(dynamicJWTKeyPairManager.isKeyPairReady()).thenReturn(true);
        HealthService service = new HealthService(camelLoadCompleteMonitor, externalPolicyManager, null, dynamicJWTKeyPairManager);
        Assertions.assertThat(service.isReady()).isTrue();
    }

    @Test
    public void testNotReadyWhenCamelNotLoaded() {
        when(camelLoadCompleteMonitor.isDoneLoading()).thenReturn(false);
        Assertions.assertThat(healthService.isReady()).isFalse();
    }

    @Test
    public void testNotReadyWhenExternalPolicyManagerNotLoaded() {
        when(camelLoadCompleteMonitor.isDoneLoading()).thenReturn(true);
        when(externalPolicyManager.isExternalLoadComplete()).thenReturn(false);
        Assertions.assertThat(healthService.isReady()).isFalse();
    }

    @Test
    public void testNotReadyWhenExternalEntityManagerNotLoaded() {
        when(camelLoadCompleteMonitor.isDoneLoading()).thenReturn(true);
        when(externalPolicyManager.isExternalLoadComplete()).thenReturn(true);
        when(externalEntityManager.isAccessControlLoadComplete()).thenReturn(false);
        Assertions.assertThat(healthService.isReady()).isFalse();
    }

    //Tests that once the Atomic Boolean is set to true, healthServiceIsReady.compareAndSet() is no longer called
    @Test
    public void testSwitchFromNotReadyToReady() {
        when(camelLoadCompleteMonitor.isDoneLoading()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(externalPolicyManager.isExternalLoadComplete()).thenReturn(false).thenReturn(true);
        when(externalEntityManager.isAccessControlLoadComplete()).thenReturn(true).thenReturn(true);
        when(dynamicJWTKeyPairManager.isKeyPairReady()).thenReturn(true).thenReturn(true);
        Assertions.assertThat(healthService.isReady()).isFalse();
        Assertions.assertThat(healthService.isReady()).isTrue();
        Assertions.assertThat(healthService.isReady()).isTrue();
        assertEquals(2, Mockito.mockingDetails(camelLoadCompleteMonitor).getInvocations().size());
    }
}
