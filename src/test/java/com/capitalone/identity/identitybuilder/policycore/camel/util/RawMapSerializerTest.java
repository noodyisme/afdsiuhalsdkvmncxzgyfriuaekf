package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RawMapSerializerTest {

	@Test
	public void testSerialize() throws JsonProcessingException, IOException {
		StringWriter writer = new StringWriter();
	    JsonGenerator jsonGenerator = new JsonFactory().createGenerator(writer);
	    SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
		
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("count", 3);
		
		new RawMapSerializer().serialize(map, jsonGenerator, serializerProvider);
		jsonGenerator.flush();
		assertEquals("{\"foo\":\"bar\",\"count\":3}", writer.toString());
	}
}
