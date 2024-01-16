package com.capitalone.identity.identitybuilder.policycore.camel.util;

import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class StringTemplateDependencyTest {

    @Test
    public void dependencyLoaded() {
        assertDoesNotThrow(() -> new ST("String Template Loaded <name>"));
    }
}