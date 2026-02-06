package com.example.playwright.utils;

import java.io.File;
import java.io.IOException;

public class AllureReportUtil {

    private static final String RESULTS_DIR = "target/allure-results";
    private static final String REPORT_DIR = "target/allure-report";

    public static void generateAndOpenReport() {
        try {
            File resultsDir = new File(RESULTS_DIR);
            if (!resultsDir.exists() || resultsDir.listFiles() == null || resultsDir.listFiles().length == 0) {
                System.err.println("⚠️ No Allure results found. Run your tests first (mvn test).");
                return;
            }

            System.out.println("📊 Generating Allure HTML report...");
            ProcessBuilder generate = new ProcessBuilder("allure", "generate", RESULTS_DIR, "--clean", "-o", REPORT_DIR);
            generate.inheritIO();
            Process process = generate.start();
            process.waitFor();

            System.out.println("✅ Allure report generated successfully at: " + REPORT_DIR);
            openReportInBrowser(REPORT_DIR + "/index.html");

        } catch (Exception e) {
            System.err.println("❌ Failed to generate Allure report. Make sure Allure CLI is installed and in PATH.");
            e.printStackTrace();
        }
    }

    private static void openReportInBrowser(String reportPath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        File reportFile = new File(reportPath);

        if (!reportFile.exists()) {
            System.err.println("⚠️ Report file not found: " + reportPath);
            return;
        }

        System.out.println("🌐 Opening report in browser...");

        if (os.contains("win")) {
            new ProcessBuilder("cmd", "/c", "start", reportFile.getAbsolutePath()).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", reportFile.getAbsolutePath()).start();
        } else if (os.contains("nix") || os.contains("nux")) {
            new ProcessBuilder("xdg-open", reportFile.getAbsolutePath()).start();
        } else {
            System.out.println("⚠️ Unknown OS. Please open the report manually: " + reportFile.getAbsolutePath());
        }
    }
}
