package com.capitalone.identity.identitybuilder.policycore.feature.property;

import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This component registers the class that handles property substitution for properties of
 * the format {{policy-config-feature:<<config management propertyName>>}}. This substitution
 * pattern only works with non-overridable feature-level properties within a policy config management
 * definition.
 */
@Component
public class ConfigManagementPropertySetter implements CamelContextConfiguration {
    static final String POLICY_CONFIG_FEATURE_PLACEHOLDER_NAME = "policy-config-feature";
    private static final Pattern inputPattern = Pattern.compile("(?<prefix>\\{\\{[\\s:!?]*policy-config-feature:)(?<feature>[\\s:!?\\-_.\\dA-Za-z]*[^/]}})");
    private static final Pattern namespacePattern = Pattern.compile("^(?<policy>.*\\d\\.\\d\\.\\d)/(?<property>.*)$");

    public static String getNamespacedPropertyTransformation(String input, String namespace) {
        if (input != null && inputPattern.matcher(input).find()) {
            return inputPattern.matcher(input).replaceAll("${prefix}" + namespace + "/${feature}");
        } else {
            return null;
        }
    }

    public static String extractPolicyId(String remainder) {
        Matcher matcher = namespacePattern.matcher(remainder);
        if (matcher.matches()) {
            return matcher.group("policy");
        } else {
            return null;
        }
    }

    public static String extractProperty(String remainder) {
        Matcher matcher = namespacePattern.matcher(remainder);
        if (matcher.matches()) {
            return matcher.group("property");
        } else {
            return null;
        }
    }

    private final ConfigManagementService configManagementService;

    public ConfigManagementPropertySetter(ConfigManagementService configManagementService) {
        this.configManagementService = configManagementService;
    }

    @Override
    public void beforeApplicationStart(CamelContext camelContext) {
        camelContext.getPropertiesComponent().addPropertiesFunction(
                new ConfigManagementPropertyFunction(configManagementService)
        );
    }

    @Override
    public void afterApplicationStart(CamelContext camelContext) {
        // unused
    }

    /**
     * The purpose of this class is to supply config-management non-overridable property values
     * for policies that want to reference the properties via property substitution with the signature
     * like {{policy-config-feature:propertyX}}. This class should be registered as a properties function
     * with the camel context properties component.
     */
    static class ConfigManagementPropertyFunction implements PropertiesFunction {
        private final ConfigManagementService configManagementService;

        ConfigManagementPropertyFunction(ConfigManagementService configManagementService) {
            this.configManagementService = Objects.requireNonNull(configManagementService);
        }

        @Override
        public String getName() {
            return POLICY_CONFIG_FEATURE_PLACEHOLDER_NAME;
        }

        @Override
        public String apply(String remainder) {
            final String policyId = ConfigManagementPropertySetter.extractPolicyId(remainder);
            final String policyFeatureProperty = ConfigManagementPropertySetter.extractProperty(remainder);
            return Optional.ofNullable(configManagementService.getPolicyConfiguration(policyId))
                    .flatMap(config -> Optional.ofNullable(config.getFeaturesMap()))
                    .flatMap(features -> Optional.ofNullable(features.get(policyFeatureProperty)))
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Missing feature-level config management " +
                            "property '%s' unable to resolve property placeholder referenced in policy " +
                            "address [remainder=%s]", policyFeatureProperty, remainder)))
                    .toString();
        }
    }
}
