package com.capitalone.identity.identitybuilder.policycore.feature.rollout.camel;


import com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria.Criteria;
import com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria.CriteriaParser;
import com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria.GlobalOverride;
import com.google.common.collect.Streams;
import lombok.EqualsAndHashCode;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Examples:
 * <to uri="feature-match:match?outputHeaderName=headerNameY" />
 * <to uri="feature-match:match?dataParameter=headerX&outputHeaderName=headerNameY&globalOverride=ALWAYS_ALLOWED/>
 * <to uri="feature-match:match?dataParameter=headerX&outputHeaderName=headerNameY&globalOverride=DISABLED/>
 * <to uri="feature-match:match?dataParameter=headerX&outputHeaderName=headerNameY&globalOverride=DEFAULT/>
 * <to uri="feature-match:match?inputSrc=headerX&criteria=regex::^(?!1\.35$|1\.36$).*&outputHeaderName=headerNameY"/>
 **/
@UriEndpoint(
        firstVersion = "01.24.SNAPSHOT",
        scheme = "feature-match",
        title = "FeatureMatchEndpoint",
        syntax = "feature-match:match")
@EqualsAndHashCode(callSuper = true)
public class FeatureMatchEndpoint extends DefaultEndpoint {

    private static final String NO_CONSUMER_SUPPORT_ERROR_MESSAGE = "FeatureMatchComponent does not support consumers";

    private static final String INVALID_ARGUMENTS_INPUT_PARAM_COUNT_PARAM = "Invalid uri '%s' - count of 'inputSrc' params (%s) does not match count of 'criteria' params (%s)";

    @UriParam
    private GlobalOverride globalOverride;

    @UriParam
    private String dataParameter;

    @UriParam
    private String outputHeaderName;

    @UriParam
    private List<String> inputSrcHeaders;

    @UriParam
    private List<Criteria> rawCriteria;

    private final Map<String, Criteria> criteriaCache;

    private List<MatchArgumentHolder> queryBasedMatchParameters = Collections.emptyList();

    public FeatureMatchEndpoint(String uri, FeatureMatchComponent component, Map<String, Criteria> criteriaCache) {
        super(uri, component);
        this.criteriaCache = criteriaCache;
    }

    void setQueryBasedMatchParameters() {
        if (getInputSrc().size() == getCriteria().size()) {
            queryBasedMatchParameters = Streams
                    .zip(getInputSrc().stream(), getCriteria().stream(), MatchArgumentHolder::new)
                    .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException(String.format(INVALID_ARGUMENTS_INPUT_PARAM_COUNT_PARAM,
                    getEndpointUri(), getInputSrc().size(), getCriteria().size()));
        }
    }

    @NotNull
    Criteria loadCriteria(String rawCriteria) {
        return Optional.ofNullable(criteriaCache.get(rawCriteria)).orElseGet(() -> {
            Criteria parsed = CriteriaParser.parse(rawCriteria);
            criteriaCache.put(rawCriteria, parsed);
            return parsed;
        });
    }

    public List<MatchArgumentHolder> getQueryBasedMatchParameters() {
        return queryBasedMatchParameters;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new FeatureMatchProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException(NO_CONSUMER_SUPPORT_ERROR_MESSAGE);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public List<String> getInputSrc() {
        return inputSrcHeaders != null ? inputSrcHeaders : Collections.emptyList();
    }

    @ManagedAttribute
    public void setInputSrc(String header) {
        setInputSrc(Collections.singletonList(header));
    }

    @ManagedAttribute
    public void setInputSrc(List<String> headers) {
        headers.forEach(header -> {
            if (Strings.isBlank(header)) {
                throw new IllegalArgumentException("Blank inputSrc header not allowed");
            }
        });
        inputSrcHeaders = headers;
    }

    public List<Criteria> getCriteria() {
        return rawCriteria != null ? rawCriteria : Collections.emptyList();
    }

    @ManagedAttribute
    public void setCriteria(String rawCriteria) {
        setCriteria(Collections.singletonList(rawCriteria));
    }

    @ManagedAttribute
    public void setCriteria(List<String> rawCriteria) {
        this.rawCriteria = rawCriteria.stream().map(this::loadCriteria).collect(Collectors.toList());
    }

    public GlobalOverride getGlobalOverride() {
        return Optional.ofNullable(globalOverride).orElse(GlobalOverride.DEFAULT);
    }

    @ManagedAttribute
    public void setGlobalOverride(GlobalOverride globalOverride) {
        this.globalOverride = Objects.requireNonNull(globalOverride);
    }

    public String getDataParameter() {
        return dataParameter;
    }

    @ManagedAttribute
    public void setDataParameter(String dataParameter) {
        if (Strings.isBlank(dataParameter)) {
            throw new IllegalArgumentException("Blank 'dataParameter' not allowed");
        }
        this.dataParameter = dataParameter;
    }

    public String getOutputHeaderName() {
        return outputHeaderName;
    }

    @ManagedAttribute
    public void setOutputHeaderName(String outputHeaderName) {
        if (Strings.isBlank(outputHeaderName)) {
            throw new IllegalArgumentException("Blank 'outputHeaderName' not allowed");
        }
        this.outputHeaderName = outputHeaderName;
    }

}

