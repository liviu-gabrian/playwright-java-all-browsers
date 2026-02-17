package com.example.framework.driver;

import com.example.framework.config.ConfigManager;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Paths;

/**
 * Central factory/manager for Playwright objects (Playwright, Browser, BrowserContext, Page).
 *
 * <p>This class:
 * <ul>
 *   <li>Uses {@link ConfigManager} to determine browser type, headless mode, timeouts, and base URL.</li>
 *   <li>Maintains all Playwright primitives in {@link ThreadLocal} storage so that it is safe
 *       to use in future parallel test execution.</li>
 *   <li>Provides simple lifecycle methods to create and clean up browser resources.</li>
 * </ul>
 *
 * <p>Typical usage in tests or Cucumber hooks:
 * <pre>{@code
 *   @BeforeEach
 *   void setUp() {
 *       PlaywrightManager.initBrowserContext();
 *       Page page = PlaywrightManager.getPage();
 *   }
 *
 *   @AfterEach
 *   void tearDown() {
 *       PlaywrightManager.closeContextAndPage();
 *   }
 *
 *   @AfterAll
 *   static void tearDownAll() {
 *       PlaywrightManager.closePlaywright();
 *   }
 * }</pre>
 */
public final class PlaywrightManager {

    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();

    private PlaywrightManager() {
        // utility class
    }

    /**
     * Initializes (lazily) {@link Playwright} and the configured {@link Browser}.
     * Safe to call multiple times; initialization happens only once per thread.
     */
    public static void initPlaywright() {
        if (PLAYWRIGHT.get() != null && BROWSER.get() != null) {
            return;
        }

        Playwright playwright = Playwright.create();
        PLAYWRIGHT.set(playwright);

        String browserName = ConfigManager.getBrowser().toLowerCase();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(ConfigManager.isHeadless());

        Browser browser;
        switch (browserName) {
            case "chromium":
                browser = playwright.chromium().launch(launchOptions);
                break;
            case "firefox":
                browser = playwright.firefox().launch(launchOptions);
                break;
            case "webkit":
                browser = playwright.webkit().launch(launchOptions);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported browser configured: '" + browserName
                                + "'. Supported values are: chromium, firefox, webkit.");
        }

        BROWSER.set(browser);
    }

    /**
     * Creates a new {@link BrowserContext} and {@link Page} for the current thread using
     * configuration values from {@link ConfigManager}.
     *
     * <p>If a context/page already exists for this thread, it is first closed to avoid resource
     * leaks.</p>
     */
    public static void initBrowserContext() {
        initPlaywright();

        // Close any existing context/page for this thread before creating a new one
        closeContextAndPage();

        Browser browser = BROWSER.get();
        if (browser == null) {
            throw new IllegalStateException("Browser was not initialized. Call initPlaywright() first.");
        }

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setBaseURL(ConfigManager.getBaseUrl());

        // Optional: enable video recording directory for debugging
        contextOptions.setRecordVideoDir(Paths.get("videos"));

        BrowserContext context = browser.newContext(contextOptions);
        context.setDefaultTimeout(ConfigManager.getDefaultTimeoutSeconds() * 1000L);

        Page page = context.newPage();

        CONTEXT.set(context);
        PAGE.set(page);
    }

    public static Playwright getPlaywright() {
        Playwright playwright = PLAYWRIGHT.get();
        if (playwright == null) {
            throw new IllegalStateException("Playwright is not initialized. Call initPlaywright() first.");
        }
        return playwright;
    }

    public static Browser getBrowser() {
        Browser browser = BROWSER.get();
        if (browser == null) {
            throw new IllegalStateException("Browser is not initialized. Call initPlaywright() first.");
        }
        return browser;
    }

    public static BrowserContext getContext() {
        BrowserContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("BrowserContext is not initialized. Call initBrowserContext() first.");
        }
        return context;
    }

    public static Page getPage() {
        Page page = PAGE.get();
        if (page == null) {
            throw new IllegalStateException("Page is not initialized. Call initBrowserContext() first.");
        }
        return page;
    }

    /**
     * Closes the current thread's {@link Page} and {@link BrowserContext}, if any.
     * Does not close the shared {@link Browser} or {@link Playwright} instance.
     */
    public static void closeContextAndPage() {
        Page page = PAGE.get();
        if (page != null) {
            try {
                page.close();
            } catch (Exception ignored) {
                // ignore errors on close to not hide the original failure
            } finally {
                PAGE.remove();
            }
        }

        BrowserContext context = CONTEXT.get();
        if (context != null) {
            try {
                context.close();
            } catch (Exception ignored) {
                // ignore errors on close
            } finally {
                CONTEXT.remove();
            }
        }
    }

    /**
     * Closes the underlying {@link Browser} and {@link Playwright} instances for the current thread.
     */
    public static void closePlaywright() {
        Browser browser = BROWSER.get();
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception ignored) {
            } finally {
                BROWSER.remove();
            }
        }

        Playwright playwright = PLAYWRIGHT.get();
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception ignored) {
            } finally {
                PLAYWRIGHT.remove();
            }
        }
    }
}

