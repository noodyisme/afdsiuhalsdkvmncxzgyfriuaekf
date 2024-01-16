package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.ExternalItemDefinitionHolder;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineRuntimeLoadService;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.LoadRuleException;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroup;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockedStatic.Verification;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicPolicyLoaderImplTest {

    @Mock
    CamelContext context;

    @Mock
    DecisionEngineRuntimeLoadService decisionEngineDynamic;

    @Mock
    PolicyValidator policyValidator;

    @Mock
    ExternalRouteParser routeParser;

    DynamicPolicyLoader loader;

    @Mock
    Entity.Policy entity;

    @Mock
    EntityInfo.Policy entityInfo;

    @Mock
    SchemaManager schemaManager;

    @Spy
    ConfigManagementService configManagementService = Mockito.spy(new ConfigManagementService());

    @BeforeEach
    void setUp() {
        loader = new DynamicPolicyLoaderImpl(context, decisionEngineDynamic, policyValidator, routeParser, schemaManager, configManagementService, null, false, null);
    }

    void setUpAddMocks() {
        ExternalItemDefinitionHolder parsedItem = new ExternalItemDefinitionHolder(Collections.emptyList(), MockModel.pip(), EntityType.POLICY);
        when(routeParser.parseRouteResource(any())).thenReturn(parsedItem);
    }

    private void setUpEntityMock() {
        lenient().when(entity.getId()).thenReturn("testLob/testOrg/testPolicyName/1.0");
        lenient().when(entity.getInfo()).thenReturn((EntityInfo.Policy) entityInfo);
        lenient().when(entityInfo.getId()).thenReturn("testLob/testOrg/testPolicyName/1.0");
        lenient().when(entityInfo.getPatchVersion()).thenReturn(1);
    }

    @Test
    void add_parseFail() {
        setUpAddMocks();
        setUpEntityMock();

        RuntimeException error = new RuntimeException("parse error");
        when(routeParser.parseRouteResource(any())).thenThrow(error);

        UpdateTransactionResult add = loader.add(entity);

        assertEquals(UpdateTransactionResult.Status.ERROR, add.getStatus());
        assertEquals(error, add.getError());
    }

    @Test
    void add_validatorFail() {
        setUpAddMocks();
        setUpEntityMock();

        RuntimeException error = new RuntimeException("validation error");
        when(policyValidator.addModel(any())).thenThrow(error);

        UpdateTransactionResult add = loader.add(entity);

        assertEquals(UpdateTransactionResult.Status.ERROR, add.getStatus());
        assertEquals(error, add.getError());
    }

    @Test
    void add_camelFail_rolledBack() {
        setUpAddMocks();
        setUpEntityMock();

        when(policyValidator.removeModel(any())).thenReturn(true);

        // setup
        RuntimeException error = new RuntimeException("camel error");

        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            Verification addRouteDefinitionVerification = getAddRouteDefinitionVerification();
            mocked.when(addRouteDefinitionVerification).thenThrow(error);
            mocked.verify(addRouteDefinitionVerification, atMost(1));

            UpdateTransactionResult add = loader.add(entity);

            assertEquals(UpdateTransactionResult.Status.ERROR, add.getStatus());
            assertEquals(error, add.getError());
        }
    }

    @Test
    void add_camelFail_cleanup_failed() {
        setUpAddMocks();
        setUpEntityMock();

        // setup
        when(policyValidator.removeModel(any())).thenReturn(true);

        RuntimeException error = new RuntimeException("camel error");
        RuntimeException rollbackError = new RuntimeException("rollback error");

        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            Verification addRouteDefinitionVerification = getAddRouteDefinitionVerification();
            mocked.when(addRouteDefinitionVerification).thenThrow(error);
            mocked.verify(addRouteDefinitionVerification, atMost(1));

            Verification removeRouteDefinitionVerification = getRemoveRouteDefinitionVerification();
            mocked.when(removeRouteDefinitionVerification).thenThrow(rollbackError);
            mocked.verify(removeRouteDefinitionVerification, atMost(1));
            mocked.verifyNoMoreInteractions();

            try {
                loader.add(entity);
                fail("failure expected here");
            } catch (RuntimeException e) {
                assertEquals(error, e.getCause());
                assertEquals(rollbackError, e.getSuppressed()[0]);
            }

        }
    }

    @Test
    void add_dmnFail() {
        setUpAddMocks();
        setUpEntityMock();
        doThrow(LoadRuleException.class)
                .when(decisionEngineDynamic)
                .loadRulesIntoDecisionRuntime(Mockito.any(RuleDefinitionModelGroup.class));

        final String RULE_NAME = "rules/ABCDE";
        when(entity.getRuleItems()).thenReturn(Collections.singleton(
                new ConfigStoreItem(new ConfigStoreItemInfo(RULE_NAME, "a"), "")
        ));
        when(policyValidator.removeModel(any())).thenReturn(true);

        // setup
        final Class<LoadRuleException> expectedExceptionType = LoadRuleException.class;

        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            Verification addRouteDefinitionVerification = getAddRouteDefinitionVerification();
            mocked.when(addRouteDefinitionVerification).then((Answer<String>) invocation -> "");
            Verification removeRouteDefinitions = getRemoveRouteDefinitionVerification();
            mocked.when(removeRouteDefinitions).then((Answer<String>) invocation -> "");

            UpdateTransactionResult add = loader.add(entity);

            assertEquals(UpdateTransactionResult.Status.ERROR, add.getStatus());
            mocked.verify(addRouteDefinitionVerification, times(1));
            mocked.verify(removeRouteDefinitions, times(1));
            mocked.verifyNoMoreInteractions();

            Throwable thrownError = add.getError();
            assertNotNull(thrownError);
            assertTrue(expectedExceptionType.isInstance(thrownError),
                    String.format("During dmn load operation, expected exception type %s, got %s",
                            expectedExceptionType.getSimpleName(), thrownError.getClass().getSimpleName()));
        }
    }

    @Test
    void add_configFail() {

        setUpEntityMock();
        setUpAddMocks();

        ConfigManagementModel configManagementModel = ConfigManagementModel.newInstance(Collections.emptyMap(), Collections.emptyMap());
        when(entity.getConfigManagementModelForEnv(any())).thenReturn(Optional.of(configManagementModel));

        RuntimeException expectError = new RuntimeException("test");
        doThrow(expectError).when(configManagementService).setPolicyConfiguration(any(), any());

        UpdateTransactionResult add = loader.add(entity);
        assertEquals(UpdateTransactionResult.Status.ERROR, add.getStatus());
        assertEquals(expectError, add.getError());
    }

    @Test
    void add_success() {
        setUpAddMocks();
        setUpEntityMock();

        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            Verification addRouteDefinitionVerification = getAddRouteDefinitionVerification();
            mocked.when(addRouteDefinitionVerification).then((Answer<String>) invocation -> "");
            // setup
            UpdateTransactionResult add = loader.add(entity);

            mocked.verify(addRouteDefinitionVerification, times(1));
            mocked.verifyNoMoreInteractions();
            assertEquals(UpdateTransactionResult.Status.SUCCESS, add.getStatus());
        }
    }

    @Test
    void add_success_enforceDelete() {
        setUpAddMocks();
        setUpEntityMock();
        loader = new DynamicPolicyLoaderImpl(context, decisionEngineDynamic, policyValidator, routeParser, schemaManager, configManagementService, ClientEnvironment.DEV, true, null);
        when(policyValidator.removeModel(any())).thenReturn(true);

        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            Verification addRouteDefinitionVerification = getAddRouteDefinitionVerification();
            mocked.when(addRouteDefinitionVerification).then((Answer<String>) invocation -> "");
            Verification deleteRouteDefinitionVerification = getRemoveRouteDefinitionVerification();
            mocked.when(deleteRouteDefinitionVerification).then((Answer<String>) invocation -> "");

            // setup
            UpdateTransactionResult add = loader.add(entity);

            mocked.verify(addRouteDefinitionVerification, times(2));
            mocked.verify(deleteRouteDefinitionVerification, times(1));
            mocked.verifyNoMoreInteractions();

            assertEquals(UpdateTransactionResult.Status.SUCCESS, add.getStatus());
        }
    }

    @Test
    void add_twice_fail() {
        setUpAddMocks();
        setUpEntityMock();

        // setup
        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            Verification addRouteDefinitionVerification = getAddRouteDefinitionVerification();
            mocked.when(addRouteDefinitionVerification).then((Answer<String>) invocation -> "");
            loader.add(entity);

            mocked.verify(addRouteDefinitionVerification, times(1));
            mocked.verifyNoMoreInteractions();

            assertThrows(IllegalArgumentException.class, () -> loader.add(entity));
        }
    }

    @Test
    void add_badEntity() {
        // setup
        Entity.Access mock = mock(Entity.Access.class);
        String entityId = "testLob/testOrg/testPolicyName/1.0";
        EntityInfo.Access mockInfo = mock(EntityInfo.Access.class);
        when(mock.getInfo()).thenReturn(mockInfo);
        when(mockInfo.getId()).thenReturn(entityId);
        when(mockInfo.getPatchVersion()).thenReturn(0);

        UpdateTransactionResult result = loader.add(mock);
        assertTrue(result.isError());
        assertTrue(result.getError() instanceof UnsupportedOperationException);
        String message = result.getError().getMessage();
        assertTrue(message.contains("Access"));
        assertTrue(message.contains(entityId));
    }

    @Test
    void delete_success() {
        setUpAddMocks();
        setUpEntityMock();

        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            Verification addRouteDefinitionVerification = getAddRouteDefinitionVerification();
            mocked.when(addRouteDefinitionVerification).then((Answer<String>) invocation -> "");
            mocked.verify(addRouteDefinitionVerification, atMost(1));
            mocked.verifyNoMoreInteractions();

            assertTrue(loader.add(entity).isSuccess());
            UpdateTransactionResult update = loader.delete(entityInfo);

            verify(schemaManager, only()).purgeCache();
            assertEquals(UpdateTransactionResult.Status.SUCCESS, update.getStatus());
        }
    }

    @Test
    void delete_no_exist_fail() {
        setUpEntityMock();
        assertThrows(NullPointerException.class, () -> loader.delete(entityInfo));
    }

    @Test
    void delete_error() {
        setUpAddMocks();
        setUpEntityMock();

        RuntimeException error = new RuntimeException("delete error");
        doThrow(error).when(policyValidator).removeModel(any());

        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            Verification addRouteDefinitionVerification = getAddRouteDefinitionVerification();
            mocked.when(addRouteDefinitionVerification).then((Answer<String>) invocation -> "");
            mocked.verify(addRouteDefinitionVerification, atMost(1));
            mocked.verifyNoMoreInteractions();

            loader.add(entity);
            UpdateTransactionResult update = loader.delete(entityInfo);

            assertEquals(UpdateTransactionResult.Status.ERROR, update.getStatus());
        }
    }

    private Verification getAddRouteDefinitionVerification() {
        return () -> DynamicPolicyHelper.addRouteDefinitions(any(), anyCollection());
    }

    private Verification getRemoveRouteDefinitionVerification() {
        return () -> DynamicPolicyHelper.removeRouteDefinitions(any(), anyCollection());
    }
}
