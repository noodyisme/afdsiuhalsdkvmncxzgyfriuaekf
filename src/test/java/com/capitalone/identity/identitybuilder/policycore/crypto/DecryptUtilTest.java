package com.capitalone.identity.identitybuilder.policycore.crypto;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DecryptUtilTest {

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<DecryptUtil> constructor = DecryptUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Assertions.assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void getDecodeTest() {
        byte[] tests = DecryptUtil.getDecode("test");
        assertNotNull(tests);
    }


    @Test
    void getDecodeTest_NullData() {
        assertThrows(NullPointerException.class, () -> DecryptUtil.getDecode(null));
    }

    @Test
    void isExpiredTest() {
        boolean expired = DecryptUtil.isExpired("{\"exp\":  \"1661312826\"}");
        assertTrue(expired);
    }

    @Test
    void isExpiredTest_InvalidData() {
        assertThrows(ChassisBusinessException.class, () -> DecryptUtil.isExpired("test"));
    }

    @Test
    void isExpiredTest_NoExpData() {
        boolean expired = DecryptUtil.isExpired("{\"test\":  \"1661312826\"}");
        assertTrue(expired);
    }
}