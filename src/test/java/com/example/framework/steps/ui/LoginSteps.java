package com.example.framework.steps.ui;

import com.example.framework.driver.PlaywrightManager;
import com.example.framework.pages.DashboardPage;
import com.example.framework.pages.LoginPage;
import com.microsoft.playwright.Page;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Assertions;

/**
 * UI step definitions related to authentication and the login screen.
 *
 * <p>These steps are intentionally small and delegate all concrete UI
 * interactions to the underlying Page Object classes so that:
 * <ul>
 *   <li>Playwright selectors remain encapsulated in page objects.</li>
 *   <li>Allure receives rich, nested steps from {@link com.example.framework.pages.BasePage}.</li>
 * </ul>
 * </p>
 */
public class LoginSteps {

    private LoginPage loginPage;
    private DashboardPage dashboardPage;

    private Page page() {
        return PlaywrightManager.getPage();
    }

    private LoginPage loginPage() {
        if (loginPage == null) {
            loginPage = new LoginPage(page());
        }
        return loginPage;
    }

    private DashboardPage dashboardPage() {
        if (dashboardPage == null) {
            dashboardPage = new DashboardPage(page());
        }
        return dashboardPage;
    }

    @Given("I am on the login page")
    public void iAmOnTheLoginPage() {
        Allure.step("User opens the login page", () -> {
            loginPage().open();
            return null;
        });
    }

    @When("I log in as {string} with password {string}")
    public void iLogInAsWithPassword(String username, String password) {
        Allure.step(String.format("User logs in as '%s'", username), () -> {
            loginPage().loginAs(username, password);
            // Re-create dashboard page after navigation to represent post-login state.
            dashboardPage = new DashboardPage(page());
            return null;
        });
    }

    @Then("I should see the dashboard")
    public void iShouldSeeTheDashboard() {
        Allure.step("Verify dashboard is visible to the user", () -> {
            boolean loaded = dashboardPage().isLoaded();
            Assertions.assertTrue(loaded, "Expected dashboard to be loaded after login");
            return null;
        });
    }

    @Then("I should see an error message")
    public void iShouldSeeAnErrorMessage() {
        Allure.step("Verify login error message is displayed", () -> {
            String error = loginPage().getErrorMessage();
            String safeError = error == null ? "" : error;
            Allure.addAttachment("Login error message", safeError);

            Assertions.assertNotNull(error, "Expected an error message after failed login");
            Assertions.assertFalse(
                    safeError.trim().isEmpty(),
                    "Expected non-empty error message after failed login");
            return null;
        });
    }
}

