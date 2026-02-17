package com.example.framework.testrail;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TestRailTagParser {

    // Supported tags:
    // - @C1234
    // - @c1234
    // - @testrail-C1234 / @testrail_C1234 (optional prefix)
    private static final Pattern CASE_TAG_PATTERN = Pattern.compile("^@(?:(?i:testrail)[-_])?(?i:c)(\\d+)$");

    private TestRailTagParser() {
    }

    static Set<Long> extractCaseIds(Collection<String> tagNames) {
        Objects.requireNonNull(tagNames, "tagNames must not be null");
        Set<Long> caseIds = new LinkedHashSet<>();
        for (String tag : tagNames) {
            if (tag == null) {
                continue;
            }
            Matcher matcher = CASE_TAG_PATTERN.matcher(tag.trim());
            if (matcher.matches()) {
                String digits = matcher.group(1);
                try {
                    caseIds.add(Long.parseLong(digits));
                } catch (NumberFormatException ignored) {
                    // ignore malformed case ids
                }
            }
        }
        return caseIds;
    }
}

