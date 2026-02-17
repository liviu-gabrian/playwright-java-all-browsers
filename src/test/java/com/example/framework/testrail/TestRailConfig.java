package com.example.framework.testrail;

import com.example.framework.config.ConfigManager;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class TestRailConfig {
    final boolean enabled;
    final URI baseUri;
    final String username;
    final String apiKey;
    final long projectId;
    final Long suiteId;
    final Long runId;
    final boolean createRun;
    final boolean includeAll;
    final boolean closeRun;
    final String runName;

    private TestRailConfig(
            boolean enabled,
            URI baseUri,
            String username,
            String apiKey,
            long projectId,
            Long suiteId,
            Long runId,
            boolean createRun,
            boolean includeAll,
            boolean closeRun,
            String runName
    ) {
        this.enabled = enabled;
        this.baseUri = baseUri;
        this.username = username;
        this.apiKey = apiKey;
        this.projectId = projectId;
        this.suiteId = suiteId;
        this.runId = runId;
        this.createRun = createRun;
        this.includeAll = includeAll;
        this.closeRun = closeRun;
        this.runName = runName;
    }

    static TestRailConfig load() {
        boolean enabled = booleanFromFirstNonBlank(
                System.getProperty("testrail.enabled"),
                System.getenv("TESTRAIL_ENABLED"),
                ConfigManager.getOptional("testrail.enabled"),
                "false"
        );

        if (!enabled) {
            return new TestRailConfig(false, null, null, null, -1, null, null, false, false, false, null);
        }

        String url = firstNonBlank(
                System.getProperty("testrail.url"),
                System.getenv("TESTRAIL_URL"),
                ConfigManager.getOptional("testrail.url")
        );
        String username = firstNonBlank(
                System.getProperty("testrail.username"),
                System.getenv("TESTRAIL_USERNAME"),
                ConfigManager.getOptional("testrail.username")
        );
        String apiKey = firstNonBlank(
                System.getProperty("testrail.apiKey"),
                System.getenv("TESTRAIL_API_KEY"),
                ConfigManager.getOptional("testrail.apiKey")
        );
        String projectIdRaw = firstNonBlank(
                System.getProperty("testrail.projectId"),
                System.getenv("TESTRAIL_PROJECT_ID"),
                ConfigManager.getOptional("testrail.projectId")
        );

        Long runId = longOrNull(firstNonBlank(
                System.getProperty("testrail.runId"),
                System.getenv("TESTRAIL_RUN_ID"),
                ConfigManager.getOptional("testrail.runId")
        ));

        boolean createRun = booleanFromFirstNonBlank(
                System.getProperty("testrail.createRun"),
                System.getenv("TESTRAIL_CREATE_RUN"),
                ConfigManager.getOptional("testrail.createRun"),
                (runId == null ? "true" : "false")
        );

        boolean includeAll = booleanFromFirstNonBlank(
                System.getProperty("testrail.includeAll"),
                System.getenv("TESTRAIL_INCLUDE_ALL"),
                ConfigManager.getOptional("testrail.includeAll"),
                "false"
        );

        boolean closeRun = booleanFromFirstNonBlank(
                System.getProperty("testrail.closeRun"),
                System.getenv("TESTRAIL_CLOSE_RUN"),
                ConfigManager.getOptional("testrail.closeRun"),
                "false"
        );

        Long suiteId = longOrNull(firstNonBlank(
                System.getProperty("testrail.suiteId"),
                System.getenv("TESTRAIL_SUITE_ID"),
                ConfigManager.getOptional("testrail.suiteId")
        ));

        String runName = firstNonBlank(
                System.getProperty("testrail.runName"),
                System.getenv("TESTRAIL_RUN_NAME"),
                ConfigManager.getOptional("testrail.runName")
        );
        if (runName == null || runName.isBlank()) {
            String env = ConfigManager.getActiveEnvironment();
            String ts = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            runName = "Automation - " + env + " - " + ts;
        }

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("TestRail enabled but missing 'testrail.url' / TESTRAIL_URL");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("TestRail enabled but missing 'testrail.username' / TESTRAIL_USERNAME");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("TestRail enabled but missing 'testrail.apiKey' / TESTRAIL_API_KEY");
        }
        if (projectIdRaw == null || projectIdRaw.isBlank()) {
            throw new IllegalArgumentException("TestRail enabled but missing 'testrail.projectId' / TESTRAIL_PROJECT_ID");
        }

        long projectId;
        try {
            projectId = Long.parseLong(projectIdRaw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid TestRail project id: '" + projectIdRaw + "'", e);
        }

        if (runId == null && !createRun) {
            throw new IllegalArgumentException("TestRail enabled but neither 'testrail.runId' provided nor 'testrail.createRun=true'");
        }

        return new TestRailConfig(true, URI.create(url.trim()), username.trim(), apiKey.trim(), projectId, suiteId, runId, createRun, includeAll, closeRun, runName);
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return null;
    }

    private static boolean booleanFromFirstNonBlank(String a, String b, String c, String defaultValue) {
        String raw = firstNonBlank(a, b, c, defaultValue);
        String v = raw == null ? "false" : raw.trim().toLowerCase(Locale.ROOT);
        return "true".equals(v);
    }

    private static Long longOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: '" + raw + "'", e);
        }
    }
}

