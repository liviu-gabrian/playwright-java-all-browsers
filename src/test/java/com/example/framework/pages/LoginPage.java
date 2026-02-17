package com.example.framework.pages;

import com.microsoft.playwright.Page;

/**
 * Page Object representing the login screen of the application.
 *
 * <p>This class models the core interactions required to authenticate a user and
 * is intended to be used from Cucumber step definitions and JUnit tests.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * LoginPage loginPage = new LoginPage(page);
 * loginPage.open();
 * DashboardPage dashboard = loginPage.loginAs("user", "password");
 * }</pre>
 *
 * <p>The concrete CSS selectors are intentionally kept simple and can be
 * adjusted to match the real application under test. They should also
 * remain stable over time so that step definitions do not depend on raw
 * selectors.</p>
 */
public class LoginPage extends BasePage {

    // URL path for the login page relative to the configured base URL.
    // Adjust this constant to match your real application route.
    public static final String LOGIN_PATH = "/login";

    // Core element selectors – update as needed to match the AUT.
    private static final String INPUT_USERNAME = "#username";
    private static final String INPUT_PASSWORD = "#password";
    private static final String BUTTON_SUBMIT = "button[type='submit']";
    private static final String ALERT_ERROR = ".login-error, .alert-error, .flash.error, .flash";

    public LoginPage(Page page) {
        super(page);
    }

    /**
     * Opens the login page using the configured base URL and {@link #LOGIN_PATH}.
     */
    public void open() {
        navigateTo(LOGIN_PATH);
        waitForVisible(INPUT_USERNAME);
    }

    /**
     * Types the provided username into the username input.
     */
    public void enterUsername(String username) {
        type(INPUT_USERNAME, username, "Fill login username");
    }

    /**
     * Types the provided password into the password input.
     */
    public void enterPassword(String password) {
        type(INPUT_PASSWORD, password, "Fill login password");
    }

    /**
     * Clicks the login/submit button.
     */
    public void submit() {
        click(BUTTON_SUBMIT, "Click login submit");
    }

    /**
     * Performs a full login flow and returns a {@link DashboardPage} representing
     * the post-login area of the application.
     */
    public DashboardPage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        submit();
        return new DashboardPage(page);
    }

    /**
     * Returns the text of a generic login error message, if present.
     * This can be used by step definitions or tests to assert on error content.
     */
    public String getErrorMessage() {
        return getText(ALERT_ERROR);
    }
}

