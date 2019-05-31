```
interface Character {
  id: ID!
  name: String!
  friends: [Character]
  appearsIn: [Episode]!
}

type Human implements Character {
  id: ID!
  name: String!
  friends: [Character]
  appearsIn: [Episode]!
  starships: [Starship]
  totalCredits: Int
}

type Droid implements Character {
  id: ID!
  name: String!
  friends: [Character]
  appearsIn: [Episode]!
  primaryFunction: String
}

union SearchResult = Human | Droid | Starship

{
  leftComparison: hero(episode: EMPIRE) {
    ...comparisonFields
  }
  rightComparison: hero(episode: JEDI) {
    ...comparisonFields
  }
}

fragment comparisonFields on Character {
  name
  appearsIn
  friends {
    name
  }
}

{"query" : String, "operationName" : String, "variables" : Map<String, String>}

{ "data" : Map<String, String>, "errors" : [...], "extensions" : Map<String, String>}

   query {                      
     authors {                  # fetches authors (1 query)
       name       
       address {                # fetches address for each author (N queries for N authors)
         country
       }
     }
   }
   
{heroOne:getHero(name:"Thor") {powers} heroTwo:getHero(name:"Star-lord") {powers}}

[{getHero(name:"Thor") {powers}}, {getHero(name:"Star-lord") {powers}}]
```