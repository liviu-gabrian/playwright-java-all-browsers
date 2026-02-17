package com.example.framework.testrail;

import com.example.framework.config.ConfigManager;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes Cucumber scenario results to TestRail.
 *
 * <p>How mapping works:
 * <ul>
 *   <li>Tag scenarios with case ids like {@code @C1234} (or {@code @testrail-C1234}).</li>
 *   <li>At the end of the run, results are posted to TestRail for all encountered case ids.</li>
 * </ul>
 *
 * <p>Enable via config or system properties:
 * <ul>
 *   <li>{@code -Dtestrail.enabled=true}</li>
 *   <li>{@code -Dtestrail.url=https://company.testrail.io}</li>
 *   <li>{@code -Dtestrail.username=you@company.com}</li>
 *   <li>{@code -Dtestrail.apiKey=...}</li>
 *   <li>{@code -Dtestrail.projectId=1}</li>
 * </ul>
 */
public final class TestRailCucumberPlugin implements ConcurrentEventListener {

    // TestRail status ids:
    // 1 Passed, 2 Blocked, 3 Untested, 4 Retest, 5 Failed
    private static final int STATUS_PASSED = 1;
    private static final int STATUS_RETEST = 4;
    private static final int STATUS_FAILED = 5;

    private final ConcurrentHashMap<Long, AggregatedResult> aggregatedByCaseId = new ConcurrentHashMap<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onTestRunFinished);
    }

    private void onTestCaseFinished(TestCaseFinished event) {
        TestCase testCase = event.getTestCase();
        Set<Long> caseIds = TestRailTagParser.extractCaseIds(tagNamesFrom(testCase.getTags()));
        if (caseIds.isEmpty()) {
            return;
        }

        int statusId = mapStatus(event.getResult().getStatus());
        String comment = buildComment(event);

        for (Long caseId : caseIds) {
            if (caseId == null) {
                continue;
            }
            aggregatedByCaseId.merge(
                    caseId,
                    new AggregatedResult(statusId, comment),
                    AggregatedResult::mergePreferWorseStatus
            );
        }
    }

    private void onTestRunFinished(TestRunFinished event) {
        TestRailConfig cfg;
        try {
            cfg = TestRailConfig.load();
        } catch (RuntimeException e) {
            attachAllure("TestRail configuration error", e.toString());
            return;
        }

        if (!cfg.enabled) {
            return;
        }

        if (aggregatedByCaseId.isEmpty()) {
            attachAllure("TestRail", "Enabled, but no @C1234 tags were found in this run.");
            return;
        }

        long runId = cfg.runId != null ? cfg.runId : -1;
        Set<Long> caseIds = aggregatedByCaseId.keySet();

        try {
            TestRailClient client = new TestRailClient(cfg.baseUri, cfg.username, cfg.apiKey);

            if (cfg.runId == null) {
                if (!cfg.createRun) {
                    attachAllure("TestRail", "Enabled, but no run id provided and createRun=false. Skipping publish.");
                    return;
                }
                runId = client.addRun(cfg.projectId, cfg.suiteId, cfg.runName, cfg.includeAll, cfg.includeAll ? List.of() : caseIds);
                attachAllure("TestRail run created", "Run id: " + runId + "\nName: " + cfg.runName);
            }

            List<TestRailClient.TestRailResult> results = new ArrayList<>(aggregatedByCaseId.size());
            for (var entry : aggregatedByCaseId.entrySet()) {
                long caseId = entry.getKey();
                AggregatedResult agg = entry.getValue();
                results.add(new TestRailClient.TestRailResult(caseId, agg.statusId, agg.comment));
            }

            client.addResultsForCases(runId, results);
            attachAllure("TestRail publish", "Published " + results.size() + " case result(s) to run " + runId
                    + " (env=" + ConfigManager.getActiveEnvironment() + ", at=" + OffsetDateTime.now() + ").");

            if (cfg.closeRun) {
                client.closeRun(runId);
                attachAllure("TestRail run closed", "Run id: " + runId);
            }
        } catch (IOException | InterruptedException e) {
            attachAllure("TestRail publish error", stackTrace(e));
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (RuntimeException e) {
            attachAllure("TestRail publish error", stackTrace(e));
        }
    }

    private static int mapStatus(Status status) {
        if (status == null) {
            return STATUS_RETEST;
        }
        return switch (status) {
            case PASSED -> STATUS_PASSED;
            case FAILED -> STATUS_FAILED;
            case SKIPPED, PENDING, UNDEFINED, AMBIGUOUS -> STATUS_RETEST;
            default -> STATUS_RETEST;
        };
    }

    /**
     * Extracts tag name strings from the given collection of tag objects.
     * Compatible with Cucumber 7.x where tags may be Tag (plugin.event) or PickleTag (core.gherkin).
     */
    private static Collection<String> tagNamesFrom(Collection<?> tags) {
        List<String> names = new ArrayList<>();
        if (tags == null) {
            return names;
        }
        for (Object t : tags) {
            if (t == null) {
                continue;
            }
            String name = null;
            if (t instanceof String) {
                name = (String) t;
            } else {
                try {
                    java.lang.reflect.Method m = t.getClass().getMethod("getName");
                    Object result = m.invoke(t);
                    if (result != null) {
                        name = result.toString();
                    }
                } catch (Exception ignored) {
                    name = t.toString();
                }
            }
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private static String buildComment(TestCaseFinished event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Scenario: ").append(event.getTestCase().getName()).append('\n');
        sb.append("Status: ").append(event.getResult().getStatus()).append('\n');
        sb.append("Env: ").append(ConfigManager.getActiveEnvironment()).append('\n');
        if (event.getResult().getError() != null) {
            sb.append('\n').append(stackTrace(event.getResult().getError()));
        }
        return sb.toString();
    }

    private static void attachAllure(String name, String content) {
        try {
            Allure.addAttachment(
                    name,
                    "text/plain",
                    new ByteArrayInputStream((content == null ? "" : content).getBytes(StandardCharsets.UTF_8)),
                    "txt"
            );
        } catch (Exception ignored) {
            // best-effort only
        }
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private record AggregatedResult(int statusId, String comment) {
        static AggregatedResult mergePreferWorseStatus(AggregatedResult a, AggregatedResult b) {
            // "worse" meaning Failed beats Retest beats Passed.
            AggregatedResult worse = severity(a.statusId) >= severity(b.statusId) ? a : b;

            // Keep both comments if they differ (helps when multiple scenarios map to one case)
            String mergedComment = mergeComments(a.comment, b.comment);
            return new AggregatedResult(worse.statusId, mergedComment);
        }

        private static int severity(int statusId) {
            return switch (statusId) {
                case STATUS_FAILED -> 3;
                case STATUS_RETEST -> 2;
                case STATUS_PASSED -> 1;
                default -> 0;
            };
        }

        private static String mergeComments(String a, String b) {
            if (a == null || a.isBlank()) {
                return b;
            }
            if (b == null || b.isBlank()) {
                return a;
            }
            if (a.equals(b)) {
                return a;
            }
            return a + "\n\n---\n\n" + b;
        }
    }
}

