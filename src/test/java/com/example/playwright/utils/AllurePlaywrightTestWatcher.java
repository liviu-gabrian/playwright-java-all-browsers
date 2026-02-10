package com.example.playwright.utils;

import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * JUnit 5 TestWatcher that only adds Playwright artifacts (screenshot + video info)
 * when a test FAILS. Passing tests will not generate these attachments.
 */
public class AllurePlaywrightTestWatcher implements TestWatcher {

    private final Supplier<Page> pageSupplier;

    public AllurePlaywrightTestWatcher(Supplier<Page> pageSupplier) {
        this.pageSupplier = pageSupplier;
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        Page page = pageSupplier.get();
        if (page == null) {
            return;
        }

        String testName = context.getDisplayName();

        try {
            // Screenshot only for failed tests
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            Allure.addAttachment(
                    "Screenshot on failure: " + testName,
                    "image/png",
                    new ByteArrayInputStream(screenshot),
                    ".png"
            );
        } catch (Exception e) {
            // ignore screenshot issues to not hide the original failure
        }

        try {
            // If video recording is enabled on the context, attach the video path as a text artifact
            Optional.ofNullable(page.video())
                    .ifPresent(video -> {
                        try {
                            Path videoPath = video.path();
                            String content = videoPath.toAbsolutePath().toString();
                            Allure.addAttachment(
                                    "Video on failure (path): " + testName,
                                    "text/plain",
                                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                                    ".txt"
                            );
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            // ignore video issues as well
        }
    }
}

