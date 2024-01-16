package com.capitalone.identity.identitybuilder.policycore.camel.util;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class RawMapSerializer extends JsonSerializer<Map<String, Object>> {

    @Override
    public void serialize(Map<String, Object> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        for (Map.Entry<String, Object> e : value.entrySet()) {
            gen.writeFieldName(e.getKey());
            // Write value as raw data, since it's already JSON text
            Object o = e.getValue();
            if (o instanceof String) {
                gen.writeRawValue("\"" + o + "\"");
            } else {
                gen.writeObject(o);
            }
        }
        gen.writeEndObject();
    }
}

/*
 * Copyright 2018 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */