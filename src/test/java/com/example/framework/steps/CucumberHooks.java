package com.example.framework.steps;

import com.example.framework.driver.PlaywrightManager;
import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Cucumber hooks for Playwright lifecycle and Allure attachments.
 *
 * <p>For scenarios tagged with {@code @ui} or {@code @e2e}:
 * <ul>
 *   <li>{@code @Before}: Initializes Playwright browser context and page.</li>
 *   <li>{@code @After}: On failure, attaches a screenshot to Allure; then tears down context and page.</li>
 * </ul>
 */
public class CucumberHooks {

    @Before(value = "@ui or @e2e", order = 1)
    public void beforeUiScenario() {
        PlaywrightManager.initBrowserContext();
    }

    @After(value = "@ui or @e2e", order = 1)
    public void afterUiScenario(Scenario scenario) {
        try {
            if (scenario.isFailed()) {
                attachScreenshotOnFailure();
            }
        } finally {
            PlaywrightManager.closeContextAndPage();
        }
    }

    private void attachScreenshotOnFailure() {
        try {
            Page page = PlaywrightManager.getPage();
            if (page != null) {
                byte[] screenshot = page.screenshot();
                if (screenshot != null && screenshot.length > 0) {
                    Allure.addAttachment(
                            "Screenshot on failure",
                            "image/png",
                            new ByteArrayInputStream(screenshot),
                            "png");
                }
            }
        } catch (Exception e) {
            String errorMsg = "Failed to capture screenshot: " + e.getMessage();
            Allure.addAttachment(
                    "Screenshot capture error",
                    "text/plain",
                    new ByteArrayInputStream(errorMsg.getBytes(StandardCharsets.UTF_8)),
                    "txt");
        }
    }
}
