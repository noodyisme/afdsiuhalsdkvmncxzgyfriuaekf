package com.capitalone.identity.identitybuilder.policycore.feature.rollout.camel;

import com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria.Criteria;
import com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria.GlobalOverride;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import lombok.extern.log4j.Log4j;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j
public class FeatureMatchProducer extends DefaultProducer {

    final FeatureMatchEndpoint endpoint;

    final String outputHeaderName;

    final GlobalOverride globalOverride;

    public FeatureMatchProducer(FeatureMatchEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        this.outputHeaderName = Objects.requireNonNull(endpoint.getOutputHeaderName());
        this.globalOverride = endpoint.getGlobalOverride();
    }

    @Override
    public void process(Exchange exchange) {
        if (globalOverride == GlobalOverride.ALWAYS_ALLOWED) {
            exchange.getIn().setHeader(outputHeaderName, true);
        } else if (globalOverride == GlobalOverride.DISABLED) {
            exchange.getIn().setHeader(outputHeaderName, false);
        } else {
            exchange.getIn().setHeader(outputHeaderName, calculateFeatureFlagResult(exchange));
        }
    }

    private boolean calculateFeatureFlagResult(Exchange exchange) {
        List<Map<String, String>> runtimeInputParameter = Optional.ofNullable(endpoint.getDataParameter())
                .flatMap(dataParam -> Optional.ofNullable(exchange.getIn().getHeader(dataParam, Object.class)))
                .map(runtimeInputDataParameter -> {
                    try {
                        return runtimeInputDataParameter instanceof List
                                ? (List<Map<String, String>>) runtimeInputDataParameter
                                : ImmutableList.of((Map<String, String>) runtimeInputDataParameter);
                    } catch (ClassCastException e) {
                        log.error("Error", e);
                        throw new IllegalArgumentException(String.format("Value of runtimeInputDataParameter " +
                                "[dataParameter=%s] must be a map or list of maps", outputHeaderName), e);
                    }
                })
                .orElse(Collections.emptyList());

        Stream<MatchArgumentHolder> runtimeCriteria = runtimeInputParameter.stream()
                .map(object -> {
                    if (object.containsKey("input")) {
                        String rawCriteria = Optional.ofNullable(object.get("criteria"))
                                .orElseThrow(() -> new IllegalArgumentException("criteria value required"));
                        Criteria criteria = endpoint.loadCriteria(rawCriteria);
                        return new MatchArgumentHolder(object.get("input"), criteria);
                    } else {
                        throw new IllegalArgumentException("input key required");
                    }
                });

        Stream<MatchArgumentHolder> loadTimeCriteria = endpoint.getQueryBasedMatchParameters()
                .stream().map(holder -> {
                    String resolvedArg = exchange.getIn().getHeader(holder.getInput(), String.class);
                    return new MatchArgumentHolder(resolvedArg, holder.getCriteria());
                });

        return evaluate(Streams.concat(runtimeCriteria, loadTimeCriteria).collect(Collectors.toList()));

    }

    private boolean evaluate(List<MatchArgumentHolder> inputs) {
        if (inputs.isEmpty()) {
            return false;
        }

        for (MatchArgumentHolder parameter : inputs) {
            if (parameter.getInput() == null || !parameter.getCriteria().isMatch(parameter.getInput())) {
                return false;
            }
        }
        return true;
    }

}

