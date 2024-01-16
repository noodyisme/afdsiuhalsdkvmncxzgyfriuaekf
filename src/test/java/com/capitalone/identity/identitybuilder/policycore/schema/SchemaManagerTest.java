package com.capitalone.identity.identitybuilder.policycore.schema;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.chassis.engine.model.exception.NotFoundException;
import com.capitalone.identity.identitybuilder.policycore.camel.util.PathMap;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.ValidationMessage;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.camel.test.junit5.TestSupport.assertCollectionSize;
import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@CamelSpringBootContextAwareTest(
		contextConfigClasses = SchemaManagerTest.ContextConfig.class,
		excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
				pattern = "com.capitalone.identity.identitybuilder.policycore.schema"))
public class SchemaManagerTest {

	@Autowired
	private CamelContext camelContext;

	private static final String GOOD_SCHEMA = "{\n"
			+ "  \"$schema\": \"http://json-schema.org/draft/2019-09/schema#\",\n" + "  \"type\": \"object\",\n"
			+ "  \"properties\": {\n" + "    \"date\": { \"type\": \"string\", \"format\": \"date\" },\n"
			+ "    \"boolean\": { \"type\": \"boolean\" },\n" + "    \"number\": { \"type\": \"integer\" },\n"
			+ "    \"optionalString\": { \"type\": \"string\" },\n" + "    \"level2\": {\n"
			+ "      \"type\": \"object\",\n" + "      \"properties\": {\n"
			+ "        \"number\": { \"type\": \"number\" }\n" + "      },\n" + "      \"required\": [ \"number\" ]\n"
			+ "    }\n" + "  },\n" + "  \"required\": [ \"date\", \"boolean\", \"integer\" ]\n" + "}";
	private static final String BAD_SCHEMA = "{ \"foo\": \"bar\", }";

	private static final String GOOD_ROUTE = "good_route";
	private static final String BAD_ROUTE = "bad_route";
	private static final String EXCEPTION_ROUTE = "exception_route";
	private static final String WRAPPED_EXCEPTION_ROUTE = "wrapped_exception_route";

	private SchemaManager manager;
	private PathMap request;

    @BeforeEach
    public void setup() {
        camelContext.start();
        manager = new SchemaManager(camelContext);

        request = new PathMap(new HashMap<>());
        request.put("date", "2020-11-12");
        request.put("boolean", true);
        request.put("integer", 42);
        request.put("optionalString", "text");
        request.put("level2.number", 42.5);
    }

	@Test
	public void defaultConstructor() {
		assertNotNull(new SchemaManager());
	}

	@Test
	public void missingSchemaRoute() {
		assertThrows(NotFoundException.class, () -> manager.getEntry("notThere"));
	}

	@Test
	public void illegalSchemaReturned() {
		assertThrows(JsonSchemaException.class, () ->manager.getEntry(BAD_ROUTE));
	}

	@Test
	public void schemaRouteThrowsChassisException() {
		assertThrows(ChassisBusinessException.class, () -> manager.getEntry(EXCEPTION_ROUTE));
	}

	@Test
	public void schemaRouteThrowsWrappedException() {
		assertThrows(ChassisBusinessException.class, () -> manager.getEntry(WRAPPED_EXCEPTION_ROUTE));
	}

	@Test
	public void validSchemaScanWithCaching() {
		SchemaManager.Entry entry = manager.getEntry(GOOD_ROUTE);
		assertEquals(GOOD_ROUTE, entry.getName());
		assertEquals(GOOD_SCHEMA, entry.getSource());
		assertNotNull(entry.getSchema());

		// A repeated call gets a cached object.
		SchemaManager.Entry entry2 = manager.getEntry(GOOD_ROUTE);
		assertSame(entry, entry2);
		assertSame(entry.getName(), entry2.getName());
		assertSame(entry.getSource(), entry2.getSource());
		assertSame(entry.getSchema(), entry2.getSchema());

		// After a purge, a repeated call should get a new cached object.
		manager.purgeCache();
		SchemaManager.Entry entry3 = manager.getEntry(GOOD_ROUTE);
		assertNotSame(entry, entry3);
		assertSame(entry.getName(), entry3.getName());
		assertSame(entry.getSource(), entry3.getSource());
		assertNotSame(entry.getSchema(), entry3.getSchema());
	}

	@Test
	public void validateNoErrors() {
		Set<ValidationMessage> msgs = manager.getEntry(GOOD_ROUTE).getSchema().validate(mapToNode(request));
		assertCollectionSize(msgs, 0);
	}

	@Test
	public void validateInvalidFormat() {
		request.put("date", "2020-11");
		Set<ValidationMessage> msgs = manager.validate(GOOD_ROUTE, mapToNode(request));
		assertEquals("[$.date: 2020-11 is an invalid date]", msgs.toString());
	}

	@Test
	public void validateInvalidNumber() {
		request.put("number", true);
		Set<ValidationMessage> msgs = manager.validate(GOOD_ROUTE, mapToNode(request));
		assertEquals("[$.number: boolean found, integer expected]", msgs.toString());
	}

	@Test
	public void validateInvalidBoolean() {
		request.put("boolean", "true");
		Set<ValidationMessage> msgs = manager.validate(GOOD_ROUTE, mapToNode(request));
		assertEquals("[$.boolean: string found, boolean expected]", msgs.toString());
	}

	@Test
	public void validateMultipleErrors() {
		request.put("number", true);
		request.put("boolean", "true");
		Set<ValidationMessage> msgs = manager.validate(GOOD_ROUTE, mapToNode(request));
		assertEquals("[$.number: boolean found, integer expected, $.boolean: string found, boolean expected]",
				msgs.toString());
	}

	@Test
	public void validateMissingRequired() {
		request.remove("date");
		Set<ValidationMessage> msgs = manager.validate(GOOD_ROUTE, mapToNode(request));
		assertEquals("[$.date: is missing but it is required]", msgs.toString());
	}

	@Test
	public void validateMissingOptional() {
		request.remove("optionalString");
		Set<ValidationMessage> msgs = manager.validate(GOOD_ROUTE, mapToNode(request));
		assertCollectionSize(msgs, 0);
	}

	@Test
	public void validateInvalidSecondLevel() {
		request.put("level2.number", "42.5");
		Set<ValidationMessage> msgs = manager.validate(GOOD_ROUTE, mapToNode(request));
		assertEquals("[$.level2.number: string found, number expected]", msgs.toString());
	}

	/**
	 * Deeply converts a map of objects to a JsonNode tree representation.
	 *
	 * @param map the object map to convert
	 * @return the input map represented as a JsonNode tree
	 */
	private JsonNode mapToNode(Map<String, Object> map) {
		return new ObjectMapper().convertValue(map, JsonNode.class);
	}

    @Configuration
    public static class ContextConfig {

        /**
         * Defines routes that the schema manager can invoke.
         */

        @Bean
        protected CamelContext camelContext() throws Exception {
            CamelContext camelContext = new DefaultCamelContext();
            camelContext.addRoutes(route());
            return camelContext;
        }


        public RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:" + GOOD_ROUTE + "-schema").setBody().constant(GOOD_SCHEMA);
                    from("direct:" + BAD_ROUTE + "-schema").setBody().constant(BAD_SCHEMA);
                    from("direct:" + EXCEPTION_ROUTE + "-schema").throwException(new ChassisBusinessException());
                    from("direct:" + WRAPPED_EXCEPTION_ROUTE + "-schema").throwException(new IllegalStateException());
                }
            };
        }
    }
}
