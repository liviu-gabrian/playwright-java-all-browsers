Feature: User Management API

  As an API consumer
  I want to manage users via HTTP endpoints
  So that I can create and retrieve user data

  @api
  Scenario: Create a new user
    When I send a POST request to "/users" with body:
      """
      {
        "username": "newuser",
        "email": "newuser@example.com",
        "name": "New User"
      }
      """
    Then the response status should be 201
    And the response should contain field "id"

  @api
  Scenario: Get user by ID
    Given a user exists with ID "1"
    When I send a GET request to "/users/1"
    Then the response status should be 200
    And the response should contain field "username"
