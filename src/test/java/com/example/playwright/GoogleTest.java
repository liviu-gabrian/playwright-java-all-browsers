package com.example.playwright;

import com.microsoft.playwright.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.example.playwright.utils.AllureReportUtil;
import com.example.playwright.utils.AllurePlaywrightTestWatcher;

@Epic("Demo Playwright Project")
@Feature("Google Homepage")
public class GoogleTest {
    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    Page page;

    @RegisterExtension
    AllurePlaywrightTestWatcher allureWatcher = new AllurePlaywrightTestWatcher(() -> page);

    @BeforeAll
    static void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @BeforeEach
    void createPage() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(java.nio.file.Paths.get("videos"))
        );
        page = context.newPage();
    }

    @Test
    @Story("Open Google and verify title")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Navigate to google.com and verify that the page title contains 'Google'")
    void testGoogleHomePage() {
        Allure.step("Navigate to Google");
        page.navigate("https://www.google.com");
        Allure.step("Verify the page title");
        Assertions.assertTrue(page.title().contains("Google"));
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        } else if (page != null) {
            page.close();
        }
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();

        // ✅ Generate and open Allure report automatically after all tests
        AllureReportUtil.generateAndOpenReport();
    }
}
