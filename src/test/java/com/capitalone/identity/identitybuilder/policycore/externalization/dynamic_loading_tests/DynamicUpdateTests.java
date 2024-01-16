package com.capitalone.identity.identitybuilder.policycore.externalization.dynamic_loading_tests;

import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.model.ScanRequest;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventLogger;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.externalization.EntityUpdateService;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadEventVisitor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import com.capitalone.identity.identitybuilder.polling.ScanRequester;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.core.util.Loader.getClassLoader;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;


@ContextConfiguration(classes = {
        DynamicUpdateTests.StartupTestConfig.class
})
@CamelSpringBootTest
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "chassis.spring.boot.starter.sdp.enabled=false",
        "chassis.spring.boot.starter.DFS.sdp.enabled=false",
        "chassis.spring.boot.starter.native.oath.enabled=false",
        "csc.dynamic-updates.polling-interval=PT2S",
        "csc.dynamic-updates.enabled=true",
        "csc.client-environment=prod",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=" + DynamicUpdateTests.POLICY_ROOT,
        "identitybuilder.policycore.feature.version-forwarder.enabled=true",
        "identitybuilder.policycore.abac-enforcement.strict=false",
        "identitybuilder.policycore.feature.strict-policy-start-mode=true",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false"
})
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DynamicUpdateTests {

    static final String POLICY_ROOT = "externalization/dynamic_loading/dynamic_test_policy";
    private static final String POLICY_ROOT_DIR = Objects.requireNonNull(getClassLoader().getResource(POLICY_ROOT)).getPath();
    private static final String POLICY_FULL_PATH = POLICY_ROOT_DIR + "/1.0";
    private static final String POLICY_CLONE_FULL_PATH = POLICY_ROOT_DIR + "/1.10";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @AfterAll
    static void cleanupFilesPostTest() throws IOException {
        FileUtils.deleteDirectory(new File(POLICY_CLONE_FULL_PATH));
    }

    private static List<String> getPublicEntryPointsForPatch(String patch) {
        return Arrays.asList(
                String.format("policy:dynamic_test_policy_%s?timeout=500", patch),
                String.format("direct:dynamic_test_policy_%s-schema?timeout=500", patch)
        );
    }

    @EndpointInject("mock:end")
    MockEndpoint mockEnd;

    @Autowired
    ProducerTemplate producer;

    @Autowired
    PolicyVersionService versionService;

    @Autowired
    EntityLoadEventVisitor eventLogger;

    @Autowired
    ScanRequester testScanRequester;

    @Autowired
    CamelContext camelContext;

    @BeforeEach
    void clearMocks() {
        camelContext.start();
        Mockito.clearInvocations(eventLogger);
    }

    @Test
    void testPolicyPatch_00_Bootstrap() {
        PolicyInfo.Patch patch = versionService.getPolicyVersion("dynamic_test_policy", "1.0");
        assertNotNull(patch);
        String patchValue = "1.0.0";
        assertEquals(0, patch.getPolicyPatchVersion());
        assertEquals(patchValue, patch.getPolicyPatchVersionString());

        assertResponseContains("test.config.dynamic", patchValue, "test-config-dynamic-value");
    }

    /**
     * Ensure this version handles legacy policy deployment correctly.
     */
    @Test
    void testPolicyPatch_00_LegacyDeploySuccess() throws IOException {
        assertFalse(Files.exists(Paths.get(POLICY_CLONE_FULL_PATH)));

        // no need to update the metadata file b.c. there is already a no-op entry in metadata.json that
        // isn't loaded because there are no matching files under the cloned directory
        FileUtils.copyDirectory(new File(POLICY_FULL_PATH), new File(POLICY_CLONE_FULL_PATH));

        ((TestScanRequester) testScanRequester).publisher.tryEmitNext(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));

        verify(eventLogger, times(0)).onNonEntityError(any());
        verify(eventLogger).onEntityLoaded(argThat(loaded ->
                loaded.getChangeType() == EntityState.Delta.ChangeType.ADD
                        && loaded.getInfo().getPatchVersion() == 0
        ));

        PolicyInfo.Patch patch = versionService.getPolicyVersion("dynamic_test_policy", "1.10");
        assertEquals(0, patch.getPolicyPatchVersion());
        assertEquals("1.10.0", patch.getPolicyPatchVersionString());
    }

    /**
     * Typical first deployment to add a config file
     */
    @Test
    void testPolicyPatch_01_AddConfigurationSuccess() throws IOException {
        String destResource = POLICY_CLONE_FULL_PATH + "/1";

        assertFalse(Files.exists(Paths.get(destResource)));

        FileUtils.copyDirectory(new File(POLICY_CLONE_FULL_PATH), new File(destResource));

        // add a metadata file in the new format
        BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(destResource + "/policy-metadata.json"));
        bufferedWriter.write("{\"Status\": \"AVAILABLE\"}");
        bufferedWriter.close();

        // add a configuration (legal update)f
        BufferedWriter newUseCaseDataWriter = Files.newBufferedWriter(Paths.get(destResource + "/config/test.config.dynamic.new.json"));
        final String expectedNewUseCaseValue = "test.config.dynamic.new" + UUID.randomUUID();
        newUseCaseDataWriter.write(String.format("{\"customer.code\": \"%s\"}", expectedNewUseCaseValue));
        newUseCaseDataWriter.close();

        BufferedWriter updateUseCaseWriter = Files.newBufferedWriter(Paths.get(destResource + "/config/test.config.dynamic.json"));
        final String expectedChangedUseCaseValue = "test.config.dynamic.changed" + UUID.randomUUID();
        updateUseCaseWriter.write(String.format("{\"customer.code\": \"%s\"}", expectedChangedUseCaseValue));
        updateUseCaseWriter.close();

        ((TestScanRequester) testScanRequester).publisher.tryEmitNext(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));

        verify(eventLogger, times(0)).onNonEntityError(any());
        verify(eventLogger).onEntityLoaded(argThat(loaded ->
                loaded.getChangeType() == EntityState.Delta.ChangeType.UPDATE
                        && loaded.getInfo().getPatchVersion() == 1
        ));

        PolicyInfo.Patch patch = versionService.getPolicyVersion("dynamic_test_policy", "1.10");
        assertNotNull(patch);
        assertEquals(1, patch.getPolicyPatchVersion());
        assertEquals("1.10.1", patch.getPolicyPatchVersionString());

        String patchValue = "1.10.1";
        assertResponseContains("test.config.dynamic.new", patchValue, expectedNewUseCaseValue);
        assertResponseContains("test.config.dynamic", patchValue, expectedChangedUseCaseValue);
    }

    @Test
    void testPolicyPatch_02_DeleteConfigSuccess() throws IOException {
        String srcResource = POLICY_CLONE_FULL_PATH + "/1";
        String destResource = POLICY_CLONE_FULL_PATH + "/2";

        assertFalse(Files.exists(Paths.get(destResource)));
        FileUtils.copyDirectory(new File(srcResource), new File(destResource));

        Files.delete(Paths.get(destResource + "/config/test.config.dynamic.json"));

        ((TestScanRequester) testScanRequester).publisher.tryEmitNext(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));

        verify(eventLogger, times(0)).onNonEntityError(any());
        verify(eventLogger).onEntityLoaded(argThat(loaded ->
                loaded.getChangeType() == EntityState.Delta.ChangeType.UPDATE
                        && loaded.getInfo().getPatchVersion() == 2
        ));

        PolicyInfo.Patch patch = versionService.getPolicyVersion("dynamic_test_policy", "1.10");
        assertEquals(2, patch.getPolicyPatchVersion());
        assertEquals("1.10.2", patch.getPolicyPatchVersionString());
    }

    /**
     * Deployment to update the activation status of a policy
     */
    @Test
    void testPolicyPatch_03_DeactivateSuccess() throws IOException {
        assertNull(versionService.getPolicyVersion("dynamic_test_policy", "1"));

        String srcResource = POLICY_CLONE_FULL_PATH + "/1";
        String destResource = POLICY_CLONE_FULL_PATH + "/3";

        assertFalse(Files.exists(Paths.get(destResource)));
        FileUtils.copyDirectory(new File(srcResource), new File(destResource));

        BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(destResource + "/policy-metadata.json"));
        bufferedWriter.write("{\"Status\": \"DISABLED\"}");
        bufferedWriter.close();

        ((TestScanRequester) testScanRequester).publisher.tryEmitNext(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));

        verify(eventLogger, times(0)).onNonEntityError(any());
        verify(eventLogger).onEntityLoaded(argThat(loaded ->
                loaded.getChangeType() == EntityState.Delta.ChangeType.UPDATE
                        && loaded.getInfo().getPatchVersion() == 3
        ));

        assertNull(versionService.getPolicyVersion("dynamic_test_policy", "1.10"));
        assertNull(versionService.getPolicyVersion("dynamic_test_policy", "1"));

    }

    /**
     * Deployment to update the activation status of a policy
     */
    @Test
    void testPolicyPatch_04_ActivateSuccess() throws IOException {
        assertNull(versionService.getPolicyVersion("dynamic_test_policy", "1"));

        String srcResource = POLICY_CLONE_FULL_PATH + "/1";
        String destResource = POLICY_CLONE_FULL_PATH + "/4";

        assertFalse(Files.exists(Paths.get(destResource)));
        FileUtils.copyDirectory(new File(srcResource), new File(destResource));

        BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(destResource + "/policy-metadata.json"));
        bufferedWriter.write("{\"Status\": \"ACTIVE\"}");
        bufferedWriter.close();

        ((TestScanRequester) testScanRequester).publisher.tryEmitNext(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));

        verify(eventLogger, times(0)).onNonEntityError(any());
        verify(eventLogger).onEntityLoaded(argThat(loaded ->
                loaded.getChangeType() == EntityState.Delta.ChangeType.UPDATE
                        && loaded.getInfo().getPatchVersion() == 4
        ));

        PolicyInfo.Patch patch = versionService.getPolicyVersion("dynamic_test_policy", "1");
        assertEquals(4, patch.getPolicyPatchVersion());
        assertEquals("1.10.4", patch.getPolicyPatchVersionString());
    }


    @Test
    void testPolicyPatch_05_ContentUpdateFails() throws IOException {
        String srcResource = POLICY_CLONE_FULL_PATH + "/4";
        String destResource = POLICY_CLONE_FULL_PATH + "/10";

        assertTrue(Files.exists(Paths.get(srcResource)));
        assertFalse(Files.exists(Paths.get(destResource)));
        FileUtils.copyDirectory(new File(srcResource), new File(destResource));

        BufferedWriter newPolicyDataWriter = Files.newBufferedWriter(Paths.get(destResource + "/process/empty_file.xml"));
        newPolicyDataWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><routes></routes>");
        newPolicyDataWriter.close();

        ((TestScanRequester) testScanRequester).publisher.tryEmitNext(new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL));

        verify(eventLogger, times(0)).onNonEntityError(any());
        verify(eventLogger).onEntityLoadFailed(argThat(loaded ->
                loaded.getChangeType() == EntityState.Delta.ChangeType.UPDATE
                        && loaded.getInfo().getPatchVersion() == 10
        ));

        PolicyInfo.Patch patch = versionService.getPolicyVersion("dynamic_test_policy", "1.10");
        assertNotNull(patch);
        assertEquals(4, patch.getPolicyPatchVersion());
        assertEquals("1.10.4", patch.getPolicyPatchVersionString());
    }

    private void assertResponseContains(String businessEvent, String patchValue, String expectedValue) {
        List<String> publicEntryPoints = getPublicEntryPointsForPatch(patchValue);
        for (String endpoint : publicEntryPoints) {
            String configAddedResult = getResultFromEndpoint(endpoint, patchValue, businessEvent);
            assertTrue(configAddedResult.contains(expectedValue),
                    String.format("Expected text [%s] not found in result [%s]", expectedValue, configAddedResult));
        }
    }

    private String getResultFromEndpoint(String endpoint, String patchValue, String businessEvent) {

        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "dynamic_test_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION_PATCH, patchValue);
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, businessEvent);

        int start = mockEnd.getReceivedCounter();
        producer.sendBodyAndHeaders(endpoint, new HashMap<>(), headers);
        int end = mockEnd.getReceivedCounter();
        List<Object> results = mockEnd.getReceivedExchanges().subList(start, end).stream().map(exchange -> exchange.getMessage().getBody()).collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Configuration
    @ComponentScan(
            basePackages = {
                    "com.capitalone.identity.identitybuilder.client",
                    "com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic",
                    "com.capitalone.identity.identitybuilder.policycore.decisionengine",
                    "com.capitalone.identity.identitybuilder.decisionengine",
                    "com.capitalone.identity.identitybuilder.policycore.externalization",
                    "com.capitalone.identity.identitybuilder.policycore.service.dmn"
            },
            basePackageClasses = {SchemaManager.class, ConfigManagementService.class, PolicyAccessRegistry.class,
                    EntityUpdateService.class, PolicyVersionService.class},
            excludeFilters = @ComponentScan.Filter(value = CamelSpringBootTest.class, type = FilterType.ANNOTATION)
    )
    public static class StartupTestConfig {

        @Bean
        EntityLoadEventVisitor loadEventVisitor() {
            return Mockito.mock(EntityLoadEventVisitor.class);
        }

        @Bean
        ScanRequester testScanRequester() {
            return new TestScanRequester();
        }

        @Bean
        RuntimeUpdateEventLogger testLogger() {
            return Mockito.mock(RuntimeUpdateEventLogger.class);
        }

        @Bean
        OperationalAuditor operationalAuditor() {
            return new OperationalAuditor(null);
        }

        @Bean
        protected CamelContext camelContext(ApplicationContext applicationContext) {
            CamelContext camelContext = new SpringCamelContext(applicationContext);
            camelContext.adapt(ModelCamelContext.class)
                    .setDataFormats(Collections.singletonMap("jackson", new JsonDataFormat()));
            camelContext.build();
            return camelContext;
        }

        @Bean
        protected ProducerTemplate producerTemplate(CamelContext camelContext) {
            return camelContext.createProducerTemplate();
        }
    }

    public static final class TestScanRequester implements ScanRequester {

        public final Sinks.Many<ScanRequest> publisher = Sinks.many().multicast().onBackpressureBuffer();

        @Override
        public Flux<ScanRequest> getScanRequests() {
            return publisher.asFlux();
        }
    }


}
