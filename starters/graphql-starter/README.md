# GraphQL Starter

[GraphQL](https://jooby.io/modules/graphql) starter project.

## Schema

We mimic the GraphQL tutorial available [here](https://www.graphql-java.com/tutorials/getting-started-with-spring-boot)

```graphql
type Query {
  bookById(id: ID): Book 
}

type Book {
  id: ID
  name: String
  pageCount: Int
  author: Author
}

type Author {
  id: ID
  firstName: String
  lastName: String
}
```

## run

    mvn jooby:run

Open a browser a type: http://localhost:8080/graphql

## help

* Read the [GraphQL documentation](https://jooby.io/modules/graphql)
* Join the [channel](https://gitter.im/jooby-project/jooby)
