package com.capitalone.identity.identitybuilder.policycore.fileupload.camel.util;

import com.capitalone.dsd.elasticache.client.cluster.ElasticCacheService;
import com.capitalone.identity.identitybuilder.policycore.service.exception.PolicyCacheException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class FileUploadCamelCacheUtilTest {

	private static final String CACHE_VALUE = "cacheFileValue";
	private static final String CACHE_KEY = "cacheFileKey";
	private static final long CACHE_EXPIRATION_MS = 900000L;

	Map<String, Object> headers = new HashMap<>();

	@InjectMocks
	private FileUploadCamelCacheUtil fileUploadCamelCacheUtil;

	@Mock
	private ElasticCacheService cacheService;

	@Test
	void testCacheRead() {
		when(cacheService.get(CACHE_KEY)).thenReturn(CACHE_VALUE);
		fileUploadCamelCacheUtil.cacheRead(CACHE_KEY, headers);
		verify(cacheService, times(1)).get(eq(CACHE_KEY));
		assertEquals(CACHE_VALUE, this.headers.get(CACHE_VALUE));
	}

	@Test
	void testCacheRead_cacheKey_Null() {
		assertThrows(IllegalArgumentException.class, () -> fileUploadCamelCacheUtil.cacheRead(null, headers));
	}

	@Test
	void testCacheRead_keyNotFound() {
		when(cacheService.get(CACHE_KEY)).thenReturn("KEY_NOT_FOUND");
		fileUploadCamelCacheUtil.cacheRead(CACHE_KEY, headers);
		verify(cacheService, times(1)).get(eq(CACHE_KEY));
	}

	@Test
	void testCacheRead_token_expired() {
		when(cacheService.get(CACHE_KEY)).thenReturn("TOKEN_EXPIRED");
		fileUploadCamelCacheUtil.cacheRead(CACHE_KEY, headers);
		verify(cacheService, times(1)).get(eq(CACHE_KEY));
	}

	@Test
	void testCacheRead_connectionError() {
		when(cacheService.get(CACHE_KEY)).thenReturn("CONNECTION_ERROR");
		assertThrows(PolicyCacheException.class, () -> fileUploadCamelCacheUtil.cacheRead(CACHE_KEY, headers));
	}

	@Test
	void testCacheRead_timeoutError() {
		when(cacheService.get(CACHE_KEY)).thenReturn("TIMEOUT_ERROR");
		assertThrows(PolicyCacheException.class, () -> fileUploadCamelCacheUtil.cacheRead(CACHE_KEY, headers));
	}

	@Test
	void testCacheDelete() {
		fileUploadCamelCacheUtil.cacheDelete(CACHE_KEY);
		verify(cacheService, times(1)).delete(eq(CACHE_KEY));
	}

	@Test
	void testPopulateCache() {
		when(cacheService.putWithExpiry(CACHE_KEY, CACHE_VALUE, CACHE_EXPIRATION_MS)).thenReturn(HttpStatus.SC_OK);
		fileUploadCamelCacheUtil.populateCache(CACHE_KEY, CACHE_VALUE);
		verify(cacheService, times(1)).putWithExpiry(eq(CACHE_KEY), eq(CACHE_VALUE), eq(CACHE_EXPIRATION_MS));
	}

	@Test
	void testPopulateCache_exception() {
		when(cacheService.putWithExpiry(CACHE_KEY, CACHE_VALUE, CACHE_EXPIRATION_MS)).thenReturn(HttpStatus.SC_BAD_REQUEST);
		assertThrows(PolicyCacheException.class, () -> fileUploadCamelCacheUtil.populateCache(CACHE_KEY, CACHE_VALUE));
	}

}
