package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.CamelDataModel;
import org.apache.commons.compress.utils.Lists;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.*;

public class MockModel implements CamelDataModel {

    public static MockModel pip() {
        return new MockModel(EntityType.PIP);
    }

    public static MockModel policy() {
        return new MockModel(EntityType.POLICY);
    }

    public static MockModel internal() {
        return new MockModel(null);
    }

    public static MockModel ofType(@Nullable EntityType type) {
        return new MockModel(type);
    }

    final List<String> routeIds;
    final List<String> inboundUris;
    final List<String> outboundUris;
    final String name;
    final EntityType type;
    final Optional<String> childPolicyUri;

    public MockModel() {
        this(null);
    }

    private MockModel(EntityType type) {
        this(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), UUID.randomUUID().toString(),
                type, Optional.empty());
    }

    MockModel(List<String> routeIds, List<String> inboundUris, List<String> outboundUris, String name, EntityType type,
              Optional<String> childPolicyUri) {
        this.routeIds = routeIds;
        this.inboundUris = inboundUris;
        this.outboundUris = outboundUris;
        this.name = name;
        this.type = type;
        this.childPolicyUri = childPolicyUri;
    }

    public MockModel withLinksFrom(String... fromUris) {
        return new MockModel(
                Lists.newArrayList(routeIds.iterator()),
                Arrays.asList(fromUris),
                Lists.newArrayList(outboundUris.iterator()),
                name, type, childPolicyUri);
    }

    public MockModel withLinksTo(String... toUris) {
        return new MockModel(
                Lists.newArrayList(routeIds.iterator()),
                Lists.newArrayList(inboundUris.iterator()),
                Arrays.asList(toUris),
                name, type, childPolicyUri);
    }

    public MockModel withLinksFromTo(List<String> fromUris, List<String> toUris) {
        return this.withLinksFrom(fromUris.toArray(new String[0]))
                .withLinksTo(toUris.toArray(new String[0]));
    }

    public MockModel withRouteIds(String... ids) {
        return new MockModel(
                Arrays.asList(ids),
                Lists.newArrayList(inboundUris.iterator()),
                Lists.newArrayList(outboundUris.iterator()),
                name, type, childPolicyUri);
    }

    public MockModel withName(String newName) {
        return new MockModel(
                Lists.newArrayList(routeIds.iterator()),
                Lists.newArrayList(inboundUris.iterator()),
                Lists.newArrayList(outboundUris.iterator()),
                newName, type, childPolicyUri);
    }

    public MockModel withType(EntityType type) {
        return new MockModel(
                Lists.newArrayList(routeIds.iterator()),
                Lists.newArrayList(inboundUris.iterator()),
                Lists.newArrayList(outboundUris.iterator()),
                name, type, childPolicyUri);
    }

    public MockModel withChildPolicyUri(String childPolicyUri) {
        return new MockModel(
                Lists.newArrayList(routeIds.iterator()),
                Lists.newArrayList(inboundUris.iterator()),
                Lists.newArrayList(outboundUris.iterator()),
                name, type, Optional.ofNullable(childPolicyUri));
    }

    @Override
    public List<String> getRouteIds() {
        return routeIds;
    }

    @Override
    public List<String> getInboundUris() {
        return inboundUris;
    }

    @Override
    public List<String> getOutboundUris() {
        return outboundUris;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public EntityType getType() {
        return type;
    }

    @Override
    public Optional<String> getChildPolicyUri() {
        return childPolicyUri;
    }
}
