Feature: User Login

  As a user
  I want to log in to the application
  So that I can access my account

  @ui
  Scenario: Successful login with valid credentials
    Given I am on the login page
    When I log in as "tomsmith" with password "SuperSecretPassword!"
    Then I should see the dashboard

  @ui
  Scenario: Failed login with invalid credentials
    Given I am on the login page
    When I log in as "invaliduser" with password "wrongpass"
    Then I should see an error message
