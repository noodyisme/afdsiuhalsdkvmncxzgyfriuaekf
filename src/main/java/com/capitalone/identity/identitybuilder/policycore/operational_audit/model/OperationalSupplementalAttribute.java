package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Leveraged for auditing any supplemental attributes not defined in the schema
 * WARNING: ENSURE SUPPLEMENTAL ATTRIBUTES BEING AUDITED DO NOT INCLUDE ANY CUSTOMER RELATED INFO AND/OR NPI/PII
 */
@Value
@AllArgsConstructor
public class OperationalSupplementalAttribute {
    String key;
    String value;
}
