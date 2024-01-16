package com.capitalone.identity.identitybuilder.policycore.crypto;

import com.capitalone.identity.identitybuilder.policycore.model.ExecutePolicyRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ExecutePolicyRequest.class)
class COSRequestPropertiesTest {

    @Test
    void testSettersAndGetters() {
        COSRequestProperties config = COSRequestProperties.builder()
                .lockBoxId("lockBoxId")
                .productId("productId")
                .vaultRole("vaultRole")
                .vaultAddress("vaultAddress")
                .build();
        Assertions.assertEquals("lockBoxId", config.getLockBoxId());
        Assertions.assertEquals("productId", config.getProductId());
        Assertions.assertEquals("vaultRole", config.getVaultRole());
        Assertions.assertEquals("vaultAddress", config.getVaultAddress());
    }
}