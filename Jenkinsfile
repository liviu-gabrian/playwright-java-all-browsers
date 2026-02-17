pipeline {
    agent any

    parameters {
        choice(
                name: 'ENV',
                choices: ['test', 'uat'],
                description: 'Target environment for tests (mapped to Maven profile/env)'
        )
    }

    environment {
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                dir('PlaywrightWithJava/playwright-java-all-browsers') {
                    sh "mvn clean test -P${params.ENV}"
                }
            }
        }

        stage('Allure Report') {
            when {
                expression { fileExists('PlaywrightWithJava/playwright-java-all-browsers/allure-results') }
            }
            steps {
                dir('PlaywrightWithJava/playwright-java-all-browsers') {
                    // If the Jenkins Allure plugin is installed, this block can be replaced
                    // with the built-in 'allure' step. For now we simply archive results.
                    archiveArtifacts artifacts: 'allure-results/**', fingerprint: true
                }
            }
        }
    }
}

