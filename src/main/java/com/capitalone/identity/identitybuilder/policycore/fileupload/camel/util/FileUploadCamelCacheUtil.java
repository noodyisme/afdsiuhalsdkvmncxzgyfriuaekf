package com.capitalone.identity.identitybuilder.policycore.fileupload.camel.util;

import com.capitalone.dsd.elasticache.client.cluster.ElasticCacheService;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadService;
import com.capitalone.identity.identitybuilder.policycore.service.exception.PolicyCacheException;
import com.newrelic.api.agent.Trace;
import org.apache.camel.Consume;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(value = FileUploadService.FILE_UPLOAD_ENABLED_FLAG)
public class FileUploadCamelCacheUtil {

	private static final Logger logger = LogManager.getLogger(FileUploadCamelCacheUtil.class.getName());

	private static final String CACHE_EXPIRE = "PT15M";

	@Inject
	private ElasticCacheService cacheService;

	public static final String CACHE_VALUE = "cacheFileValue";
	public static final String CACHE_KEY = "cacheFileKey";
	public static final String CACHE_EXPIRATION = "cacheExpiration";
	private static final String ERROR_STRING = "Cache Key must be set";

	private static final List<String> READ_CACHE_FAILURE_CAUSES = Stream.of("CONNECTION_ERROR", "TIMEOUT_ERROR")
			.collect(Collectors.toList());
	private static final List<String> READ_CACHE_FAILURE_EXPIRED = Stream.of("KEY_NOT_FOUND", "TOKEN_EXPIRED")
			.collect(Collectors.toList());

	/**
	 * A direct route that reads from the Policy Cache using a given key. The value
	 * is returned as a <code>Map&lt;String, Object&gt;</code> in the
	 * <code>cacheValue</code> header.
	 * <p>
	 * Input Headers:
	 * <dl>
	 * <dt>cacheKey</dt>
	 * <dd>The key to read data for</dd>
	 * </dl>
	 * Output Headers:
	 * <dl>
	 * <dt>cacheValue</dt>
	 * <dd>The value read from the cache (as a
	 * <code>Map&lt;String, Object&gt;</code>)</dd>
	 * </dl>
	 *
	 * @param cacheKey the key to look up in the cache
	 */
	@Consume("direct:cacheFileRead")
	@Trace
	public void cacheRead(@Header(CACHE_KEY) String cacheKey, @Headers Map<String, Object> headers) {

		if (StringUtils.isEmpty(cacheKey)) {
			throw new IllegalArgumentException(String.format(ERROR_STRING));
		}
		logger.debug("cache Key {} ", cacheKey);
		String cacheValue = cacheService.get(cacheKey);

		if (READ_CACHE_FAILURE_EXPIRED.stream().anyMatch(cacheValue.trim()::equalsIgnoreCase)) {
			logger.info("Cache miss for cache Key {}, value was {}", cacheKey, cacheValue);
		} else if (READ_CACHE_FAILURE_CAUSES.stream().anyMatch(cacheValue.trim()::equalsIgnoreCase)) {
			logger.error("Cache Read failed for cache Key : {} {}", cacheKey, cacheValue);
			throw PolicyCacheException.newReadFailedException(cacheKey);
		} else {
			headers.put(CACHE_VALUE, cacheValue);
		}

	}

	/**
	 * A direct route that deletes the entry in the cache associated with a given
	 * key.
	 *
	 * @param cacheKey the key to delete from the cache
	 */

	@Consume("direct:cacheFileDelete")
	@Trace
	public void cacheDelete(@Header(CACHE_KEY) String cacheKey) {
		logger.debug("cache Key {} ", cacheKey);
		cacheService.delete(cacheKey);
	}

	public void populateCache(String cacheKey, String cacheValue) {

		int status;
		logger.info("cache Key {} and cache expiration {} ", cacheKey);

		long durationMs = Duration.parse(CACHE_EXPIRE).toMillis();
		logger.info("cache duration in millisecs {} ", durationMs);
		status = cacheService.putWithExpiry(cacheKey, cacheValue, durationMs);

		if (status != HttpStatus.SC_OK) {
			logger.error("Cache writing failed for {} with status {}", cacheKey, status);
			throw PolicyCacheException.newWriteFailedException(cacheKey);
		}
	}


}
