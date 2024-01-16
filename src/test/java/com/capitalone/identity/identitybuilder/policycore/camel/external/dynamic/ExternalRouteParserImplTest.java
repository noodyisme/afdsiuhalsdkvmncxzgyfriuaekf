package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.policycore.camel.external.InvalidItemDefinitionException;
import lombok.Value;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalRouteParserImplTest {

    private static final String ROUTE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<routes xmlns=\"http://camel.apache.org/schema/spring\">\n" +
            "    <route id=\"direct:test\">\n" +
            "        <from uri=\"direct:test\"/>\n" +
            "        <to uri=\"direct:foo\"/>\n" +
            "    </route>\n" +
            "</routes>";

    private static final String ROUTE_WIRETAP = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<routes xmlns=\"http://camel.apache.org/schema/spring\">\n" +
            "    <route id=\"direct:test\">\n" +
            "        <from uri=\"direct:test\"/>\n" +
            "        <wireTap uri=\"direct:foo\"/>\n" +
            "    </route>\n" +
            "</routes>";

    private static final String ROUTE_TRANSFORMATION = "<routes xmlns=\"http://camel.apache.org/schema/spring\">\n" +
            "    <route id=\"direct:test\">\n" +
            "        <from uri=\"direct:test\" />\n" +
            "        <setHeader name=\"arg1\">\n" +
            "            <constant>\n" +
            "                testProperty    value aa 'testProperty'\n" +
            "           </constant>\n" +
            "        </setHeader>\n" +
            "        <to uri=\"direct:foo\" />\n" +
            "        <transform>\n" +
            "            <groovy>\n" +
            "                [\n" +
            "                testProperty : URLEncoder.encode(headers.testProperty)\n" +
            "                ]\n" +
            "            </groovy>\n" +
            "        </transform>\n" +
            "        <removeProperty propertyName=\"testProperty\"/>\n" +
            "        <multicast strategyRef=\"listStrategy\" parallelProcessing=\"true\" stopOnException=\"false\" stopOnAggregateException=\"true\">\n" +
            "            <to uri=\"direct:foo\"/>\n" +
            "            <to uri=\"direct:bar\" />\n" +
            "        </multicast>\n" +
            "    </route>\n" +
            "</routes>";

    private static final String ROUTE_BAD_XML = "<?xml.gov>";

    private static final String ROUTE_BAD_CAMEL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<hello xmlns=\"http://camel.apache.org/schema/spring\"></hello>";

    ExternalRouteParser routeParser;

    @BeforeEach
    void setUp() {
        DefaultCamelContext context = new DefaultCamelContext();
        routeParser = new ExternalRouteParserImpl(context);
    }

    @Test
    void parseRouteResource_pip() {
        ConfigStoreItemInfo pipInfo = new ConfigStoreItemInfo("a/b/c/pip.xml", "a");
        Entity.Pip pip = new Entity.Pip(
                new EntityInfo.Pip("a/b/c/pip.xml", "a/b/c/pip.xml", Collections.singleton(pipInfo)),
                Collections.singleton(new ConfigStoreItem(pipInfo, ROUTE)));

        assertNotNull(routeParser.parseRouteResource(pip));
    }

    @Test
    void parseRouteResource_unknownEntity() {
        Entity unknown = Mockito.mock(Entity.class);
        EntityInfo info = Mockito.mock(EntityInfo.class);
        when(unknown.getInfo()).thenReturn(info);
        when(info.getLocationPrefix()).thenReturn("test");
        assertNotNull(routeParser.parseRouteResource(unknown));
    }

    @Test
    void parseRouteResource_policy() {

        Entity.Policy policyEntity = Mockito.mock(Entity.Policy.class);
        EntityInfo.Policy policyInfo = Mockito.mock(EntityInfo.Policy.class);
        when(policyEntity.getInfo()).thenReturn(policyInfo);
        when(policyInfo.getLocationPrefix()).thenReturn("testlocation");
        when(policyInfo.getPolicyShortName()).thenReturn("a");

        Set<ConfigStoreItem> ruleItem = Collections.singleton(
                new ConfigStoreItem(new ConfigStoreItemInfo("pip", "a"), ROUTE)
        );
        when(policyEntity.getProcessItems()).thenReturn(ruleItem);

        assertNotNull(routeParser.parseRouteResource(policyEntity));

    }

    @Test
    void parseRouteResourceInvalidCompileVerison() {
        // arrange
        Entity.Policy policyEntity = Mockito.mock(Entity.Policy.class);
        when(policyEntity.getCompileVersion()).thenReturn(4);

        Set<ConfigStoreItem> ruleItem = Collections.singleton(
                new ConfigStoreItem(new ConfigStoreItemInfo("pip", "a"), ROUTE)
        );
        when(policyEntity.getProcessItems()).thenReturn(ruleItem);

        // assert
        assertThrows(InvalidItemDefinitionException.class, ()-> routeParser.parseRouteResource(policyEntity));

    }

    @Test
    void parseRouteResource_wireTap() {
        Entity.Policy policyEntity = Mockito.mock(Entity.Policy.class);
        EntityInfo.Policy policyInfo = Mockito.mock(EntityInfo.Policy.class);
        String expectedRoute = "[Route(direct:test[ns=a_null])[From[direct:test|a_null] -> [WireTap[direct:foo]]]]";
        when(policyEntity.getInfo()).thenReturn(policyInfo);
        when(policyInfo.getLocationPrefix()).thenReturn("testlocation");
        when(policyInfo.getPolicyShortName()).thenReturn("a");

        Set<ConfigStoreItem> ruleItem = Collections.singleton(
                new ConfigStoreItem(new ConfigStoreItemInfo("pip", "a"), ROUTE_WIRETAP)
        );
        when(policyEntity.getProcessItems()).thenReturn(ruleItem);

        assertEquals(expectedRoute, routeParser.parseRouteResource(policyEntity).getRoutes().stream().collect(Collectors.toList()).toString());
    }

    @Test
    void testRouteTransformation() {
        //arrange
        Entity.Policy policyEntity = Mockito.mock(Entity.Policy.class);
        EntityInfo.Policy policyInfo = Mockito.mock(EntityInfo.Policy.class);
        when(policyEntity.getInfo()).thenReturn(policyInfo);
        when(policyInfo.getLocationPrefix()).thenReturn("testlocation");
        when(policyInfo.getPolicyShortName()).thenReturn("a");

        Set<ConfigStoreItem> ruleItem = Collections.singleton(
                new ConfigStoreItem(new ConfigStoreItemInfo("pip", "a"), ROUTE_TRANSFORMATION)
        );
        when(policyEntity.getProcessItems()).thenReturn(ruleItem);

        //assert fails without content transformation
        when(policyEntity.getCompileVersion()).thenReturn(3);
        assertThrows(InvalidItemDefinitionException.class, () -> routeParser.parseRouteResource(policyEntity));

        //assert success with content transformation
        when(policyEntity.getCompileVersion()).thenReturn(2);
        assertDoesNotThrow(() -> routeParser.parseRouteResource(policyEntity));
    }

    @ParameterizedTest
    @ValueSource(strings = {ROUTE_BAD_XML, ROUTE_BAD_CAMEL})
    void parseRouteResource_fail_bad_camel(String content) {
        Entity.Policy policyEntity = Mockito.mock(Entity.Policy.class);

        when(policyEntity.getProcessItems()).thenReturn(Collections.singleton(
                new ConfigStoreItem(new ConfigStoreItemInfo("pip", "a"), content)
        ));

        assertThrows(InvalidItemDefinitionException.class, () -> routeParser.parseRouteResource(policyEntity));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "policy:policy_a_1.0",
            "policy:policy_a_1.1",
            "policy:policy_a_2.1",
            "policy:policy_a_1.0-step1",
            "policy:policy_a_1.1-step1",
            "policy:policy_a_2.1-step1",
            "direct:policy_a_1.0",
            "direct:policy_a_1.1",
            "direct:policy_a_2.1",
            "direct:policy_a_1.0-step1",
            "direct:policy_a_1.1-step1",
            "direct:policy_a_2.1-step1",
            "direct:policy_a_1.0-schema",
            "direct:policy_a_1.1-schema",
            "direct:policy_a_2.1-schema",
            "direct:policy_a_1.0-step1-schema",
            "direct:policy_a_1.1-step1-schema",
            "direct:policy_a_2.1-step1-schema",
    })
        /*
         */
    void testRouteNamespacing_replaceExpected(String value) {
        // These uris are "public" entry-points to "policy_a". They are specified only to minor
        // version. The transformed uri value should be a basic replacement of the minor version
        // with the policy patch version. The simple replacement allows public entry-points to
        // be addressable with knowledge of a policy patch version.
        PolicyInfo.Patch policyInfo = new TestPolicyInfo("policy_a", 1, 4, 2);
        String actual = ExternalRouteParserImpl.getNamespacedUri(value, policyInfo, Collections.singleton(value));
        String expected = value.replaceAll("\\d+.\\d+", "1.4.2");
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:testFile_1.0",
            "P2P:resources/policytopolicy/policy_a/1.0",
            "policy-configuration:policy_a_1.0",
            "direct:testUri_1.0",
            "direct:testUri_1.1",
            "direct:policy_a_b_1.0",
            "direct:a_policy_a_1.0",
            "policy:policy_a_b_1.0",
            "policy:a_policy_a_1.0",
            "policy:policy_a_b_1.0-test",
            "policy:a_policy_a_1.0-test",
            "other:policy_a_1.0",
            "policy:policy_a_4.5.6",
            "direct:policy_a_4.5.6",
    })
    void testRouteNamespacing_noChangeExpected(String original) {
        // These URI patterns don't match a 'public' policy uri, and there are no
        // corresponding 'from' consuming uris, so they are calling "policy-external"
        // component or route and should not be modified
        Set<String> consumingUris = Collections.emptySet();
        PolicyInfo.Patch policyInfo = new TestPolicyInfo("policy_a", 1, 0, 0);
        String namespacedUri = ExternalRouteParserImpl.getNamespacedUri(original, policyInfo, consumingUris);
        assertEquals(original, namespacedUri);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "direct:testUri_1.0",
            "direct:testUri_1.1",
            "direct:policy_a_b_1.0",
            "direct:a_policy_a_1.0",
            "policy:policy_a_b_1.0",
            "policy:a_policy_a_1.0",
            "policy:policy_a_b_1.0-test",
            "policy:a_policy_a_1.0-test",
            "other:policy_a_1.0",
            "policy:policy_a_4.5.6",
            "direct:policy_a_4.5.6",
    })
    void testRouteNamespacing_appendExpected(String value) {

        // These uris are not public, but they are in the list of consuming apis in the policy, so
        // they should be namespaced with policy patch information.
        Set<String> consumingUris = Collections.singleton(value);
        PolicyInfo.Patch policyInfo = new TestPolicyInfo("policy_a", 1, 0, 0);
        String actual = ExternalRouteParserImpl.getNamespacedUri(value, policyInfo, consumingUris);
        String newPolicyString = ExternalRouteParserImpl.getNewPolicyString(policyInfo);
        String expected = value + "|" + newPolicyString;
        assertEquals(expected, actual);
    }

    @Value
    private static class TestPolicyInfo implements PolicyInfo.Patch {

        String name;
        int majorVersion;
        int minorVersion;
        int patchVersion;

        @Override
        public int getPolicyMinorVersion() {
            return minorVersion;
        }

        @Override
        public int getPolicyPatchVersion() {
            return patchVersion;
        }

        @Override
        public int getPolicyMajorVersion() {
            return majorVersion;
        }

        @Override
        public String getPolicyShortName() {
            return name;
        }

        @Override
        public String getPolicyFullName() {
            return name;
        }
    }

}
