package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

import java.util.Arrays;
import java.util.Objects;

public enum PreformattedRegex {

    PERCENT_ROLLOUT_5("5-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$)"),
    PERCENT_ROLLOUT_10("10-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5])"),
    PERCENT_ROLLOUT_15("15-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$)"),
    PERCENT_ROLLOUT_20("20-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*4[0-9]$|.*a0$)"),
    PERCENT_ROLLOUT_25("25-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*4[0-9]$|.*a0$|.*a[1-9]$|.*b[0-3]$)"),
    PERCENT_ROLLOUT_30("30-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*4[0-9]$|.*a0$|.*a[1-9]$|.*b[0-3]$|.*b[4-9]$|.*c[0-6]$)"),
    PERCENT_ROLLOUT_35("35-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*4[0-9]$|.*a0$|.*a[1-9]$|.*b[0-3]$|.*b[4-9]$|.*c[0-6]$|.*c[7-9]$|.*d[0-9]$)"),
    PERCENT_ROLLOUT_40("40-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*4[0-9]$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*b[0-3]$|.*b[4-9]$|.*c[0-6]$|.*c[7-9]$|.*d[0-9]$|.*e[0-9]$)"),
    PERCENT_ROLLOUT_45("45-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*4[0-9]$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*e[0-9]$)"),
    PERCENT_ROLLOUT_50("50-percentRollout", "(.*0[0-2]$|.*0[3-9]$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*4[0-9]$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_55("55-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-9]a$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*4[0-9]$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_60("60-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-5]c$|.*[0-9]a$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*[3-9]b$|.*4[0-9]$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_65("65-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-5]c$|.*[0-7]d$|.*[0-9]a$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*[3-9]b$|.*4[0-9]$|.*[6-9]c$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_70("70-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-5]c$|.*[0-7]d$|.*[0-9]a$|.*[0-9]e$|.*0f$|.*1[0-2]$|.*1[3-9]$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*[3-9]b$|.*4[0-9]$|.*[6-9]c$|.*[8-9]d$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_75("75-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-5]c$|.*[0-7]d$|.*[0-9]a$|.*[0-9]e$|.*0f$|.*1[0-2]$|.*1[3-9]$|.*[1-9]f$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*[3-9]b$|.*4[0-9]$|.*5[0-3]$|.*[6-9]c$|.*[8-9]d$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_80("80-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-5]c$|.*[0-7]d$|.*[0-9]a$|.*[0-9]e$|.*0f$|.*1[0-2]$|.*1[3-9]$|.*[1-9]f$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*[3-9]b$|.*4[0-9]$|.*5[0-3]$|.*5[4-9]$|.*6[0-6]$|.*[6-9]c$|.*[8-9]d$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_85("85-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-5]c$|.*[0-7]d$|.*[0-9]a$|.*[0-9]e$|.*0f$|.*1[0-2]$|.*1[3-9]$|.*[1-9]f$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*[3-9]b$|.*4[0-9]$|.*5[0-3]$|.*5[4-9]$|.*6[0-6]$|.*6[7-9]$|.*[6-9]c$|.*7[0-9]$|.*[8-9]d$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_90("90-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-5]c$|.*[0-7]d$|.*[0-9]a$|.*[0-9]e$|.*0f$|.*1[0-2]$|.*1[3-9]$|.*[1-9]f$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*[3-9]b$|.*4[0-9]$|.*5[0-3]$|.*5[4-9]$|.*6[0-6]$|.*6[7-9]$|.*[6-9]c$|.*7[0-9]$|.*8[0-9]$|.*[8-9]d$|.*9[0-1]$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[a-e]$)"),
    PERCENT_ROLLOUT_95("95-percentRollout", "(.*0[0-2]$|.*[0-2]b$|.*0[3-9]$|.*[0-5]c$|.*[0-7]d$|.*[0-9]a$|.*[0-9]e$|.*0f$|.*1[0-2]$|.*1[3-9]$|.*[1-9]f$|.*2[0-5]|.*2[6-9]$|.*3[0-8]$|.*39$|.*[3-9]b$|.*4[0-9]$|.*5[0-3]$|.*5[4-9]$|.*6[0-6]$|.*6[7-9]$|.*[6-9]c$|.*7[0-9]$|.*8[0-9]$|.*[8-9]d$|.*9[0-1]$|.*9[2-9]$|.*a0$|.*a[1-9]$|.*a[d-e]$|.*af$|.*b[0-3]$|.*b[4-9]$|.*ba$|.*b[c-f]$|.*c[0-6]$|.*c[7-9]$|.*c[a-b]$|.*c[d-f]$|.*d[0-9]$|.*d[a-b]$|.*dc$|.*de$|.*df$|.*e[0-9]$|.*e[a-d]$|.*ef$|.*f[0-4]$|.*f[a-e]$)"),
    MATCH_ALL("match-all",".*");
    final String label;
    final String regex;

    PreformattedRegex(String label, String pattern) {
        regex = Objects.requireNonNull(pattern);
        this.label = Objects.requireNonNull(label);
    }

    static PreformattedRegex valueOfLabel(String pattern) {
        return Arrays.stream(PreformattedRegex.values())
                .filter(preformatted->preformatted.label.equals(pattern))
                .findFirst().orElse(null);
    }

}
