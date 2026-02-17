Feature: API + UI user verification

  As a QA engineer
  I want to verify a user via API and UI
  So that I can validate both layers in a single E2E flow

  @api @ui @e2e
  Scenario: Verify user details via API and perform UI login
    Given a user exists with ID "1"
    When I send a GET request to "/users/1"
    Then the response status should be 200
    And the response should contain field "username"
    Given I am on the login page
    When I log in as "tomsmith" with password "SuperSecretPassword!"
    Then I should see the dashboard

