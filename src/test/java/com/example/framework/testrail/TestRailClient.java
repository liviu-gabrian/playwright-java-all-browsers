package com.example.framework.testrail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TestRailClient {

    // TestRail API returns objects with "id": <number>
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    private final URI baseUri;
    private final HttpClient http;
    private final String authHeaderValue;

    TestRailClient(URI baseUri, String username, String apiKey) {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(apiKey, "apiKey must not be null");

        String raw = username + ":" + apiKey;
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        this.authHeaderValue = "Basic " + encoded;

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    long addRun(long projectId, Long suiteId, String name, boolean includeAll, Collection<Long> caseIds)
            throws IOException, InterruptedException {
        String payload = buildAddRunPayload(suiteId, name, includeAll, caseIds);
        String response = postJson(apiUri("add_run/" + projectId), payload);
        return extractId(response, "add_run");
    }

    void addResultsForCases(long runId, List<TestRailResult> results) throws IOException, InterruptedException {
        Objects.requireNonNull(results, "results must not be null");
        String payload = buildAddResultsPayload(results);
        postJson(apiUri("add_results_for_cases/" + runId), payload);
    }

    void closeRun(long runId) throws IOException, InterruptedException {
        postJson(apiUri("close_run/" + runId), "{}");
    }

    private URI apiUri(String endpoint) {
        String base = baseUri.toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String full = base + "/index.php?/api/v2/" + endpoint;
        return URI.create(full);
    }

    private String postJson(URI uri, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", authHeaderValue)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "{}" : jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("TestRail API call failed: " + uri + " status=" + status + " body=" + response.body());
        }
        return response.body();
    }

    private static long extractId(String json, String context) throws IOException {
        if (json == null) {
            throw new IOException("TestRail API response was empty for " + context);
        }
        Matcher m = ID_PATTERN.matcher(json);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        throw new IOException("Could not extract id from TestRail API response for " + context + ": " + json);
    }

    private static String buildAddRunPayload(Long suiteId, String name, boolean includeAll, Collection<Long> caseIds) {
        String safeName = name == null ? "Automation Run" : name;
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"name\":").append(TestRailJson.string(safeName));
        if (suiteId != null) {
            sb.append(",\"suite_id\":").append(suiteId);
        }
        sb.append(",\"include_all\":").append(includeAll);
        if (!includeAll) {
            sb.append(",\"case_ids\":").append(TestRailJson.arrayOfNumbers(caseIds));
        }
        sb.append('}');
        return sb.toString();
    }

    private static String buildAddResultsPayload(List<TestRailResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\":[");
        for (int i = 0; i < results.size(); i++) {
            TestRailResult r = results.get(i);
            sb.append('{');
            sb.append("\"case_id\":").append(r.caseId());
            sb.append(",\"status_id\":").append(r.statusId());
            if (r.comment() != null && !r.comment().isBlank()) {
                sb.append(",\"comment\":").append(TestRailJson.string(truncate(r.comment(), 4000)));
            }
            sb.append('}');
            if (i < results.size() - 1) {
                sb.append(',');
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) {
            return null;
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, Math.max(0, maxChars - 30)) + "\n\n[truncated]";
    }

    record TestRailResult(long caseId, int statusId, String comment) {
    }
}

