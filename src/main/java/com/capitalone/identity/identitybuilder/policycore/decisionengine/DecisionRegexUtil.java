package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecisionRegexUtil {

    public static String findFirstGroupMatch(final Pattern pattern, final String input) {
        final Matcher m = pattern.matcher(input);
        if (m.find()) {
            return m.group(1);
        } else {
            throw new IllegalArgumentException(String.format("Failed to find first matching group from regex %s "
                    + "in input %s", pattern, input));
        }
    }

    private DecisionRegexUtil() {
    }

}
