package com.capitalone.identity.identitybuilder.policycore.camel.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestTypeConverterTest {

	@Test
	public void testToStringArray() {
		assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(TestTypeConverters.toStringArray("a,b,c")));
		assertEquals(Arrays.asList("aa ab", "b", "cccc"), Arrays.asList(TestTypeConverters.toStringArray(" aa ab , b , cccc ")));
	}

	@Test
	public void testPrivateConstructor() throws Throwable {
		Constructor<TestTypeConverters> c = TestTypeConverters.class.getDeclaredConstructor();
		c.setAccessible(true);
		InvocationTargetException invocationTargetException = assertThrows(InvocationTargetException.class, c::newInstance);
		assertEquals(IllegalStateException.class, invocationTargetException.getTargetException().getClass());
	}
}
