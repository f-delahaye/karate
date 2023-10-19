Feature:

Background:
* driver serverUrl + '/04'

Scenario:
# foo=bar is set by the server
* def cookie1 = { name: 'foo', value: 'bar' }
And match driver.cookies contains deep cookie1
And match cookie('foo') contains deep cookie1

* def cookie2 = { name: 'hello', value: 'world', domain: 'foo.bar', path: '/' }
* cookie(cookie2)
* match driver.cookies contains deep cookie2

# delete cookie
* deleteCookie('foo')
* match driver.cookies !contains '#(^cookie1)'

# clear cookies
* clearCookies()
* match driver.cookies == '#[0]'

# set multiple cookies at once e.g. from an API call
* def data = [{ name: 'one', value: '1', domain: 'foo.bar', path: '/' }, { name: 'two', value: '2', domain: 'foo.bar', path: '/' }]
* driver.cookies = data
* match driver.cookies contains deep data

