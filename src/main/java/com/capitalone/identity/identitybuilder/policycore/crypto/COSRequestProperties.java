package com.capitalone.identity.identitybuilder.policycore.crypto;

import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class COSRequestProperties {
    String productId;
    String vaultRole;
    String lockBoxId;
    String vaultAddress;
}
