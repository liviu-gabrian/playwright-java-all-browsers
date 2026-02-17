package com.example.framework.api;

import com.example.framework.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Thin wrapper around RestAssured that preconfigures the base URI and common settings
 * using values from {@link ConfigManager}.
 *
 * <p>All HTTP requests from step definitions should go through this client so that
 * environment-specific configuration is applied consistently.</p>
 */
public class ApiClient {

    private final RequestSpecification baseSpec;

    public ApiClient() {
        this.baseSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigManager.getApiBaseUrl())
                .setContentType(ContentType.JSON)
                .build();
    }

    private RequestSpecification givenBase() {
        return RestAssured
                .given()
                .spec(baseSpec)
                .log().all(); // log requests for easier debugging in CI
    }

    public Response post(String path, String body) {
        return givenBase()
                .body(body)
                .when()
                .post(path)
                .then()
                .log().all()
                .extract()
                .response();
    }

    public Response get(String path) {
        return givenBase()
                .when()
                .get(path)
                .then()
                .log().all()
                .extract()
                .response();
    }
}

