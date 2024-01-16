package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyState;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyStateCacheService;
import com.capitalone.identity.identitybuilder.policycore.service.exception.PolicyCacheException;
import com.newrelic.api.agent.Trace;
import org.apache.camel.Consume;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.apache.camel.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

@Component
@ConditionalOnBean(value = PolicyStateCacheService.class)
public class CamelCacheUtil {

    private static final Logger logger = LogManager.getLogger(CamelCacheUtil.class.getName());

    public static final String CACHE_VALUE = "cacheValue";
    public static final String CACHE_KEY = "cacheKey";
    public static final String CACHE_EXPIRATION = "cacheExpiration";
    private static final String ERROR_STRING = "Camel header \"%s\" must be set";

    private final PolicyStateCacheService cacheService;
    private final boolean logSensitiveData;

    public CamelCacheUtil(PolicyStateCacheService cacheService,
                          @Value("${identitybuilder.policycore.feature.sensitive-data-logging.enabled:false}")
                          boolean logSensitiveData) {
        this.cacheService = cacheService;
        this.logSensitiveData = logSensitiveData;
    }

    /**
     * A direct route that reads from the Policy Cache using a given key.
     * The value is returned as a <code>Map&lt;String, Object&gt;</code> in
     * the <code>cacheValue</code> header.
     * <p>
     * Input Headers:
     * <dl>
     * <dt>cacheKey</dt><dd>The key to read data for</dd>
     * </dl>
     * Output Headers:
     * <dl>
     * <dt>cacheValue</dt><dd>The value read from the cache (as a <code>Map&lt;String, Object&gt;</code>)</dd>
     * </dl>
     *
     * @param cacheKey the key to look up in the cache
     * @param headers  the message headers
     */
    @Consume("direct:cacheRead")
    @Trace
    public void cacheRead(@Header(CACHE_KEY) String cacheKey, @Headers Map<String, Object> headers) {
        if (StringUtils.isEmpty(cacheKey)) {
            throw new IllegalArgumentException(String.format(ERROR_STRING, CACHE_KEY));
        }
        PolicyState policyState = cacheService.retrieveFromCache(cacheKey);
        debugCacheMethod("cacheRead", cacheKey, policyState, "N/A");

        if (policyState == null) {
            throw PolicyCacheException.newReadMissException(cacheKey);
        } else {
            headers.put(CACHE_VALUE, policyState.getPolicyState());
        }

    }

    /**
     * A direct route that writes a {@link com.capitalone.identity.identitybuilder.policycore.model.PolicyState}
     * object to the Policy Cache using a given key.
     *
     * @param message that contains
     *                cacheKey - the key to look up in the cache
     *                cacheValue - the data to write to the cache
     *                cacheExpiration - the cache entry time-to-live, in ISO-8601 duration format
     *                policyName - the policy name
     *                policyVersion - the policy version
     *                stepName - the current step name
     *                availableNextSteps - a list of available next steps
     *                policyState - the previous policy state (null if first step)
     */
    @Trace
    public void implicitCacheWrite(Message message) {
        cacheWrite(message, true);
    }

    @Consume("direct:cacheWrite")
    @Trace
    public void explicitCacheWrite(Message message) {
        cacheWrite(message, false);
    }

    private void cacheWrite(Message message, boolean isMultiStepCacheWrite) {

        String cacheKey = message.getHeader(CACHE_KEY, String.class);
        Map<String, Serializable> cacheValue = message.getHeader(CACHE_VALUE, Map.class);
        String cacheExpiration = message.getHeader(CACHE_EXPIRATION, String.class);
        String policyName = message.getHeader(PolicyConstants.HEADER_POLICYNAME, String.class);
        String policyVersion = message.getHeader(PolicyConstants.HEADER_POLICYVERSION, String.class);
        String stepName = message.getHeader(PolicyConstants.HEADER_STEPNAME, String.class);
        Set<String> availableNextSteps = message.getHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS, Set.class);
        String effectiveNextStep = message.getHeader(PolicyConstants.HEADER_EFFECTIVENEXTSTEP, String.class);
        PolicyState policyState = message.getHeader(PolicyConstants.HEADER_POLICYSTATE, PolicyState.class);
        debugCacheMethod("cacheWrite", cacheKey, policyState, cacheExpiration);

        if (StringUtils.isEmpty(cacheKey)) {
            throw new IllegalArgumentException(String.format(ERROR_STRING, CACHE_KEY));
        } else if (cacheExpiration == null) {
            throw new IllegalArgumentException(String.format(ERROR_STRING, CACHE_EXPIRATION));
        }

        if (cacheValue == null) {
            cacheValue = new HashMap<>();
        }

        if (policyState == null && isMultiStepCacheWrite) {
            // discouraged case when writing to existing resourceId from different policy
            try { //Added as part of AUTH-140886 to resolve Redis Cache failover issue fix
                policyState = cacheService.retrieveFromCache(cacheKey);
                if(policyState != null && !policyName.equals(policyState.getPolicyName())){
                    policyState.setPolicyName(policyName);
                    policyState.setPolicyVersion(policyVersion);
                }
            } catch (ChassisSystemException cse) {
                logger.warn(String.format("An Exception occurred during cache read. Suppressed exception during cache write operation. cacheKey: %s", cacheKey), cse);
            }
        }

        if (policyState == null) {
            policyState = new PolicyState();
            policyState.setPolicyName(policyName);
            policyState.setPolicyVersion(policyVersion);
        }

        policyState.setPolicyState(cacheValue);

        if (isMultiStepCacheWrite) {
            policyState.setAvailableNextSteps(availableNextSteps == null ? new HashSet<>() : availableNextSteps);
            policyState.setEffectiveNextStep(effectiveNextStep);
            policyState.getStepsCompleted().add(stepName);
        }

        cacheService.populateCache(cacheKey, policyState, cacheExpiration);
    }

    /**
     * A direct route that deletes the entry in the cache associated with a given key.
     *
     * @param cacheKey the key to delete from the cache
     */
    @Consume("direct:cacheDelete")
    @Trace
    public void cacheDelete(@Header(CACHE_KEY) String cacheKey) {
        logger.debug("cacheMethod: cacheDelete | cacheKey: {} ", cacheKey);
        cacheService.deleteFromCache(cacheKey);
    }

    /**
     * Helper method that truncates stepsCompleted to a list of effective steps completed.
     * Useful for setting availableNextSteps based on path taken.
     * e.g. [a, b, c, a, b', c] becomes [a, b', c]
     *
     * @param stepsCompleted complete history of steps completed
     * @return truncated history of effective path taken
     *
     * <pre>{@code
     * <!-- example usage -->
     * <setHeader name="effectiveStepsCompleted">
     * 	<method ref="camelCacheUtil" method="effectiveStepsCompleted(${headers.policyState.getStepsCompleted()})"/>
     * </setHeader>
     *
     * <script>
     *   <groovy>
     *     headers.effectiveStepsCompleted.add("step3");
     *     headers.availableNextSteps = headers.effectiveStepsCompleted;
     *   </groovy>
     * </script>
     * }</pre>
     */
    public List<String> effectiveStepsCompleted(List<String> stepsCompleted) {
        // time complexity: O(n) despite nested for loop
        List<String> result = new ArrayList<>();
        for (String step : stepsCompleted) {
            if (!result.contains(step)) {
                result.add(step);
            } else {
                for (int i = result.size() - 1; !result.get(i).equals(step); i--) {
                    result.remove(i);
                }
            }
        }
        return result;
    }

    private void debugCacheMethod(final String cacheMethod,
            final String cacheKey,
            final PolicyState policyState,
            final String cacheExpiration) {
        if (logSensitiveData) {
            logger.debug("cacheMethod: {} | cacheKey: {}, cacheValue: {}, cacheExpiration: {}", cacheMethod, cacheKey, policyState, cacheExpiration);
        } else {
            logger.debug("cacheMethod: {} | cacheKey: {}, cacheExpiration: {}", cacheMethod, cacheKey, cacheExpiration);
        }
    }
}
