package com.example.framework.pages;

import com.microsoft.playwright.Page;

/**
 * Page Object representing the main post-login dashboard/home screen.
 *
 * <p>Encapsulates common interactions and verifications that are
 * typically performed after a successful login, such as checking
 * that the dashboard is loaded and performing navigation or logout.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * DashboardPage dashboard = new DashboardPage(page);
 * assertTrue(dashboard.isLoaded());
 * }</pre>
 */
public class DashboardPage extends BasePage {

    // Optional direct route to the dashboard – adjust to match the AUT.
    public static final String DASHBOARD_PATH = "/dashboard";

    // Core dashboard selectors – update as needed to match the AUT.
    private static final String HEADER_MAIN = "h1, h2";
    private static final String USER_MENU = "[data-testid='user-menu'], .user-menu";
    private static final String BUTTON_LOGOUT = "[data-testid='logout'], .logout-button";

    public DashboardPage(Page page) {
        super(page);
    }

    /**
     * Navigates directly to the dashboard page using {@link #DASHBOARD_PATH}.
     * This is useful for scenarios that start from an already-authenticated state.
     */
    public void open() {
        navigateTo(DASHBOARD_PATH);
        waitForVisible(HEADER_MAIN);
    }

    /**
     * Returns {@code true} if the dashboard appears to be loaded.
     * <p>
     * This method only performs a lightweight visibility check on core
     * dashboard elements. More specific assertions should be done
     * in tests or step definitions using finer-grained methods.
     * </p>
     */
    public boolean isLoaded() {
        return step("Verify dashboard is loaded", () -> page.locator(HEADER_MAIN).isVisible());
    }

    /**
     * Opens the user menu if it is present on the dashboard.
     */
    public void openUserMenu() {
        click(USER_MENU, "Open user menu");
    }

    /**
     * Performs a logout action from the dashboard, if supported by the AUT.
     */
    public void logout() {
        openUserMenu();
        click(BUTTON_LOGOUT, "Click logout");
    }
}

