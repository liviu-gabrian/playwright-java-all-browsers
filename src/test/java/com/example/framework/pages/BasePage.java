package com.example.framework.pages;

import com.example.framework.config.ConfigManager;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Allure;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Base abstraction for all UI page objects.
 *
 * <p>Provides:
 * <ul>
 *   <li>A shared {@link Page} instance for derived page classes.</li>
 *   <li>Convenience methods for navigation, clicking, typing, and reading text.</li>
 *   <li>Lightweight Allure step wrapping for better reporting.</li>
 * </ul>
 *
 * <p>Concrete pages should extend this class and expose higher-level business actions, e.g.:
 * <pre>{@code
 * public class LoginPage extends BasePage {
 *     public LoginPage(Page page) {
 *         super(page);
 *     }
 *
 *     public void loginAs(String username, String password) {
 *         type("#username", username, "Fill username");
 *         type("#password", password, "Fill password");
 *         click("#submit", "Click login");
 *     }
 * }
 * }</pre>
 */
public abstract class BasePage {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = Objects.requireNonNull(page, "page must not be null");
    }

    /**
     * Navigates to the given URL. If the value does not start with {@code http},
     * it is treated as a path relative to {@link ConfigManager#getBaseUrl()}.
     */
    public void navigateTo(String urlOrPath) {
        String target = urlOrPath;
        if (!urlOrPath.toLowerCase().startsWith("http")) {
            String base = ConfigManager.getBaseUrl();
            if (base.endsWith("/") && urlOrPath.startsWith("/")) {
                target = base + urlOrPath.substring(1);
            } else if (!base.endsWith("/") && !urlOrPath.startsWith("/")) {
                target = base + "/" + urlOrPath;
            } else {
                target = base + urlOrPath;
            }
        }

        final String navUrl = target;
        step("Navigate to: " + navUrl, () -> {
            page.navigate(navUrl);
            return null;
        });

        // Expose the loading lifecycle as a dedicated Allure step so that
        // consumers of the report can clearly see that the page was waiting
        // for a stable load state before further interactions.
        waitForPageLoad();
    }

    /**
     * Returns a {@link Locator} for the given selector.
     */
    protected Locator $(String selector) {
        return page.locator(selector);
    }

    public void click(String selector) {
        click(selector, "Click element: " + selector);
    }

    public void click(String selector, String stepName) {
        step(stepName, () -> {
            $(selector).click();
            return null;
        });
    }

    public void type(String selector, String text) {
        type(selector, text, "Type into element: " + selector);
    }

    public void type(String selector, String text, String stepName) {
        step(stepName, () -> {
            $(selector).fill(text);
            return null;
        });
    }

    public String getText(String selector) {
        return step("Get text from: " + selector, () -> $(selector).innerText());
    }

    public void waitForVisible(String selector) {
        step("Wait for visible: " + selector, () -> {
            $(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            return null;
        });
    }

    /**
     * Waits for the page to reach the default {@link LoadState#NETWORKIDLE}
     * state and records this as an explicit Allure step.
     *
     * <p>This makes the "loading" phase of a navigation visible in Allure
     * rather than having it be implicit inside Playwright's API calls.</p>
     */
    public void waitForPageLoad() {
        waitForPageLoad(LoadState.NETWORKIDLE);
    }

    /**
     * Waits for the page to reach the given {@link LoadState} and records
     * this as an explicit Allure step, making loading behaviour observable
     * in generated reports.
     */
    public void waitForPageLoad(LoadState state) {
        step("Wait for page load state: " + state, () -> {
            page.waitForLoadState(state);
            return null;
        });
    }

    /**
     * Wraps a Playwright action or query in an Allure step for better reporting.
     */
    protected <T> T step(String name, Supplier<T> action) {
        return Allure.step(name, action::get);
    }
}

