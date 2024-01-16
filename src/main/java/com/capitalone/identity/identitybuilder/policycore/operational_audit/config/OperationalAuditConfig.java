package com.capitalone.identity.identitybuilder.policycore.operational_audit.config;

import com.capitalone.chassis.engine.converters.config.ConverterConfig;
import com.capitalone.chassis.engine.core.constants.Constants;
import com.capitalone.chassis.engine.dfs.emitter.core.builder.HostContextBuilder;
import com.capitalone.chassis.engine.dfs.emitter.core.config.DFSSpringConfig;
import com.capitalone.chassis.engine.dfs.emitter.core.support.AuditClientHelper;
import com.capitalone.identity.identitybuilder.audit.models.ConfigurableSettings;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditAspect;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.PolicyEvaluatedAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.CyberLoggingHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ChassisStatusCodeResolver;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.PolicyEvaluatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.client.RestTemplate;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper.AspectEventFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal configuration utilized by operational events only.
 */
@Configuration
@ConditionalOnProperty("identitybuilder.policycore.operational_audit.enabled")
@Import({DFSSpringConfig.class, ConverterConfig.class})
@AllArgsConstructor
class OperationalAuditConfig {

    private static final String CLIENT_ID_PROPERTY_NAME = "OPERATIONAL-AUDIT-CLIENT-ID";
    private static final String CLIENT_SECRET_PROPERTY_NAME = "OPERATIONAL-AUDIT-CLIENT-SECRET";
    private static final String SCHEMA_PROPERTY_NAME = "identitybuilder.policycore.operational_audit.schema_name";

    // System Environment (encrichment) Properties
    private final @Value(Constants.CHASSIS_APPLICATION_NAME_KEY) String applicationName;
    private final @Value(Constants.CHASSIS_APPLICATION_CODE_KEY) String applicationCode;
    private final @Value(Constants.CLUSTER_ID_PROPERTY) String clusterId;
    private final @Value(Constants.CONTAINER_ID_PROPERTY) String containerId;
    private final @Value(Constants.HOSTNAME_PROPERTY) String hostName;
    private final @Value(Constants.DEPLOYMENT_REGION_PROPERTY) String region;
    private final @Value("${ENVIRONMENT_NAME:UNK}") String environmentName;
    private final @Value("${${SERVICE_NAME}_client_id:}") String hostClientId;
    private final @Value("${ba}") String businessApplication;
    private final @Value("${identitybuilder.policycore.operational_audit.suppress_stack_trace:false}") Boolean suppressStackTrace;

    // Audit Producer Properties
    private final @Value("${" + ApplicationConstants.ONESTREAM_HOST + "}") String apiHost;
    private final @Value("${" + ApplicationConstants.ONESTREAM_PORT + "}") int apiPort;
    private final @Value("${" + CLIENT_ID_PROPERTY_NAME + ":}") String operationalAuditClientId;
    private final @Value("${" + CLIENT_SECRET_PROPERTY_NAME + ":}") String operationalAuditClientSecret;
    private final @Value("${env.gatewayURL.audit:${env.gatewayURL}}") String devExURL;
    private final @Value("${" + SCHEMA_PROPERTY_NAME + ":}") String schemaName;

    // Audit Configurable Properties (e.g. backoff strategy, batch size, concurrency, etc.)
    private final @Value("${identitybuilder.policycore.operational_audit.batch_size:100}") Integer batchSize;
    private final @Value("${identitybuilder.policycore.operational_audit.wait_between_batches:150}") Long waitBetweenBatchesMs;
    private final @Value("${identitybuilder.policycore.operational_audit.concurrent_connections:10}") Integer concurrentConnections;
    private final @Value("${identitybuilder.policycore.operational_audit.initial_backoff:250}") Integer retryInitialBackoff;
    private final @Value("${identitybuilder.policycore.operational_audit.retry_timeout:3000}") Integer retryTimeoutMs;
    private final @Value("${identitybuilder.policycore.operational_audit.max_allowed_retries:3}") Integer maxAllowedRetries;
    private final @Value("${identitybuilder.policycore.operational_audit.preemptive_fallback:false}") Boolean preemptiveLogFallback;
    private final @Value("${identitybuilder.policycore.operational_audit.max_queue_size:100000}") Integer maxMessageQueueSize;

    private final @Value("${policy.service.logs.fieldstofilter:}") String fieldsToFilter;

    @Bean
    OperationalAuditAspect operationalAuditAspect(final OperationalEventEnricher operationalEventEnricher,
            final OperationalAuditPublisher operationalAuditPublisher,
            final List<AspectEventFactory> eventFactoryList,
            final @Qualifier("awsHostContextCollector") AWSHostContextCollector awsHostContextCollector) {
        final Map<Class<? extends AspectEventFactory>, AspectEventFactory> eventFactoryMap = eventFactoryList.stream()
                .collect(Collectors.toMap(OperationalEventMapper.AspectEventFactory::getClass, val -> val));
        return new OperationalAuditAspect(operationalEventEnricher, operationalAuditPublisher,
                eventFactoryMap, awsHostContextCollector);
    }

    @Bean
    PolicyEvaluatedEventMapper.Factory policyEvaluatedEventMapperFactory(final @Qualifier("chassisStatusCodeResolver") ChassisStatusCodeResolver chassisStatusCodeResolver) {
        return new PolicyEvaluatedEventMapper.Factory(chassisStatusCodeResolver, fieldsToFilter);
    }

    @Bean
    PolicyEvaluatedAuditor.InternalAuditor toggleablePolicyEvaluatedAuditor(final OperationalEventEnricher operationalEventEnricher,
                                                                            final OperationalAuditPublisher operationalAuditPublisher,
                                                                            final @Qualifier("awsHostContextCollector") AWSHostContextCollector awsHostContextCollector,
                                                                            final PolicyEvaluatedEventMapper.Factory policyEvaluatedEventMapperFactory) {
        return new PolicyEvaluatedAuditor.InternalAuditor(operationalEventEnricher,
                operationalAuditPublisher, awsHostContextCollector, policyEvaluatedEventMapperFactory);
    }

    @Bean
    OperationalAuditor.OperationalAuditDispatcher toggleableAuditDispatcher(
            final OperationalEventEnricher operationalEventEnricher,
            final OperationalAuditPublisher operationalAuditPublisher,
            final @Qualifier("awsHostContextCollector") AWSHostContextCollector awsHostContextCollector) {

        // Create the audit dispatcher
        return new OperationalAuditor.OperationalAuditDispatcher(
                operationalEventEnricher,
                operationalAuditPublisher,
                awsHostContextCollector);
    }

    @Bean
    ChassisStatusCodeResolver chassisStatusCodeResolver(
            final @Qualifier("auditClientHelper") AuditClientHelper chassisAuditClientHelper,
            final @Qualifier("conversionService") ConversionService conversionService) {
        return new ChassisStatusCodeResolver(chassisAuditClientHelper, conversionService);
    }

    @Bean(name = "chassisSdpCyberLoggingContext")
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public CyberLoggingHostContext sdpCyberLoggingContext(
        @Value("${chassis.dfs.sdp.cyberlogging.suppress.discovery:false}") boolean suppressDiscovery,
        @Value("${chassis.dfs.sdp.cyberlogging.timeout:1000}") int timeout) {
      return new CyberLoggingHostContext(
          new HostContextBuilder(new RestTemplate(), timeout)
              .build(suppressDiscovery));
    }

    @Bean(name = "awsHostContextCollector")
    AWSHostContextCollector awsHostContextCollector(
            @Qualifier("chassisSdpCyberLoggingContext") CyberLoggingHostContext cyberLoggingHostContext) {
        return new AWSHostContextCollector(cyberLoggingHostContext);
    }


    /**
     * Creates the common operational audit enrichment helper (package scoped)
     */
    @Bean
    OperationalEventEnricher operationalAuditEventEnricher() {
        final OperationalSystemContext operationalSystemContext = OperationalSystemContext.builder()
                .applicationName(applicationName)
                .applicationCode(applicationCode)
                .clusterId(Strings.emptyToNull(clusterId))
                .containerId(Strings.isNullOrEmpty(containerId)
                        ? Strings.emptyToNull(hostName)
                        : containerId)
                .hostName(Strings.emptyToNull(hostName))
                .region(Strings.emptyToNull(region))
                .environmentName(Strings.emptyToNull(environmentName))
                .clientId(Strings.emptyToNull(hostClientId))
                .businessApplication(Strings.emptyToNull(businessApplication))
                .systemId(String.format("%s.%s.%s.%s-%s", applicationCode, environmentName,
                        !Strings.isNullOrEmpty(region) ? region : "NA",
                        !Strings.isNullOrEmpty(clusterId) ? clusterId : "NA",
                        !Strings.isNullOrEmpty(containerId) ? containerId : "NA"))
                .build();

        return new OperationalEventEnricher(operationalSystemContext,
                isProd() || suppressStackTrace);
    }

    /**
     * Creates the operational audit publisher, common to all operational auditors (package scoped)
     *
     * @return the operational audit publisher
     */
    @Bean
    OperationalAuditPublisher operationalAuditPublisher() {
        // Check for required properties
        final String missingPropertyErrorFormat = "The property, %s, could not be found on startup. This field is "
                + "required for publishing Operational Audits to OneIngest. Please reach out to the Policy Core "
                + "Development Team for non-prod/prod Operational Audit API on-boarding instructions and/or any general "
                + "questions. If testing locally against dollhouse then use the property value recommended in dollhouse "
                + "documentation or based on your local configuration.";
        if (StringUtils.isBlank(operationalAuditClientId)) {
            throw new IllegalArgumentException(String.format(missingPropertyErrorFormat, CLIENT_ID_PROPERTY_NAME));
        }
        final boolean isApiHostLocal = apiHost.equals("localhost") || apiHost.equals("127.0.0.1");
        if (!isApiHostLocal && StringUtils.isBlank(operationalAuditClientSecret)) {
            throw new IllegalArgumentException(String.format(missingPropertyErrorFormat, CLIENT_SECRET_PROPERTY_NAME));
        }
        if (StringUtils.isBlank(schemaName)) {
            throw new IllegalArgumentException(String.format(missingPropertyErrorFormat, SCHEMA_PROPERTY_NAME));
        }

        // Create the producer properties
        final ProducerProperties producerProperties = ProducerProperties.builder()
                .businessApplication("BAIDENTITYPOLICYSERVICES")
                .schemaName(schemaName)
                // Oauth
                .clientId(operationalAuditClientId)
                .clientSecret(operationalAuditClientSecret)
                .devexUrl(devExURL)
                .devexEnvConfig(isProd() ? "PROD" : "IT")
                // SDP target URL
                .fanInHost(apiHost)
                .fanInPort(apiPort)
                // Configure to flatten nested attributes
                .shouldFlatten(true)
                .build();

        // Create the configurable settings
        final ConfigurableSettings configurableSettings = ConfigurableSettings.builder()
                .concurrentConnections(concurrentConnections)
                .batchSize(batchSize)
                .waitBetweenBatchesMs(waitBetweenBatchesMs)
                .delayBetweenRetriesMs(retryInitialBackoff)
                .retryTimeoutMs(retryTimeoutMs)
                .maxAllowedRetries(maxAllowedRetries)
                .preemptiveFallback(preemptiveLogFallback)
                .maxAllowedQueueSize(maxMessageQueueSize)
                .build();

        // Create the audit publisher and register the schema
        final AuditPublisher auditPublisher = new AuditPublisher(configurableSettings);
        auditPublisher.newEntry(producerProperties);

        // Create the operational events audit publisher
        return new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName());
    }

    /**
     * Determine, based on configEnv whether the current environment is prod or not
     *
     * @return true if prod false otherwise
     */
    private boolean isProd() {
        return environmentName.toLowerCase(Locale.ROOT).contains("prod");
    }

}
