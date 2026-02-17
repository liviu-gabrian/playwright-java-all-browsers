package com.example.framework.steps.api;

import com.example.framework.api.ApiClient;
import com.example.framework.context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Cucumber step definitions for HTTP/API interactions using RestAssured.
 *
 * <p>Each step logs the HTTP request and response details as Allure steps and
 * attaches JSON payloads for rich reporting.</p>
 */
public class ApiSteps {

    private final ApiClient apiClient;
    private final ScenarioContext context;

    public ApiSteps(ApiClient apiClient, ScenarioContext context) {
        this.apiClient = apiClient;
        this.context = context;
    }

    @When("I send a POST request to {string} with body:")
    public void iSendAPostRequestWithBody(String path, String body) {
        Allure.step("HTTP POST " + path, () -> {
            Response response = apiClient.post(path, body);
            context.setLastResponse(response);
            attachRequestAndResponse("POST", path, body, response);
            return null;
        });
    }

    @When("I send a GET request to {string}")
    public void iSendAGetRequest(String path) {
        Allure.step("HTTP GET " + path, () -> {
            Response response = apiClient.get(path);
            context.setLastResponse(response);
            attachRequestAndResponse("GET", path, null, response);
            return null;
        });
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        Response response = context.getLastResponse();
        assertNotNull(response, "No HTTP response available in scenario context");
        Allure.step("Assert HTTP status code", () -> {
            int actualStatus = response.statusCode();
            attachText("HTTP status assertion",
                    "Expected: " + expectedStatus + System.lineSeparator()
                            + "Actual:   " + actualStatus);
            assertEquals(expectedStatus, actualStatus, "Unexpected HTTP status code");
            return null;
        });
    }

    @Then("the response should contain field {string}")
    public void theResponseShouldContainField(String fieldName) {
        Response response = context.getLastResponse();
        assertNotNull(response, "No HTTP response available in scenario context");

        Allure.step("Assert response contains field '" + fieldName + "'", () -> {
            Object value = response.jsonPath().get(fieldName);
            String pretty = response.asPrettyString();
            attachText("HTTP response body", pretty);
            assertNotNull(value, "Expected JSON field '" + fieldName + "' to be present in response");
            return null;
        });
    }

    @Given("a user exists with ID {string}")
    public void aUserExistsWithId(String userId) {
        Allure.step("Assume user exists with ID: " + userId, () -> {
            // In a real implementation you might call the API to create or verify the user.
            context.put("userId", userId);
            return null;
        });
    }

    private void attachRequestAndResponse(String method, String path, String requestBody, Response response) {
        StringBuilder meta = new StringBuilder();
        meta.append(method).append(" ").append(path).append(System.lineSeparator())
                .append("Status: ").append(response.statusCode());
        attachText("HTTP meta", meta.toString());

        if (requestBody != null && !requestBody.trim().isEmpty()) {
            attachText("HTTP request body", requestBody);
        }

        String responseBody = response.asPrettyString();
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            attachText("HTTP response body", responseBody);
        }
    }

    private void attachText(String name, String content) {
        Allure.addAttachment(
                name,
                "text/plain",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "txt");
    }
}

