## Playwright Java Framework – UI + API + BDD

This module (`playwright-java-all-browsers`) is a **Playwright + Java 21 + Cucumber BDD + RestAssured + Allure** automation framework for UI, API, and end‑to‑end testing.

### Tech stack

- **Java 21**
- **Maven**
- **Playwright** `1.48.0` (UI & E2E)
- **Cucumber 7** (BDD feature files + JUnit 5 runner + TestRail plugin)
- **RestAssured 5** (HTTP/API testing)
- **Allure** (JUnit 5 + Cucumber 7 adapters)
- **Jenkins + Docker** (CI execution)

### Project structure (framework)

- `pom.xml` – Maven configuration, Playwright/Cucumber/RestAssured/Allure dependencies, `test`/`uat` profiles.
- `src/test/resources/config-test.properties` – configuration for **test** environment (base URLs, browser, timeouts).
- `src/test/resources/config-uat.properties` – configuration for **uat** environment.
- `src/test/java/com/example/framework/config/ConfigManager.java` – loads env config (based on `-Denv` or Maven profile).
- `src/test/java/com/example/framework/driver/PlaywrightManager.java` – Playwright browser/context/page lifecycle.
- `src/test/java/com/example/framework/pages/*.java` – Page Objects (`LoginPage`, `DashboardPage`, `BasePage`).
- `src/test/java/com/example/framework/steps/ui/*.java` – UI step definitions.
- `src/test/java/com/example/framework/api/ApiClient.java` – RestAssured client configured per environment.
- `src/test/java/com/example/framework/context/ScenarioContext.java` – shared per‑scenario context (e.g. last HTTP response).
- `src/test/java/com/example/framework/steps/api/ApiSteps.java` – HTTP/API step definitions with Allure logging.
- `src/test/java/com/example/framework/steps/CucumberHooks.java` – Playwright+Cucumber hooks, screenshots on failure.
- `src/test/java/com/example/framework/runners/RunCucumberTests.java` – JUnit 5 Cucumber runner.
- `src/test/resources/features/ui/*.feature` – UI and E2E UI scenarios.
- `src/test/resources/features/api/*.feature` – HTTP/API scenarios.
- `src/test/resources/features/e2e/*.feature` – combined API + UI scenarios.
- `Jenkinsfile` – Jenkins pipeline for running tests with `ENV` parameter (`test` / `uat`).
- `Dockerfile` – container definition for running the suite in CI or locally.
- Legacy demo:
  - `src/test/java/com/example/playwright/GoogleTest.java`, `GoogleDemoTests.java`, `utils/*` – original JUnit+Playwright demo tests.

### Optional TestRail integration

This framework can **publish Cucumber results to TestRail**. Integration is opt‑in and disabled by default.

- **Tagging cases**
  - Add TestRail case IDs as tags on scenarios:
    - `@C1234`
    - or `@testrail-C1234`, `@testrail_C1234`
  - Multiple tags can be used if one scenario should update several cases.

- **Configuration sources (priority order)**
  - JVM system properties (e.g. `-Dtestrail.enabled=true`)
  - Environment variables (e.g. `TESTRAIL_ENABLED=true`)
  - Environment config files:
    - `src/test/resources/config-test.properties`
    - `src/test/resources/config-uat.properties`

- **Main properties / env vars**
  - `testrail.enabled` / `TESTRAIL_ENABLED` (`true` / `false`)
  - `testrail.url` / `TESTRAIL_URL` – e.g. `https://your-company.testrail.io`
  - `testrail.username` / `TESTRAIL_USERNAME`
  - `testrail.apiKey` / `TESTRAIL_API_KEY`
  - `testrail.projectId` / `TESTRAIL_PROJECT_ID` – numeric project id
  - `testrail.suiteId` / `TESTRAIL_SUITE_ID` (optional, for multi‑suite projects)
  - `testrail.runId` / `TESTRAIL_RUN_ID` (optional, use existing run)
  - `testrail.createRun` / `TESTRAIL_CREATE_RUN` (`true`/`false`, default `true` if no runId)
  - `testrail.includeAll` / `TESTRAIL_INCLUDE_ALL` – when `true`, all cases in the suite are added to the run; when `false`, only tagged case IDs are added.
  - `testrail.closeRun` / `TESTRAIL_CLOSE_RUN` – close run after publishing.
  - `testrail.runName` / `TESTRAIL_RUN_NAME` – custom run name (defaults to `Automation - <env> - <timestamp>`).

- **How it works**
  - A Cucumber plugin (`com.example.framework.testrail.TestRailCucumberPlugin`) is wired into the JUnit 5 runner.
  - At the end of the run it:
    - Collects all scenario results with `@Cxxxx` tags.
    - Creates a TestRail run (if no `runId` is provided and `createRun=true`), or reuses the existing run.
    - Sends results via `add_results_for_cases`.
    - Optionally closes the run when `closeRun=true`.
  - All publish activity and errors are attached to the Allure report under attachments like:
    - `TestRail run created`
    - `TestRail publish`
    - `TestRail publish error`

### Prerequisites

- **Java 21+**
  - Verify installation:
    ```bash
    java -version
    ```
    The version should be 21.

- **Maven**
  - Verify installation:
    ```bash
    mvn -version
    ```
    Make sure Maven is using the same JDK (Java 21).

- **Browsers**
  - On a typical desktop OS (Windows/macOS/Linux), having Chrome/Edge/Firefox installed is enough.
  - Playwright will download its own browser binaries on the first run.

### Install dependencies / compile

From the project root (`playwright-java-all-browsers`), run:

```bash
mvn clean compile
```

This will:
- Download `com.microsoft.playwright` (version `1.48.0`)
- Download JUnit 5 and Allure dependencies
- Compile the sources

### Running Cucumber tests (UI, API, E2E)

All Cucumber tests are executed via the `RunCucumberTests` JUnit 5 runner. Environment selection is controlled by Maven profiles and the `env` system property.

- **Run against `test` environment** (default):

  ```bash
  mvn clean test -Ptest
  ```

- **Run against `uat` environment**:

  ```bash
  mvn clean test -Puat
  ```

Notes:

- The `test` profile uses demo URLs out of the box:
  - **UI**: [the-internet.herokuapp.com](https://the-internet.herokuapp.com) (login: `tomsmith` / `SuperSecretPassword!`)
  - **API**: [jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com)
- Update `config-test.properties` / `config-uat.properties` to point to your real app when integrating.
- `GoogleDemoTests` (legacy demo with intentional failures) is excluded by default; run with the additional `demo` profile to include it:

  ```bash
  mvn clean test -Ptest -Pdemo
  ```

- On the first run, Playwright downloads its browser binaries (Chromium, Firefox, WebKit).
- Tests produce Allure results under `allure-results`.
- Cucumber features are organised as:
  - UI: `src/test/resources/features/ui` (e.g. `login.feature`, `e2e_checkout.feature`)
  - API: `src/test/resources/features/api` (e.g. `user_management.feature`)
  - E2E (API + UI): `src/test/resources/features/e2e` (e.g. `api_ui_user_login.feature` – verifies user via API then performs UI login)

### Generate Allure report (optional)

If tests produce Allure results, you can generate the report with:

```bash
mvn allure:report
```

The HTML report will be available under:

- `target/site/allure-maven-plugin/index.html`

Open it in a browser to inspect the latest test run.

### Quick start: run Cucumber suite and open Allure

Allure is **already configured** for both JUnit 5 and Cucumber:

From `playwright-java-all-browsers`:

```bash
# 1) Run all BDD tests (UI + API + E2E) against test environment
mvn clean test -Ptest
```

If the Allure CLI is installed and on your `PATH`, the test suite will:

- Produce Allure results under `allure-results`.

If you prefer to open it manually, you can also do:

```bash
# Regenerate report via Maven plugin
mvn allure:report

# Or serve directly from raw results
allure serve allure-results
```

In the report you will see:
- `GoogleTest` – a simple happy‑path test for Google home page with screenshot attachments.
- `GoogleDemoTests` (when executed with `-Pdemo`) – **10 demo tests**:
  - A mix of **passing** checks (title, URL, simple math).
  - **Intentionally failing** and **timeout** scenarios to demo how failures and errors look in Allure.

### Jenkins CI

- The module contains a `Jenkinsfile` that:
  - Accepts an **`ENV` parameter** (`test` / `uat`).
  - Runs `mvn clean test -P${ENV}` inside the `PlaywrightWithJava/playwright-java-all-browsers` directory.
  - Archives `allure-results` (ready for the Jenkins Allure plugin, if installed).

In a Jenkins multibranch pipeline, point the job at the repo; Jenkins will detect the `Jenkinsfile` in this module.

### Docker usage

You can run the tests in a container using the provided `Dockerfile`:

```bash
cd playwright-java-all-browsers
docker build -t playwright-java-framework .

# Run against test environment (default)
docker run --rm -e ENV=test playwright-java-framework

# Run against uat environment
docker run --rm -e ENV=uat playwright-java-framework
```

The container runs `mvn clean test -P$ENV` by default; the base image already contains Playwright browsers and a compatible Java runtime.

### What is Allure and why it’s here

- **Allure** is a test reporting framework that:
  - Aggregates results from your test runs (JUnit 5 in this project).
  - Produces a rich, interactive HTML report (suites, stories, steps, attachments, screenshots).
  - Makes it easy to see **which tests passed/failed**, with **stack traces** and **screenshots**.

- In this project:
  - Allure is integrated via:
    - The Maven plugin (`allure-maven`) configured in `pom.xml`.
    - The JUnit 5 integration (`allure-junit5`) used in the tests.
    - `AllureReportUtil` which can generate and open the report after tests run.
  - Each test can use:
    - Allure annotations like `@Epic`, `@Feature`, `@Story`, `@Severity`, `@Description`.
    - `Allure.step(...)` and attachments (e.g. screenshots) for detailed reporting.

#### Installing Allure CLI (to open reports)

To view the report locally, you need the **Allure Commandline** installed and available on your `PATH`.

- On macOS with Homebrew:

```bash
brew install allure
```

- Then from the project root you can either:

```bash
# 1) Use Maven plugin output (after mvn test)
mvn allure:report
open target/site/allure-maven-plugin/index.html   # macOS
```

or, if you prefer the Allure CLI “serve” mode using `allure-results`:

```bash
allure serve allure-results
```

This starts a local HTTP server and opens the Allure report in your browser, showing all **passing, failing, and broken** tests with their details.
