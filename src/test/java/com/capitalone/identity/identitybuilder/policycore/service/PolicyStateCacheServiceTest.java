package com.capitalone.identity.identitybuilder.policycore.service;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.dsd.elasticache.client.cluster.ElasticCacheService;
import com.capitalone.dsd.elasticache.client.constants.ClientConstants;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyState;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.util.ApplicationUtil;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PolicyStateCacheServiceTest {

    Map<String, Serializable> policyStateData = new HashMap<>();

    private String cacheValue;

    private String resourceId;

    private PolicyState policyState;

    private final String cacheExpiration = "PT15M";

    @Spy
    private final ApplicationUtil applicationUtil = new ApplicationUtil();

    @Mock
    private ElasticCacheService elasticCacheService;

    @InjectMocks
    PolicyStateCacheService policyStateCacheService;

    @BeforeEach
    public void setup() {
        resourceId = "ec91bfc5-e1a3-46a0-8bea-e3ad8d2944db";
        policyStateData.put("firstName", "John");
        policyState = new PolicyState();
        policyState.setPolicyState(policyStateData);
        policyState.setPolicyName("success");
        policyState.setPolicyVersion("1.0");
        policyState.getStepsCompleted().add("start");
        policyState.getAvailableNextSteps().add("step2");
        policyState.getAvailableNextSteps().add("step2a");

        cacheValue = applicationUtil.convertObjectToString(policyState);
    }

    @Test
    public void testPopulateCacheSuccess() {
        when(elasticCacheService.putWithExpiry(eq(resourceId), eq(cacheValue), anyLong())).thenReturn(HttpStatus.SC_OK);
        policyStateCacheService.populateCache(resourceId, policyState, cacheExpiration);
        assertNotNull(policyStateCacheService);
    }

    @Test
    public void testPopulateCacheFailure() {
        when(elasticCacheService.putWithExpiry(eq(resourceId), eq(cacheValue), anyLong())).thenReturn(HttpStatus.SC_BAD_REQUEST);
        ChassisSystemException policyCacheException = assertThrows(ChassisSystemException.class, () -> policyStateCacheService.populateCache(resourceId, policyState, cacheExpiration));
        assertEquals(ApplicationConstants.CACHE_WRITE_FAILED_ID, policyCacheException.getApiError().getId());
        assertEquals(ApplicationConstants.CACHE_OPERATION_FAILED_DEV_TEXT_ID, policyCacheException.getApiError().getDeveloperTextId());
    }

    @Test
    public void testPopulateCacheNegativeExpiration() {
        String cacheExpiration = "-PT10M";
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> policyStateCacheService.populateCache(resourceId, policyState, cacheExpiration));
        assertEquals(String.format(ApplicationConstants.NEGATIVE_CACHE_EXPIRATION, cacheExpiration), illegalArgumentException.getMessage());
    }

    @Test
    void stepExpirationPrecisionValid() {
        final String actual = policyStateCacheService.getStepExpiration(resourceId).toString();
        Assertions.assertTrue(actual.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})"));
    }

    @Test
    public void testReadCacheSuccess() {
        when(elasticCacheService.get(resourceId)).thenReturn(cacheValue);
        assertEquals(policyState, policyStateCacheService.retrieveFromCache(resourceId));
    }

    @Test
    public void testReadCacheNotFound() {
        when(elasticCacheService.get(resourceId)).thenReturn("KEY_NOT_FOUND");
        assertNull(policyStateCacheService.retrieveFromCache(resourceId));
    }

    @Test
    public void testReadCacheTokenExpired() {
        when(elasticCacheService.get(resourceId)).thenReturn("TOKEN_EXPIRED");
        assertNull(policyStateCacheService.retrieveFromCache(resourceId));
    }

    private static Stream<Arguments> provideElasticCacheResponse() {
        return Stream.of(
                Arguments.of(ClientConstants.TIMEOUT_ERROR),
                Arguments.of(ClientConstants.CONNECTION_ERROR)
        );
    }

    @ParameterizedTest
    @MethodSource("provideElasticCacheResponse")
    public void testReadCacheFailure(String eCacheResponse) {
        when(elasticCacheService.get(resourceId)).thenReturn(eCacheResponse);
        ChassisSystemException exception = assertThrows(ChassisSystemException.class, () -> policyStateCacheService.retrieveFromCache(resourceId));
        assertEquals(ApplicationConstants.CACHE_OPERATION_FAILED_DEV_TEXT_ID, exception.getApiError().getDeveloperTextId());
    }

    @Test
    public void testGetStepExpiration() {
        when(elasticCacheService.getKeyTTL(resourceId)).thenReturn(300L);
        LocalDateTime expectedTTL = LocalDateTime.now().plusSeconds(300);
        LocalDateTime actualTTL = policyStateCacheService.getStepExpiration(resourceId);

        // allow 1 second time-delta
        assertTrue(Duration.between(expectedTTL, actualTTL).getSeconds() < 1);
    }

    @Test
    public void testGetStepExpirationNoTTL() {
        when(elasticCacheService.getKeyTTL(resourceId)).thenReturn(-1L);
        assertEquals(LocalDateTime.MAX, policyStateCacheService.getStepExpiration(resourceId));
    }

    @Test
    public void testGetStepExpirationMissingKey() {
        when(elasticCacheService.getKeyTTL(resourceId)).thenReturn(-2L);
        assertNull(policyStateCacheService.getStepExpiration(resourceId));
    }

    @Test
    public void testDeleteCache() {
        when(elasticCacheService.delete(resourceId)).thenReturn(HttpStatus.SC_OK);
        assertDoesNotThrow(() -> policyStateCacheService.deleteFromCache(resourceId));
    }

    @Test
    public void testDeleteCacheFailure() {
        when(elasticCacheService.delete(resourceId)).thenReturn(HttpStatus.SC_BAD_REQUEST);
        assertDoesNotThrow(() -> policyStateCacheService.deleteFromCache(resourceId));
    }
}
