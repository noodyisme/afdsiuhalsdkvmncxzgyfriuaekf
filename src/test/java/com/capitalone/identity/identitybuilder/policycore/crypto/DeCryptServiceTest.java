package com.capitalone.identity.identitybuilder.policycore.crypto;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {DeCryptService.class}, properties = "lockBoxId/productId/kid=test")
@ActiveProfiles("test")
class DeCryptServiceTest {

    @Autowired
    DeCryptService service;

    @SneakyThrows
    @Test
    void decryptTest() {
        // prepare
        COSRequestProperties config = COSRequestProperties.builder()
                .lockBoxId("lockBoxId")
                .productId("productId")
                .vaultAddress("vaultAddress")
                .vaultRole("vaultRole")
                .build();
        JWEHeader header = mock(JWEHeader.class);
        Payload payload = mock(Payload.class);
        when(header.getKeyID()).thenReturn("kid");
        try (MockedStatic<DecryptUtil> utility = mockStatic(DecryptUtil.class); MockedStatic<JWEObject> jwe =
                mockStatic(
                        JWEObject.class)) {
            jwe.when(() -> JWEObject.parse(any())).thenReturn(new JWEObject(header, payload));
            utility.when(() -> DecryptUtil.getDecode(any())).thenReturn("test".getBytes());
            utility.when(() -> DecryptUtil.isExpired(any())).thenReturn(false);
            utility.when(() -> DecryptUtil.parseData(anyString(), any())).thenReturn("data");
            // execute
            String test = service.decrypt("test", config);

            // test
            Assertions.assertEquals("data", test);
        }
    }

    @SneakyThrows
    @Test
    void decryptTestIFKeyExpired() {
        // prepare
        COSRequestProperties config = COSRequestProperties.builder()
                .lockBoxId("lockBoxId")
                .productId("productId")
                .vaultAddress("vaultAddress")
                .vaultRole("vaultRole")
                .build();
        JWEHeader header = mock(JWEHeader.class);
        Payload payload = mock(Payload.class);
        when(header.getKeyID()).thenReturn("kid");
        try (MockedStatic<DecryptUtil> utility = mockStatic(DecryptUtil.class); MockedStatic<JWEObject> jwe =
                mockStatic(
                        JWEObject.class)) {

            jwe.when(() -> JWEObject.parse(any())).thenReturn(new JWEObject(header, payload));
            utility.when(() -> DecryptUtil.getDecode(any())).thenReturn("test".getBytes());
            utility.when(() -> DecryptUtil.isExpired(any())).thenReturn(true);
            utility.when(() -> DecryptUtil.parseData(anyString(), any())).thenReturn("data");
            // execute and test
            assertThrows(ChassisBusinessException.class, () -> service.decrypt("test", config));

        }
    }

    @SneakyThrows
    @Test
    void decryptTestIFErrorThrown() {
        // prepare
        COSRequestProperties config = COSRequestProperties.builder()
                .lockBoxId("lockBoxId")
                .productId("productId")
                .vaultAddress("vaultAddress")
                .vaultRole("vaultRole")
                .build();
        JWEHeader header = mock(JWEHeader.class);
        Payload payload = mock(Payload.class);
        when(header.getKeyID()).thenReturn("kid");
        try (MockedStatic<DecryptUtil> utility = mockStatic(DecryptUtil.class); MockedStatic<JWEObject> jwe =
                mockStatic(
                        JWEObject.class)) {
            jwe.when(() -> JWEObject.parse(any())).thenReturn(new JWEObject(header, payload));
            utility.when(() -> DecryptUtil.getDecode(any())).thenReturn("test".getBytes());
            utility.when(() -> DecryptUtil.isExpired(any())).thenReturn(false);
            utility.when(() -> DecryptUtil.parseData(anyString(), any()))
                    .thenThrow(new JOSEException("Error parse data"));
            // execute and test
            assertThrows(ChassisBusinessException.class, () -> service.decrypt("test", config));

        }
    }

    @SneakyThrows
    @Test
    void decryptTest_If_JWEObjet_Error() {
        // prepare
        COSRequestProperties config = COSRequestProperties.builder()
                .lockBoxId("lockBoxId")
                .productId("productId")
                .vaultAddress("vaultAddress")
                .vaultRole("vaultRole")
                .build();
        try (MockedStatic<JWEObject> jwe = mockStatic(JWEObject.class)) {
            jwe.when(() -> JWEObject.parse(any())).thenThrow(new ParseException("Error Parsing data", 0));
            // execute and test
            assertThrows(ParseException.class, () -> service.decrypt("test", config));

        }
    }
}