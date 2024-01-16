package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;


import com.capitalone.identity.identitybuilder.client.dynamic.DynamicUpdateConfigurationProperties;
import com.capitalone.identity.identitybuilder.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test event formats logged and published for external use by clients of the library.
 * <ul>
 *     <li>Published: clients encouraged to set alerts on these values</li>
 *     <li>Logged: clients notified these values are subject to change</li>
 * </ul>
 * <h3>Test description:</h3>
 * <ol>
 *     <li>Checks namespace of serialized events</li>
 *     <li>Enforces the object type that is logged via mock return values</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class RuntimeUpdateEventContentTest {

    @Mock
    EntityInfo mockEntityInfo;

    @Spy
    RuntimeUpdateEventLogger logger;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);

    /**
     * @param o                    object with serialized form to verify
     * @param equalsSeparatedValue namespace that represents location of field in serialized json object. Expected
     *                             value can be placed after an '=' sign. If no equals sign is provided, this method
     *                             verifies that the value is not empty.
     */
    private void assertFieldValueContents(Object o, String equalsSeparatedValue) {
        // prep args
        String[] arguments = equalsSeparatedValue.split("=", 2);
        String name = arguments[0];
        String expectedValue = arguments.length > 1 ? arguments[1] : "";


        // parse the value at namespace
        String checked = name.replace("EventStreamData.response.", "");
        ArrayList<String> namespaceArray = new ArrayList<>(Arrays.asList(checked.split("\\.")));
        String result;
        try {
            String serialized = mapper.writeValueAsString(o);
            JsonNode node = mapper.readTree(serialized);

            while (!namespaceArray.isEmpty()) {
                String field = namespaceArray.remove(0);
                final JsonNode curNode = node;
                node = assertDoesNotThrow(() -> curNode.required(field));
            }

            result = StringUtils.isBlank(node.asText()) ? node.toString() : node.asText();

        } catch (JsonProcessingException e) {
            throw new IOError(e);
        }

        // apply conditions
        if (StringUtils.isNotBlank(expectedValue)) {
            assertEquals(expectedValue, result);
        } else {
            assertTrue(StringUtils.isNotBlank(result));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EventStreamData.response.entity.id=a",
            "EventStreamData.response.entity.location=a/9",
            "EventStreamData.response.result.status=SUCCESS",
    })
    void testPublishedUpdateInfo(String arg) {

        EntityState.Delta<EntityInfo> update = new EntityState.Delta<>(EntityState.Delta.ChangeType.UPDATE, mockEntityInfo);
        when(mockEntityInfo.getId()).thenReturn("a");
        when(mockEntityInfo.getVersion()).thenReturn("1");
        when(mockEntityInfo.getLocationPrefix()).thenReturn("a/9");
        when(mockEntityInfo.getType()).thenReturn(EntityType.POLICY);
        when(mockEntityInfo.getFilteredItemNames()).thenReturn(Collections.emptySet());

        when(logger.auditUpdate(any(), any(), any()))
                .thenAnswer(ignored -> LoggedUpdate.newFromLoad(update.getType(), update.getEntityInfo(), EntityActivationStatus.ACTIVE));

        //noinspection ConstantConditions
        LoggedUpdate loggedObject = logger.auditUpdate(null, null, null);
        assertFieldValueContents(loggedObject, arg);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EventStreamData.response.error",
    })
    void testPublishedTerminationEvents(String arg) {

        when(logger.auditTermination(any()))
                .thenAnswer(ignored -> new LoggedTerminationEvent(null));

        LoggedTerminationEvent loggedObject = logger.auditTermination(null);
        assertFieldValueContents(loggedObject, arg);

    }

    @Test
    void testPublishedPollingConfigurationInfo() {

        // has no error case
        DynamicUpdateProperties properties = new DynamicUpdateConfigurationProperties("12:00:00", "PT10S");
        when(logger.auditPollingConfigurationChange(any(), any()))
                .thenAnswer(ignored -> new LoggedPollingConfigurationChange(properties.toString(), null));

        //noinspection ConstantConditions
        LoggedPollingConfigurationChange loggedObject = logger.auditPollingConfigurationChange(null, null);

        assertFieldValueContents(loggedObject, "EventStreamData.response.error=null");
        assertFieldValueContents(loggedObject, "EventStreamData.response.description=" + loggedObject.getDescription());
        assertTrue(loggedObject.getDescription().contains("12:00"));
        assertTrue(loggedObject.getDescription().contains("PT10S"));


        // has error case
        when(logger.auditPollingConfigurationChange(any(), any()))
                .thenAnswer(ignored -> new LoggedPollingConfigurationChange(properties.toString(), "test"));

        //noinspection ConstantConditions
        loggedObject = logger.auditPollingConfigurationChange(null, null);
        assertFieldValueContents(loggedObject, "EventStreamData.response.error=");

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EventStreamData.response.type=UPDATE",
            "EventStreamData.response.entity.type=POLICY",
            "EventStreamData.response.entity.id=a",
            "EventStreamData.response.entity.location=a/9",
            "EventStreamData.response.entity.version=1",
            "EventStreamData.response.activationStatus=ACTIVE",
            "EventStreamData.response.entity.versionNumber=109",
            "EventStreamData.response.entity.items",
            "EventStreamData.response.entity.itemsCount=0",
            "EventStreamData.response.entity.ignoredNonPolicyItems=",
            "EventStreamData.response.result.status=SUCCESS",
            "EventStreamData.response.result.error=null",
    })
    void testLoggedUpdateInfo(String arg) {

        EntityState.Delta<EntityInfo> update = new EntityState.Delta<>(EntityState.Delta.ChangeType.UPDATE, mockEntityInfo);
        when(mockEntityInfo.getId()).thenReturn("a");
        when(mockEntityInfo.getLocationPrefix()).thenReturn("a/9");
        when(mockEntityInfo.getVersion()).thenReturn("1");
        when(mockEntityInfo.getPatchVersion()).thenReturn(109);
        when(mockEntityInfo.getType()).thenReturn(EntityType.POLICY);
        when(mockEntityInfo.getFilteredItemNames()).thenReturn(Collections.emptySet());

        when(logger.auditUpdate(any(), any(), any()))
                .thenAnswer(ignored -> LoggedUpdate.newFromLoad(update.getType(), update.getEntityInfo(), EntityActivationStatus.ACTIVE));

        //noinspection ConstantConditions
        LoggedUpdate loggedObject = logger.auditUpdate(null, null, null);

        assertFieldValueContents(loggedObject, arg);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EventStreamData.response.type=DELETE",
            "EventStreamData.response.entity.type=POLICY",
            "EventStreamData.response.entity.id=a",
            "EventStreamData.response.entity.location=a/9",
            "EventStreamData.response.entity.version=1",
            "EventStreamData.response.entity.versionNumber=109",
            "EventStreamData.response.entity.items",
            "EventStreamData.response.entity.itemsCount=0",
            "EventStreamData.response.entity.ignoredNonPolicyItems=",
            "EventStreamData.response.result.status=SUCCESS",
            "EventStreamData.response.result.error=null",
    })
    void testLoggedUnloadInfo(String arg) {

        EntityState.Delta<EntityInfo> update = new EntityState.Delta<>(EntityState.Delta.ChangeType.DELETE, mockEntityInfo);
        when(mockEntityInfo.getId()).thenReturn("a");
        when(mockEntityInfo.getLocationPrefix()).thenReturn("a/9");
        when(mockEntityInfo.getVersion()).thenReturn("1");
        when(mockEntityInfo.getPatchVersion()).thenReturn(109);
        when(mockEntityInfo.getType()).thenReturn(EntityType.POLICY);
        when(mockEntityInfo.getFilteredItemNames()).thenReturn(Collections.emptySet());

        when(logger.auditUnload(any()))
                .thenAnswer(ignored -> LoggedUpdate.newFromUnload(update.getEntityInfo()));

        //noinspection ConstantConditions
        LoggedUpdate loggedObject = logger.auditUnload(null);

        assertFieldValueContents(loggedObject, arg);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EventStreamData.response.type=DELETE",
            "EventStreamData.response.entity.type=POLICY",
            "EventStreamData.response.entity.id=a",
            "EventStreamData.response.entity.location=a/9",
            "EventStreamData.response.entity.version=1",
            "EventStreamData.response.entity.versionNumber=109",
            "EventStreamData.response.entity.items",
            "EventStreamData.response.entity.itemsCount=0",
            "EventStreamData.response.entity.ignoredNonPolicyItems=",
            "EventStreamData.response.result.status=ERROR",
    })
    void testLoggedErrorInfo(String arg) {

        RuntimeException error = new RuntimeException("test");
        EntityState.Delta<EntityInfo> update = new EntityState.Delta<>(EntityState.Delta.ChangeType.DELETE, mockEntityInfo);
        when(mockEntityInfo.getId()).thenReturn("a");
        when(mockEntityInfo.getLocationPrefix()).thenReturn("a/9");
        when(mockEntityInfo.getVersion()).thenReturn("1");
        when(mockEntityInfo.getPatchVersion()).thenReturn(109);
        when(mockEntityInfo.getType()).thenReturn(EntityType.POLICY);
        when(mockEntityInfo.getFilteredItemNames()).thenReturn(Collections.emptySet());
        when(logger.auditLoadOperationFailed(any(), any(), any()))
                .thenAnswer(ignored -> LoggedUpdate.newFromError(update.getEntityInfo(), EntityState.Delta.ChangeType.DELETE, error));

        //noinspection ConstantConditions
        LoggedUpdate loggedObject = logger.auditLoadOperationFailed(null, null, null);

        assertFieldValueContents(loggedObject, arg);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EventStreamData.response.scheduled=1970-01-01T00:00:00.1Z",
            "EventStreamData.response.start=1970-01-01T00:00:00.2Z",
            "EventStreamData.response.end=1970-01-01T00:00:00.3Z",
            "EventStreamData.response.scanType=POLL",
    })
    void testLoggedScanInfo(String arg) {

        ScanRequest mockRequest = Mockito.mock(ScanRequest.class);
        when(mockRequest.getScanType()).thenReturn(ScanRequest.ScanType.POLL);
        when(mockRequest.getStartScheduled()).thenReturn(100L);
        when(mockRequest.getStartActual()).thenReturn(200L);

        ConfigStoreScanCompleted mockResult = Mockito.mock(ConfigStoreScanCompleted.class);
        when(mockResult.getRequest()).thenReturn(mockRequest);
        when(mockResult.getEndActual()).thenReturn(300L);
        LoggedScan loggedScan = LoggedScan.newFromResult(mockResult);

        when(logger.auditScanResult(any())).thenAnswer(ignored -> loggedScan);

        //noinspection ConstantConditions
        LoggedScan loggedObject = logger.auditScanResult(null);
        assertFieldValueContents(loggedObject, arg);

    }

}
