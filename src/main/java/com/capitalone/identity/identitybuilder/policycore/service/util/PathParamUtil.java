package com.capitalone.identity.identitybuilder.policycore.service.util;

import java.util.regex.Pattern;

public class PathParamUtil {

    private static final Pattern cleanParameterPattern = Pattern.compile("[\n|\r|\t]");

    /**
     * SonarQube reminds us that we shouldn't be using the path parameters
     * without cleansing them of log file pattern-breaking characters.
     *
     * @param param the raw parameter value
     * @return the cleansed version
     */
    public static String cleanParameter(String param) {
        if (param != null) {
            return cleanParameterPattern.matcher(param).replaceAll("_");
        } else {
            return null;
        }
    }

    private PathParamUtil() {
    }
}
