Feature:

  Background:
    * driver serverUrl + '/18'
    * timeout(500)


  Scenario:
# wait for slow loading element
    * click('#slowlink')
    # Page takes 600 ms to load. 3 * 190 = 570 < 600 so page will be loaded at 4th attempt.
    # Is there a way to assert that?? Other than manually checking the console?
    * retry(5, 190).waitForUrl('/06.html')

    * driver.back()
    * submit().click('#slowlink')
    * match optional('#containerDiv').present == true

    * driver.back()
    * locate('#slowlink').submit().click()
    * match optional('#containerDiv').present == true
