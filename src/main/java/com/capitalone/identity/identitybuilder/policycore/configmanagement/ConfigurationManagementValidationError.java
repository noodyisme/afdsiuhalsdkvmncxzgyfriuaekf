package com.capitalone.identity.identitybuilder.policycore.configmanagement;

public class ConfigurationManagementValidationError extends RuntimeException {
    public ConfigurationManagementValidationError(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationManagementValidationError(String message) {
        super(message);
    }


    public static class SchemaError extends ConfigurationManagementValidationError {
        public SchemaError(String policyId, Throwable cause) {
            super(String.format("Exception with content of 'schema.json' for policy id '%s'", policyId), cause);
        }

        public SchemaError(String message) {
            super(message);
        }
    }
}

