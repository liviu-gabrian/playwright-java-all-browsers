package com.example.framework.context;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simple per-scenario context holder used to share data between step definitions.
 *
 * <p>This class is designed to be instantiated by Cucumber's object factory
 * (e.g. PicoContainer) and injected into step definition classes via
 * constructor injection.</p>
 */
public class ScenarioContext {

    private Response lastResponse;
    private final Map<String, Object> data = new HashMap<>();

    /**
     * Stores the latest HTTP response for the current scenario.
     */
    public void setLastResponse(Response response) {
        this.lastResponse = response;
    }

    /**
     * Returns the latest HTTP response for the current scenario.
     */
    public Response getLastResponse() {
        return lastResponse;
    }

    /**
     * Puts an arbitrary key/value pair in the scenario context.
     */
    public void put(String key, Object value) {
        data.put(Objects.requireNonNull(key, "key must not be null"), value);
    }

    /**
     * Retrieves a value from the context as a string, or {@code null} if absent.
     */
    public String getString(String key) {
        Object value = data.get(Objects.requireNonNull(key, "key must not be null"));
        return value != null ? value.toString() : null;
    }
}

