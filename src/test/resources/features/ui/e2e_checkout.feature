Feature: End-to-end checkout flow

  As a customer
  I want to complete a checkout
  So that I can purchase items

  @ui @e2e
  Scenario: Complete checkout as logged-in user
    Given I am on the login page
    And I log in as "customer" with password "customer123"
    When I navigate to the checkout
    And I complete the checkout with my saved address
    Then I should see an order confirmation
