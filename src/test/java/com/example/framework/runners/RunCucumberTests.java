package com.example.framework.runners;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * JUnit 5 Cucumber test runner that discovers and executes all feature files
 * under {@code src/test/resources/features}.
 *
 * <p>Configuration:
 * <ul>
 *   <li>Glue: {@code com.example.framework.steps} (step definitions and hooks)</li>
 *   <li>Plugins: Allure Cucumber 7 adapter and pretty console output</li>
 * </ul>
 *
 * <p>Run with: {@code mvn test -Ptest} or {@code mvn test -Puat}
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.framework.steps")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm, pretty, com.example.framework.testrail.TestRailCucumberPlugin")
public class RunCucumberTests {
}
