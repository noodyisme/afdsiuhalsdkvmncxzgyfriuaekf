package com.capitalone.identity.identitybuilder.policycore.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyProcessMetadataTest {

    @Test
    void getPolicyProcessId() {
        ProcessMetadata metadata = new ProcessMetadata();
        metadata.setResourceId("abc123");
        metadata.setPolicyName("policy_a");
        metadata.setPolicyVersion("1.1");
        metadata.setStepExpiration(LocalDateTime.of(2022, 10, 6, 21, 3, 1, 234));
        metadata.setEffectiveNextStep("stepB");
        metadata.setStepsCompleted(Collections.singletonList("stepA"));
        metadata.setAvailableNextSteps(Stream.of("stepB", "stepC").collect(Collectors.toSet()));

        JourneyProcessMetadata data = new JourneyProcessMetadata(metadata);
        assertEquals(metadata.getResourceId(), data.getPolicyProcessId());
        assertEquals(metadata.getPolicyName(), data.getPolicyName());
        assertEquals(metadata.getPolicyVersion(), data.getPolicyVersion());
        assertEquals(metadata.getStepExpiration(), data.getStepExpirationTimestamp());
        assertEquals(metadata.getEffectiveNextStep(), data.getEffectiveNextStep());
        assertEquals(metadata.getStepsCompleted(), data.getStepsCompleted());
        assertEquals(metadata.getAvailableNextSteps(), data.getAvailableNextSteps());
    }
}
