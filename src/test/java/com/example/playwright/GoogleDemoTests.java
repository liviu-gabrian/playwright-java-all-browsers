package com.example.playwright;

import com.microsoft.playwright.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.example.playwright.utils.AllurePlaywrightTestWatcher;

@Epic("Demo Playwright Project")
@Feature("Mixed passing and failing tests")
public class GoogleDemoTests {

    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    Page page;

    @RegisterExtension
    AllurePlaywrightTestWatcher allureWatcher = new AllurePlaywrightTestWatcher(() -> page);

    @BeforeAll
    static void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
    }

    @BeforeEach
    void createPage() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(java.nio.file.Paths.get("videos"))
        );
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        } else if (page != null) {
            page.close();
        }
    }

    @AfterAll
    static void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    // ===== Passing UI tests =====

    @Test
    @Story("Open Google home page")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Should open google.com and verify the title contains 'Google'")
    void googleHomePageTitle_shouldContainGoogle() {
        Allure.step("Navigate to Google");
        page.navigate("https://www.google.com");

        Allure.step("Verify title contains 'Google'");
        Assertions.assertTrue(page.title().contains("Google"));
    }

    @Test
    @Story("Search for Playwright")
    @Severity(SeverityLevel.NORMAL)
    @Description("Search for 'Playwright' and verify that the results page URL contains 'search'")
    void searchForPlaywright_shouldOpenResultsPage() {
        Allure.step("Navigate to Google");
        page.navigate("https://www.google.com");

        Allure.step("Type query 'Playwright' and submit");
        page.locator("input[name='q']").fill("Playwright");
        page.keyboard().press("Enter");

        Allure.step("Wait for navigation and verify URL");
        page.waitForURL("**/search**");
        Assertions.assertTrue(page.url().contains("search"));
    }

    @Test
    @Story("Simple assertion demo - pass")
    @Severity(SeverityLevel.MINOR)
    @Description("Pure JUnit assertion that should always pass")
    void simpleMath_shouldPass() {
        Allure.step("Verify that 2 + 2 = 4");
        Assertions.assertEquals(4, 2 + 2);
    }

    @Test
    @Story("Another UI check")
    @Severity(SeverityLevel.NORMAL)
    @Description("Open google.com and verify that the URL starts with https")
    void googleHomePageUrl_shouldUseHttps() {
        Allure.step("Navigate to Google");
        page.navigate("https://www.google.com");

        Allure.step("Verify URL starts with https");
        Assertions.assertTrue(page.url().startsWith("https://"));
    }

    @Test
    @Story("Title does not equal exact string")
    @Severity(SeverityLevel.TRIVIAL)
    @Description("Demonstrates negative assertion that still passes")
    void googleTitle_shouldNotBeJustGoogle() {
        Allure.step("Navigate to Google");
        page.navigate("https://www.google.com");

        Allure.step("Verify title is not exactly 'Google'");
        Assertions.assertNotEquals("Google", page.title());
    }

    // ===== Intentionally failing tests =====

    @Test
    @Story("Intentional failure - wrong title")
    @Severity(SeverityLevel.CRITICAL)
    @Description("This test is designed to fail: expects title to contain 'Bing'")
    void googleTitle_shouldContainBing_butFails() {
        Allure.step("Navigate to Google");
        page.navigate("https://www.google.com");

        Allure.step("Intentionally wrong expectation");
        Assertions.assertTrue(page.title().contains("Bing"),
                "Intentional failure: Google title does not contain 'Bing'");
    }

    @Test
    @Story("Intentional failure - math")
    @Severity(SeverityLevel.MINOR)
    @Description("This test is designed to fail: incorrect math expectation")
    void simpleMath_shouldFail() {
        Allure.step("Verify that 2 * 2 = 5 (intentional failure)");
        Assertions.assertEquals(5, 2 * 2,
                "Intentional failure: 2 * 2 is not 5");
    }

    @Test
    @Story("Intentional failure - search result URL")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test is designed to fail: expects results URL to contain 'this-will-never-exist'")
    void searchForPlaywright_shouldFailOnWeirdUrlCheck() {
        Allure.step("Navigate to Google");
        page.navigate("https://www.google.com");

        Allure.step("Search for Playwright");
        page.locator("input[name='q']").fill("Playwright");
        page.keyboard().press("Enter");
        page.waitForURL("**/search**");

        Allure.step("Intentionally wrong URL expectation");
        Assertions.assertTrue(page.url().contains("this-will-never-exist"),
                "Intentional failure: URL does not contain the expected fake string");
    }

    @Test
    @Story("Intentional failure - boolean condition")
    @Severity(SeverityLevel.TRIVIAL)
    @Description("Simple assertion that is always false")
    void alwaysFalseCondition_shouldFail() {
        Allure.step("Assert that false is true (intentional failure)");
        Assertions.assertTrue(false, "Intentional failure: false is not true");
    }

    @Test
    @Story("Intentional failure - string equality")
    @Severity(SeverityLevel.TRIVIAL)
    @Description("String equality check that is designed to fail")
    void stringEquality_shouldFail() {
        Allure.step("Compare two different strings");
        Assertions.assertEquals("expected", "actual",
                "Intentional failure: strings are not equal");
    }
}
