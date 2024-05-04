Feature: Functionalities only supported in strict-match mode

Background:
  * configure strictMatch = true

Scenario: issue #2515

  * def cat =
  """
    {
      name: 'Billie',
      kittens: [
        { id: 23, name: 'Bob', bla: [{ b: '1'}] },
        { id: 42, name: 'Wild' }
      ]
    }
    """
  # this matches as expected
  * def expected = [{ id: 42, name: 'Wild' }, { id: 23, name: 'Bob', bla: [{ b: '1'}]}]
  * match cat == { name: 'Billie', kittens: '#(^^expected)' }

  # this does not match as expected
  * def expected = [{ id: 42, name: 'Wild' }, { id: 23, name: 'Bob', bla: { b: '1'} }]
  * match cat == { name: 'Billie', kittens: '#(!^^expected)' }

#    unless the operator is contains
  * match cat == { name: 'Billie', kittens: '#(^expected)' }


Scenario: issue #2516

* def phx =
"""
{
    "name": "Phoenix (All)",
    "city_code": "PHX",
    "airports": {
        "SCF": {
            "name": "Scottsdale Municipal",
        },
        "PHX": {
            "name": "Sky Harbor International",
        }
    }
}
"""
* def aus =
"""
{
    "name": "Austin (All)",
    "city_code": "AUS",
    "airports": {
        "AUS": {
            "name": "Austin–Bergstrom"
        }
    }
}
"""

* def phx_aus_map =
""" {
        "PHX": #(phx),
        "AUS": #(aus)
}
"""

* def phx_aus_list = [#(phx), #(aus)]

* match each phx_aus_map contains {name: #string}
* match each phx_aus_map !contains {code: #string}
* match each phx_aus_list contains {name: #string}

# works with shortcuts too

* def name_schema = {name: #string}
* def code_schema = {code: #string}

* match phx_aus_map == '#[] ^name_schema'
* match phx_aus_map == '#[] !^code_schema'
* match phx_aus_list == '#[] ^name_schema'
* match phx_aus_list == '#[] !^code_schema'

* def multi_level_name_schema = {name: #string, airports: #[] ^name_schema}
* match phx_aus_map == '#[] ^multi_level_name_schema'
* match phx_aus_map == '#[] ^+multi_level_name_schema'
* match phx_aus_list == '#[] ^multi_level_name_schema'

* def multi_level_schema = {name: #string, city_code: #string, airports: #[] name_schema}
* match phx_aus_map == '#[] multi_level_schema'
* match phx_aus_list == '#[] multi_level_schema'

#    each match only applies only at depth > 1 (airports) and not at the root
* match phx == '#(multi_level_schema)'


 Scenario: regular strings that happen to start with # (and are not macros)

    * match ['#foo'] contains '#foo'
    * match ['#foo'] !contains '#fo'
    * match ['#foo'] contains deep '#fo'

    * match {a: '#foo'} == {a: '#foo'}
    * match {a: '#foo'} != {a: '#fo'}
    * match {a: '#foo'} !contains {a: '#fo'}
    * match {a: '#foo'} contains deep {a: '#fo'}