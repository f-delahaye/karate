Feature:

  Background:
    * driver serverUrl + '/19'
    * timeout(2000)

  Scenario: Friendly locators

    * match locate('{^i}Center').rightOf().find('span').attribute('id') == 'e'
    * match rightOf('{^i}Center').find('span').attribute('id') == 'e'
    * match leftOf('{^i}Center').find('span').attribute('id') == 'w'
    * match leftOf('{^i}Center').find('{^b}West').attribute('id') == 'w_nested'
#    PW returns them all sorted by distance. Currently, the Karate API only allows find() to return one element.
    * match near('{^i}Center').find('span').attribute('id') == 'w'
    * match rightOf('{^i}Center').find('{^b}West').present == false