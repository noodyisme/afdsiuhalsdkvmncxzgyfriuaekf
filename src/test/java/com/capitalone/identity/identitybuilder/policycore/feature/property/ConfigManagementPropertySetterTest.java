package com.capitalone.identity.identitybuilder.policycore.feature.property;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagementPropertySetterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "{{policy-config-feature:featureX}}",
            " {{policy-config-feature:featureX}} ",
            "{{ policy-config-feature:featureX }}",
            "{{  policy-config-feature:featureX  }}",
            "{{  policy-config-feature:featureX-dash-test  }}",
            "{{  policy-config-feature:featureX.dot.test  }}",
            "{{  policy-config-feature:featureX_under_test  }}",
            "{{!policy-config-feature:featureX:}}",
            "{{!policy-config-feature:featureX:Hello}}",
            "{{policy-config-feature:featureX?}}",
            "{{!policy-config-feature:featureX?Hello}}",
            "{{?policy-config-feature:featureX}}",
            "{{!policy-config-feature:featureX}}",
            // braces and content
            "abc:def:ghi?test=True&headerOutput={{  policy-config-feature:featureX  }}",
            "abc:def:ghi?test=True\n\t&headerOutput={{  policy-config-feature:featureX  }}",
            "abc:def:ghi?test=True\n\t&headerOutput={{\n\tpolicy-config-feature:featureX  }}\n\t",
            "policy-config-feature:featureK {{  policy-config-feature:featureX  }}",
            "{{policy-config-feature:featureK} {{policy-config-feature:featureX}}",
            "{{ policy-config-feature:featureK } {{policy-config-feature:featureX}} }",
            "{{ policy-config-feature:featureK {{policy-config-feature:featureX}} }}",
            "{{  policy-config-feature:featureX  }} policy-config-feature:featureK ",
            "{{policy-config-feature:featureX}} {{policy-config-feature:featureK}",
            "{{{policy-config-feature:featureX}} {policy-config-feature:featureK }}",
            "{{{{policy-config-feature:featureX}} policy-config-feature:featureK}}",
            // multiples
            "{{policy-config-feature:featureX}}{{policy-config-feature:featureX}}",
            "{{policy-config-feature:featureX}}{{policy-config-feature:featureY}}",
            "{{ policy-config-feature:featureX }} {{ policy-config-feature:featureY}}",
            "{{!policy-config-feature:featureX:Hello}}{{!policy-config-feature:featureY:Hello}}",
            "{{policy-config-feature:featureX?}}{{policy-config-feature:featureY?}}",
            "{{policy-config-feature:featureX?Hello}}{{policy-config-feature:featureY?Helo}}",
            "{{?policy-config-feature:featureX}}{{!policy-config-feature:featureY}}",
            // expression
            "version ${headers.internalPolicyVersionHeader} test-feature-property-iso [property={{policy-config-feature:featureX}}, config=${headers.testFeaturePropertyIso}]"
    })
    void returnNamespacedProperty(String input) {
        String namespace = "abc/def/ghi/1.0.2";
        String expectedResult = input.replaceAll("featureX", namespace + "/featureX");
        expectedResult = expectedResult.replaceAll("featureY", namespace + "/featureY");
        String change = ConfigManagementPropertySetter.getNamespacedPropertyTransformation(input, namespace);
        assertNotNull(change);
        assertEquals(expectedResult, change);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test",
            "{{policy-config-feature:abc/featureX}}",
            "{{policy-config-feature:abc/dev/hij/1.0.0/featureX}}",
            "{{policy-config-feature:abc/featureX}}{{policy-config-feature:abc/dev/hij/1.0.0/featureX}}",
    })
    @NullAndEmptySource
    void returnNoChange(String input) {
        String namespace = "abc/def/ghi/1.0.2";
        assertNull(ConfigManagementPropertySetter.getNamespacedPropertyTransformation(input, namespace));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "abc/def/ghi/1.0.2/featureX,abc/def/ghi/1.0.2,featureX",
            "a/b/c/1.1.0/featureY,a/b/c/1.1.0,featureY",
            "///0.0.0/a,///0.0.0,a",
            "///0.0.0/ ,///0.0.0, ",
    }, ignoreLeadingAndTrailingWhitespace = false)
    void extractInformationFromProperty(String input, String expectedPolicyAddress, String expectedPropertyName) {
        assertEquals(expectedPolicyAddress, ConfigManagementPropertySetter.extractPolicyId(input));
        String actual = ConfigManagementPropertySetter.extractProperty(input);
        assertEquals(expectedPropertyName, actual);
    }

    @Test
    void testConfigManagmentPropertiesFunctionCreateError() {
        assertThrows(NullPointerException.class,
                () -> new ConfigManagementPropertySetter.ConfigManagementPropertyFunction(null));
    }

    @Test
    void testConfigManagmentPropertiesFunctionCreateSuccess() {
        ConfigManagementService configManagementService = new ConfigManagementService();
        ConfigManagementPropertySetter.ConfigManagementPropertyFunction function =
                new ConfigManagementPropertySetter.ConfigManagementPropertyFunction(configManagementService);
        assertEquals(ConfigManagementPropertySetter.POLICY_CONFIG_FEATURE_PLACEHOLDER_NAME, function.getName());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "a/b/c/1.0.1,featureX,propertyValue",
            "a/b/c/1.0.1,featureY,propertyValue",
    })
    void testConfigManagmentPropertiesFunctionExecute(String policyId, String propertyKey, String propertyValue) {
        ConfigManagementService configManagementService = new ConfigManagementService();
        HashMap<String, Serializable> features = new HashMap<>();
        features.put(propertyKey, propertyValue);
        configManagementService.setPolicyConfiguration(policyId, ConfigManagementModel.newInstance(new HashMap<>(), new HashMap<>(), features));

        ConfigManagementPropertySetter.ConfigManagementPropertyFunction function =
                new ConfigManagementPropertySetter.ConfigManagementPropertyFunction(configManagementService);

        String result = function.apply(String.format("%s/%s", policyId, propertyKey));
        assertEquals(propertyValue, result);

    }

    @Test
    void testConfigManagmentPropertiesResult() {
        String policyId = "a/b/c/1.0.1";
        ConfigManagementService configManagementService = new ConfigManagementService();
        HashMap<String, Serializable> features = new HashMap<>();
        features.put("featureFoo", "exists");
        configManagementService.setPolicyConfiguration(policyId, ConfigManagementModel.newInstance(new HashMap<>(), new HashMap<>(), features));

        ConfigManagementPropertySetter.ConfigManagementPropertyFunction function =
                new ConfigManagementPropertySetter.ConfigManagementPropertyFunction(configManagementService);

        String propertyFoo = String.format("%s/%s", policyId, "featureFoo");
        String propertyBar = String.format("%s/%s", policyId, "featureBar");

        assertEquals("exists", function.apply(propertyFoo));
        assertThrows(IllegalArgumentException.class, () -> function.apply(propertyBar));

    }

    @Test
    void testConfigManagmentPropertiesMissingConfigModelResult() {
        String policyId = "a/b/c/1.0.1";
        ConfigManagementService configManagementService = new ConfigManagementService();
        ConfigManagementPropertySetter.ConfigManagementPropertyFunction function =
                new ConfigManagementPropertySetter.ConfigManagementPropertyFunction(configManagementService);

        String propertyFoo = String.format("%s/%s", policyId, "featureFoo");
        assertThrows(IllegalArgumentException.class, () -> function.apply(propertyFoo));

    }

}
