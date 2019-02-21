Feature: User management

    Scenario: Retrieve administrator user
        When I search user by users key 'xm' on tenant 'XM'
        Then the user is found
        And his first name is 'Administrator'
