package com.capitalone.identity.identitybuilder.policycore.feature.rollout.camel;

import com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria.Criteria;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor
public class MatchArgumentHolder {

    String input;

    @NonNull
    Criteria criteria;

}
