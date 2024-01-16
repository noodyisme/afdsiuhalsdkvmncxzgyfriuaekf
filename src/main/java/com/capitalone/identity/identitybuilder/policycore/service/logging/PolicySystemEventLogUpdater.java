package com.capitalone.identity.identitybuilder.policycore.service.logging;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.google.common.base.Strings;
import lombok.extern.log4j.Log4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Optional;

@Aspect
@Singleton
@Component
@Order(0)
@Log4j
public class PolicySystemEventLogUpdater {

    final String applicationName;
    final String applicationCode;
    final String clusterId;
    final String containerId;
    final String systemRuntimeId;
    final String region;
    final String environmentName;

    @Inject
    public PolicySystemEventLogUpdater(@Value("${chassis.app.name}") String applicationName,
                                       @Value("${chassis.app.code}") String applicationCode,
                                       @Value("${CLUSTER_ID:}") String clusterId,
                                       @Value("${CONTAINER_ID:}") String containerId,
                                       @Value("${HOSTNAME:}") String hostname,
                                       @Value("${DEPLOYMENT_REGION:}") String region,
                                       @Value("${ENVIRONMENT_NAME:UNK}") String environmentName
    ) {
        log.debug("PolicySystemEventLogUpdater Args: " + String.join("|", applicationName, applicationCode,
                clusterId, containerId, hostname, region, environmentName));
        this.applicationName = applicationName;
        this.applicationCode = applicationCode;
        this.containerId = Strings.isNullOrEmpty(containerId)
                ? Strings.emptyToNull(hostname)
                : containerId;
        this.clusterId = Strings.emptyToNull(clusterId);
        this.region = Strings.emptyToNull(region);
        this.environmentName = Strings.emptyToNull(environmentName);
        this.systemRuntimeId = String.format("%s.%s.%s.%s-%s", applicationName, environmentName,
                Optional.ofNullable(this.region).orElse("NA"),
                Optional.ofNullable(this.clusterId).orElse("NA"),
                Optional.ofNullable(this.containerId).orElse("NA"));
    }


    @Around("@annotation(com.capitalone.identity.identitybuilder.policycore.service.logging.PolicySystemEventLog)")
    public Object applyRequestContextForSystemEvent(ProceedingJoinPoint joinPoint) throws Throwable {

        if (RequestContextHolder.getRequestContextOrDefault().isNotNull()) {
            return joinPoint.proceed();
        } else {
            RequestContext requestContext = new RequestContext();
            requestContext.setApplicationName(applicationName);
            requestContext.setApplicationCode(applicationCode);
            requestContext.setCorrelationId(systemRuntimeId);
            if (containerId != null) requestContext.setContainerId(containerId);
            if (clusterId != null) requestContext.setClusterId(clusterId);
            if (region != null) requestContext.setRegion(region);
            if (environmentName != null) requestContext.setEnvironmentName(environmentName);

            requestContext.setEventStartTime(Instant.now());
            RequestContextHolder.put(requestContext);
            try {
                return joinPoint.proceed();
            } finally {
                RequestContextHolder.clearRequestContext();
            }
        }

    }
}
