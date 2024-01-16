package com.capitalone.identity.identitybuilder.policycore.camel.external;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ItemUtils {

    private ItemUtils() {
    }

    /**
     * Reads input stream to text and closes it.
     */
    static String asText(InputStream stream) throws IOException {
        try (InputStream is = stream) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    public static InputStream asStream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

}
