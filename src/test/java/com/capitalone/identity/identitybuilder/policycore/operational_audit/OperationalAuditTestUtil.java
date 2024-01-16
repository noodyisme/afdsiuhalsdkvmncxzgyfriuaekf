package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.times;

public class OperationalAuditTestUtil {

    public static <T> T registerWithAspect(final T obj, final OperationalAuditAspect operationalAuditAspect) {
        final AspectJProxyFactory factory = new AspectJProxyFactory(obj);
        factory.addAspect(operationalAuditAspect);
        return factory.getProxy();
    }

    public static <T> T registerMockWithCustomAspect(final T obj, final Object customAuditAspect) {
        final AspectJProxyFactory factory = new AspectJProxyFactory(obj);
        factory.addAspect(customAuditAspect);
        return factory.getProxy();
    }

    public static OperationalEvent captureOperationalEvent(final AuditPublisher auditPublisher,
                                                           ArgumentCaptor<AuditEvent> auditEventArgumentCaptor) {
        Mockito.verify(auditPublisher).publish(auditEventArgumentCaptor.capture());
        return (OperationalEvent) auditEventArgumentCaptor.getValue().getMessage();
    }

    public static List<OperationalEvent> captureOperationalEvents(final AuditPublisher auditPublisher,
                                                                  ArgumentCaptor<AuditEvent> auditEventArgumentCaptor, int expectedNumAuditEvents) {
        Mockito.verify(auditPublisher, times(expectedNumAuditEvents)).publish(auditEventArgumentCaptor.capture());
        return auditEventArgumentCaptor.getAllValues().stream().map(event -> (OperationalEvent) event.getMessage()).collect(Collectors.toList());

    }


}
