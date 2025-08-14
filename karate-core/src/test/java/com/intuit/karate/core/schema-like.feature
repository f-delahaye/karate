Feature: json-schema like validation

Scenario: but simpler and more powerful

* def response = read('schema-like-odds.json')
# here we enclose in round-brackets to preserve the optional embedded expression
# so that it can be used later in a "match"
* def oddSchema = ({ price: '#string', status: '#? _ < 3', ck: '##number', name: '#regex[0-9X]' })
* def isValidTime = read('schema-like-time-validator.js')

Then match response ==
"""
{ 
  id: '#regex[0-9]+',
  count: '#number',
  odd: '#(oddSchema)',
  data: { 
    countryId: '#number', 
    countryName: '#string', 
    leagueName: '##string', 
    status: '#number? _ >= 0', 
    sportName: '#string',
    time: '#? isValidTime(_)'
  },
  odds: '#[] oddSchema'  
}
"""
# other examples

# should be an array
* match $.odds == '#[]'

# should be an array of size 4
* match $.odds == '#[4]'

# optionally present (or null) and should be an array of size greater than zero
* match $.odds == '##[_ > 0]'

# should be an array of size equal to $.count
* match $ contains { odds: '#[$.count]' }

# use a predicate function to validate each array element
* def isValidOdd = function(o){ return o.name.length == 1 }
* match $.odds == '#[]? isValidOdd(_)'

# for simple arrays, types can be 'in-line'
* def foo = ['bar', 'baz']

# should be an array
* match foo == '#[]'

# should be an array of size 2
* match foo == '#[2]'

# should be an array of strings with size 2
* match foo == '#[2] #string'

# each item of the array should be of length 3
* match foo == '#[]? _.length == 3'

# should be an array of strings each of length 3
* match foo == '#[] #string? _.length == 3'

# should be null or an array of strings
* match foo == '##[] #string'

# each item of the array should match regex (with backslash involved)
* match foo == '#[] #regex \\w+'

# contains
* def actual = [{ a: 1, b: 'x' }, { a: 2, b: 'y' }]

* def schema = { a: '#number', b: '#string' }
* def partSchema = { a: '#number' }
* def badSchema = { c: '#boolean' }
* def mixSchema = { a: '#number', c: '#boolean' }

* def shuffled = [{ a: 2, b: 'y' }, { b: 'x', a: 1 }]
* def first = { a: 1, b: 'x' }
* def part = { a: 1 }
* def mix = { b: 'y', c: true }
* def other = [{ a: 3, b: 'u' }, { a: 4, b: 'v' }]
* def some = [{ a: 1, b: 'x' }, { a: 5, b: 'w' }]

* match actual[0] == schema
* match actual[0] == '#(schema)'

* match actual[0] contains partSchema
* match actual[0] == '#(^partSchema)'

* match actual[0] contains any mixSchema
* match actual[0] == '#(^*mixSchema)'

* match actual[0] !contains badSchema
* match actual[0] == '#(!^badSchema)'

* match each actual == schema
* match actual == '#[] schema'

* match each actual contains partSchema
* match actual == '#[] ^partSchema'

* match each actual contains any mixSchema
* match actual == '#[] ^*mixSchema'

* match each actual !contains badSchema
* match actual == '#[] !^badSchema'

* match actual contains only shuffled
* match actual == '#(^^shuffled)'

* match actual contains first
* match actual == '#(^first)'

* match actual contains any some
* match actual == '#(^*some)'

* match actual !contains other
* match actual == '#(!^other)'

* match actual contains deep part
* match actual == '#(^+part)'

# no in-line equivalent !
* match actual contains '#(^part)'

# no in-line equivalent !
* match actual contains '#(^*mix)'

* assert actual.length == 2
* match actual == '#[2]'

# contains deep
* def actualDeep = [{ a: [1, 2], b: 'x' }, { a: [3, 4], b: 'y' }]

* def partDeep = { a: [1] }
* match actual contains deep part

Scenario: complex nested arrays
* def json =
"""
{
  "foo": {
    "bars": [
      { "barOne": "dc", "barTwos": [{ title: 'blah' }] },
      { "barOne": "dc", "barTwos": [{ title: 'blah' }], barThrees: [{ num: 1 }] }
    ]
  }
}
"""
* def barTwo = { title: '#string' }
* def barThree = { num: '#number' }
* def bar = { barOne: '#string', barTwos: '#[] barTwo', barThrees: '##[] barThree' }
* match json.foo.bars == '#[] bar'

Scenario: re-usable json chunks as nodes, but optional
* def dogSchema = { id: '#string', color: '#string' }
# here we enclose in round-brackets to preserve the optional embedded expression
# so that it can be used later in a "match"
* def schema = ({ id: '#string', name: '#string', dog: '##(dogSchema)' })

* def response1 = { id: '123', name: 'foo' }
* match response1 == schema

* def response2 = { id: '123', name: 'foo', dog: { id: '456', color: 'brown' } }
* match response2 == schema

Scenario: pretty print json
* def json = read('schema-like-odds.json')
* print 'pretty print:\n' + karate.pretty(json)

Scenario: more pretty print
* def myJson = { foo: 'bar', baz: [1, 2, 3]}
* print 'pretty print:\n' + karate.pretty(myJson)

Scenario: various ways of checking that a string ends with a number
* def foo = 'hello1'
* match foo == '#regex hello[0-9]+'
* match foo == '#regex .+[0-9]+'
* match foo contains 'hello'
* assert foo.startsWith('hello')
* def isHello = function(s){ return s.startsWith('hello') && karate.match(s, '#regex .+[0-9]+').pass }
* match foo == '#? isHello(_)'



  Scenario: match each on objects (#2516)
    ### NOTE:
    # Objects are viewed as a set of property keys/property values.
    # Schema validations typically expect the rhs to be an object too and both keys and values will be validated.
    # However, when using in-line forms, the rhs may be any expression, in which case only values will be validated against that expression.

    * def response = read('schema-like-odds-map.json')
# here we enclose in round-brackets to preserve the optional embedded expression
# so that it can be used later in a "match"
    * def oddSchema = ({ price: '#string', status: '#? _ < 3', ck: '##number', name: '#regex[0-9X]' })
    * def isValidTime = read('schema-like-time-validator.js')

    Then match response ==
"""
{
  id: '#regex[0-9]+',
  count: '#number',
  odd: '#(oddSchema)',
  data: {
    countryId: '#number',
    countryName: '#string',
    leagueName: '##string',
    status: '#number? _ >= 0',
    sportName: '#string',
    time: '#? isValidTime(_)'
  },
  odds: '#{} oddSchema'
}
"""
# other examples

# should be a map
    * match $.odds == '#{}'

# should be a map of size 4
    * match $.odds == '#{4}'

# optionally present (or null) and should be an array of size greater than zero
    * match $.odds == '##{_ > 0}'

# should be a map of size equal to $.count
    * match $ contains { odds: '#{$.count}' }

# use a predicate function to validate each value. _ is the current value, so _.name is a valid reference.
    * def isValidOdd = function(o){ return o.name.length == 1 }
    * match $.odds == '#{}? isValidOdd(_)'
# the standard form (match each) also exposes a _$ magic variable with 2 properties: 'key' and 'value'
# test below compares that for each odd, its name property is the same as its key .
    * match each $.odds contains { #string: {name: '#? _ == _$.key'}}

    * def other_odds = {1: {name: 2}, 2: {name: 3}}
    * match each other_odds != { #string: {name: '#? _ == _$.key'}}


# for simple objects, types can be 'in-line'
    * def foo = {entry1: 'bar', entry2: 'baz'}

# should be a object
    * match foo == '#{}'
    * match foo != '#[]'

# should be an object of size 2
    * match foo == '#{2}'

# should be an object of strings with size 2
    * match foo == '#{2} #string'

# each property value of the object should be of length 3
    * match foo == '#{}? _.length == 3'

# each property value of the object should be strings of length 3
    * match foo == '#{} #string? _.length == 3'

# should be null or an object map of strings
    * match foo == '##{} #string'

# each value of the map should match regex (with backslash involved)
    * match foo == '#{} #regex \\w+'




# schema validation
    * def actual = {1: { a: 1, b: 'x' }, 2: { a: 2, b: 'y' }}

    * def schema = { a: '#number', b: '#string' }
    * def partSchema = { a: '#number' }
    * def badSchema = { c: '#boolean' }
    * def mixSchema = { a: '#number', c: '#boolean' }

    * match each actual == {#string: '#(schema)'}
    * match actual == '#{} {#string: #(schema)}'
    * match actual == '#{} {#regex .{1}: #(schema)}'
    * match actual != '#{} {#regex .{2}: #(schema)}'
    # Karate's easy syntax makes it look like keys are numbers but in json they are ALWAYS strings
    * match actual != '#{} {#number: #(schema)}'
    # Shorter syntax - rhs is not an object, only values will be validated
    * match actual == '#{} schema'

    * match each actual contains {#string: '#(partSchema)'}
    * match actual == '#{} {#string: #(^partSchema)}'
    # Shorter syntax - rhs is not an object, only values will be validated
    * match actual == '#{} ^partSchema'

    * match each actual contains any {#string: '#(mixSchema)'}
    * match actual == '#{} {#string: #(^*mixSchema)}'
    # Shorter syntax - rhs is not an object, only values will be validated
    * match actual == '#{} ^*mixSchema'

    # when rhs is an object, key validation is forced to equals. Otherwise, key validations using the provided operator (not contains #string))
    # would fail
    * match each actual !contains {#string: '#(badSchema)'}
    * match actual == '#{} {#string: #(!^badSchema)}'
    * match actual == '#{} {#string: #(!=badSchema)}'
    * match actual != '#{} {#string: #(badSchema)}'
    # Shorter syntax - rhs is not an object, only values will be validated
    * match actual != '#{} badSchema'
    * match actual == '#{} !^badSchema'

    * match actual == '#{2}'