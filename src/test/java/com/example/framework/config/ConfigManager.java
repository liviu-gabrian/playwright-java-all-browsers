package com.example.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Centralized configuration manager for test environments.
 *
 * <p>Resolution order for the active environment:
 * <ol>
 *   <li>System property {@code env} (set via Maven profile / CLI)</li>
 *   <li>Environment variable {@code ENV}</li>
 *   <li>Default value {@code test}</li>
 * </ol>
 *
 * <p>Based on the resolved environment, this class loads a matching
 * {@code config-<env>.properties} file from {@code src/test/resources} and
 * exposes strongly-typed getters for common configuration values.</p>
 */
public final class ConfigManager {

    private static final String ENV_PROPERTY_NAME = "env";
    private static final String ENV_ENVVAR_NAME = "ENV";
    private static final String DEFAULT_ENV = "test";
    private static final String CONFIG_FILE_PATTERN = "config-%s.properties";

    private static final Properties PROPERTIES = new Properties();
    private static final String ACTIVE_ENV;

    static {
        ACTIVE_ENV = resolveEnvironment();
        loadProperties(ACTIVE_ENV);
    }

    private ConfigManager() {
        // utility class
    }

    /**
     * Returns the active logical environment (e.g. {@code test} or {@code uat}).
     */
    public static String getActiveEnvironment() {
        return ACTIVE_ENV;
    }

    /**
     * Generic string accessor.
     */
    public static String get(String key) {
        String value = PROPERTIES.getProperty(Objects.requireNonNull(key, "key must not be null"));
        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing configuration key '" + key + "' in environment '" + ACTIVE_ENV + "'");
        }
        return value;
    }

    /**
     * Optional string accessor (returns {@code null} when the key is missing).
     */
    public static String getOptional(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return PROPERTIES.getProperty(key);
    }

    /**
     * Optional boolean accessor with default value (returns {@code defaultValue} when missing).
     */
    public static boolean getBooleanOrDefault(String key, boolean defaultValue) {
        String raw = getOptional(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String value = raw.toLowerCase(Locale.ROOT).trim();
        if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean(value);
        }
        throw new IllegalArgumentException("Configuration key '" + key + "' with value '" + value
                + "' cannot be parsed as boolean (environment: '" + ACTIVE_ENV + "')");
    }

    public static String getBaseUrl() {
        return get("base.url");
    }

    public static String getApiBaseUrl() {
        return get("api.base.url");
    }

    public static String getBrowser() {
        return get("browser");
    }

    public static boolean isHeadless() {
        return getBoolean("headless");
    }

    public static int getDefaultTimeoutSeconds() {
        return getInt("default.timeout.seconds");
    }

    public static int getRetryCount() {
        return getInt("retry.count");
    }

    public static boolean getBoolean(String key) {
        String value = get(key).toLowerCase(Locale.ROOT).trim();
        if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean(value);
        }
        throw new IllegalArgumentException("Configuration key '" + key + "' with value '" + value
                + "' cannot be parsed as boolean (environment: '" + ACTIVE_ENV + "')");
    }

    public static int getInt(String key) {
        String value = get(key).trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Configuration key '" + key + "' with value '" + value
                    + "' cannot be parsed as integer (environment: '" + ACTIVE_ENV + "')", ex);
        }
    }

    private static String resolveEnvironment() {
        String fromSystemProp = System.getProperty(ENV_PROPERTY_NAME);
        if (fromSystemProp != null && !fromSystemProp.isBlank()) {
            return fromSystemProp.trim().toLowerCase(Locale.ROOT);
        }

        String fromEnvVar = System.getenv(ENV_ENVVAR_NAME);
        if (fromEnvVar != null && !fromEnvVar.isBlank()) {
            return fromEnvVar.trim().toLowerCase(Locale.ROOT);
        }

        return DEFAULT_ENV;
    }

    private static void loadProperties(String env) {
        String fileName = String.format(CONFIG_FILE_PATTERN, env);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ConfigManager.class.getClassLoader();
        }

        try (InputStream input = classLoader.getResourceAsStream(fileName)) {
            if (input == null) {
                throw new IllegalStateException("Could not find configuration file '" + fileName
                        + "' on the test classpath for environment '" + env + "'");
            }
            PROPERTIES.load(input);
            // Transparently decrypt any values that use the ENC(...) format so that callers
            // always work with plain-text values.
            EncryptedPropertySupport.decryptAll(PROPERTIES);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load configuration file '" + fileName
                    + "' for environment '" + env + "'", e);
        }
    }
}

