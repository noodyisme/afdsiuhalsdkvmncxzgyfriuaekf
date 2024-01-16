package com.capitalone.identity.identitybuilder.policycore.service;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.dsd.elasticache.client.cluster.ElasticCacheService;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyState;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.exception.PolicyCacheException;
import com.capitalone.identity.identitybuilder.policycore.service.util.ApplicationUtil;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Named;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
@Log
@Component
@ConditionalOnProperty(ApplicationConstants.ENABLE_CACHE_PROPERTY)
public class PolicyStateCacheService {

    private final ElasticCacheService elasticCacheService;

    private final ApplicationUtil applicationUtil;

    private static final Logger logger = LogManager.getLogger(PolicyStateCacheService.class.getName());

    private static final List<String> READ_CACHE_INTERNAL_FAILURE_CAUSES = Stream.of("CONNECTION_ERROR", "TIMEOUT_ERROR").collect(Collectors.toList());
    private static final List<String> READ_CACHE_MISS_FAILURE_CAUSES = Stream.of("KEY_NOT_FOUND", "TOKEN_EXPIRED").collect(Collectors.toList());

    public PolicyStateCacheService(ElasticCacheService elasticCacheService, ApplicationUtil applicationUtil) {
        this.elasticCacheService = elasticCacheService;
        this.applicationUtil = applicationUtil;
    }

    public void populateCache(String resourceId, PolicyState policyState, String cacheExpiration) {
        String value = applicationUtil.convertObjectToString(policyState);
        int status;
        if (cacheExpiration == null) {
            status = elasticCacheService.put(resourceId, value);
        } else {
            long durationMs = Duration.parse(cacheExpiration).toMillis();
            if (durationMs <= 0) {
                throw new IllegalArgumentException(String.format(ApplicationConstants.NEGATIVE_CACHE_EXPIRATION, cacheExpiration));
            }
            status = elasticCacheService.putWithExpiry(resourceId, value, durationMs);
        }
        if (status != HttpStatus.SC_OK) {
            logger.error("Cache Write failed for resource id {} with status {}. CCID: {}", resourceId, status,
                    RequestContextHolder.getRequestContextOrDefault().getCorrelationId());
            throw PolicyCacheException.newWriteFailedException(resourceId);
        }
    }

    public PolicyState retrieveFromCache(String resourceId) {
        String dataStr = elasticCacheService.get(resourceId);
        PolicyState policyState = null;

        if (READ_CACHE_MISS_FAILURE_CAUSES.stream().anyMatch(dataStr.trim()::equalsIgnoreCase)) {
            logger.info("Cache Miss for resource id {} with status {}", resourceId, dataStr);
            // This will be handled at Policy Service layer as expired resource
        } else if (READ_CACHE_INTERNAL_FAILURE_CAUSES.stream().anyMatch(dataStr.trim()::equalsIgnoreCase)) {
            logger.error("Cache Read failed for resource id {} response from eCache was {}. CCID: {}", resourceId, dataStr,
                    RequestContextHolder.getRequestContextOrDefault().getCorrelationId());
            throw PolicyCacheException.newReadFailedException(resourceId);
        } else {
            policyState = applicationUtil.convertStringToPolicyState(dataStr);
            logger.debug("Cache details {} for resource id {}", policyState, resourceId);
        }
        return policyState;
    }

    public LocalDateTime getStepExpiration(String resourceId) {
        long ttl = elasticCacheService.getKeyTTL(resourceId);
        // getKeyTTL returns -1 if the key exists but has no associated expire
        if (ttl == -1) {
            return LocalDateTime.MAX;
        }
        // getKeyTTL returns -2 if the key does not exist
        if (ttl == -2) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        return now.plusSeconds(ttl);
    }

    public void deleteFromCache(String resourceId) {
        int deleteCountStatus = elasticCacheService.delete(resourceId);
        if (deleteCountStatus != HttpStatus.SC_OK) {
            logger.info("Cache delete failed for resource id : {}", resourceId);
        }
    }

}
