package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

import com.google.re2j.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PreformattedCriteriaTest {

    @ParameterizedTest
    @EnumSource(PreformattedRegex.class)
    void newPattern(PreformattedRegex criteria) {
        Pattern pattern = assertDoesNotThrow(() -> Pattern.compile(criteria.regex));
        assertTrue(pattern.matcher("01").matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "5-percentRollout",
            "10-percentRollout",
            "15-percentRollout",
            "20-percentRollout",
            "25-percentRollout",
            "30-percentRollout",
            "35-percentRollout",
            "40-percentRollout",
            "45-percentRollout",
            "50-percentRollout",
            "55-percentRollout",
            "60-percentRollout",
            "65-percentRollout",
            "70-percentRollout",
            "75-percentRollout",
            "80-percentRollout",
            "85-percentRollout",
            "90-percentRollout",
            "95-percentRollout",
            "match-all",
    })
    void checkPreformatted(String input) {
        assertNotNull(PreformattedRegex.valueOfLabel(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "5-percentRollout",
            "10-percentRollout",
            "15-percentRollout",
            "20-percentRollout",
            "25-percentRollout",
            "30-percentRollout",
            "35-percentRollout",
            "40-percentRollout",
            "45-percentRollout",
            "50-percentRollout",
            "55-percentRollout",
            "60-percentRollout",
            "65-percentRollout",
            "70-percentRollout",
            "75-percentRollout",
            "80-percentRollout",
            "85-percentRollout",
            "90-percentRollout",
            "95-percentRollout",
    })
    void testUUIDPreformatted(String pattern) {
        // Note: this is a "probabilistic" test. Running more tests would allow acceptance range to be tightened.
        int totalTests = 1100;
        int acceptancePercentagePlusMinus = 3;

        PreformattedRegex preformattedRegex = PreformattedRegex.valueOfLabel(pattern);
        RegexCriteria criteria = new RegexCriteria(Pattern.compile(preformattedRegex.regex));
        double expectPercentHigh = Integer.parseInt(pattern.split("-")[0]) + acceptancePercentagePlusMinus;
        double expectPercentLow = Integer.parseInt(pattern.split("-")[0]) - acceptancePercentagePlusMinus;

        int matchCount = 0;
        for (int i = 0; i < totalTests; i++) {
            // note: UUID is generated in repeatable way, so this test is actually deterministic
            String uuid = UUID.nameUUIDFromBytes((i + pattern).getBytes()).toString();
            boolean match = criteria.isMatch(uuid);
            String uuidSparse = uuid.replace("-", "");
            boolean matchSparse = criteria.isMatch(uuidSparse);
            assertNotEquals(uuid, uuidSparse);
            assertEquals(match, matchSparse);
            matchCount += match ? 1 : 0;
        }

        double percent = 100 * (double) matchCount / (double) totalTests;
        assertTrue(percent > expectPercentLow && percent < expectPercentHigh,
                String.format("Actual match percent %s is out of expected range %s-%s", percent, expectPercentLow, expectPercentHigh));
    }

}
