# embedded neo4j driver

[Neo4j](https://neo4j.com/) driver for Jooby.

## exports

* [GraphDatabaseService](http://neo4j.com/docs/java-reference/current/javadocs/org/neo4j/graphdb/GraphDatabaseService.html)
* [GraphAwareRuntime](https://graphaware.com/site/framework/latest/apidocs/)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-embedded-neo4j</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

application.conf:

```properties
neo4j.databaseDir = "/tmp"
```

```java
  {
    use(new EmbeddedNeo4j());
    get("/", req -> {
      GraphDatabaseService dbService = require(GraphDatabaseService.class);
      // work with db
    });
  }
```

Default database dir property is ```dbService``` but of course you can use any other name:

application.conf:

```properties
neo4j.myDbDir = "/tmp"
```

```java
  {
    use(new EmbeddedNeo4j("myDbDir"));
    get("/", req -> {
      GraphDatabaseService db = require(GraphDatabaseService.class);
      // work with db
    });
  }
```

## properties

Properties can be set via ```.conf``` file:

```properties
neo4j.dbms.security.allow_csv_import_from_file_urls  = true
```

or programmatically:

```java
{
  use(new EmbeddedNeo4j()
    .properties((properties, config) -> {
      properties.put(GraphDatabaseSettings.allow_file_urls, true)
    })
  );
}
```

# embedded neo4j session store

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-embedded-neo4j</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  session(EmbeddedNeo4jSessionStore.class);
  get("/", req -> {
   req.session().set("name", "jooby");
  });
}
```

The ```name``` attribute and value will be stored in a [Neo4j](https://neo4j.com/).

## properties

### timeout

By default, a neo4j session will expire after ```30 minutes```. Changing the default timeout is as simple as:

```properties
# 8 hours

session.timeout = 8h

# 15 seconds

session.timeout = 15

# 120 minutes

session.timeout = 120m

# no timeout

session.timeout = -1
```

It uses [GraphAware's expire library](https://github.com/graphaware/neo4j-expire) to automatically remove expired sessions.

### session label

It's possible to provide the session label using the `neo4j.session.label` property.

## neo4j.conf

```properties
###################################################################################################

# neo4j

###################################################################################################

neo4j.databaseDir = "/tmp"

###################################################################################################

# session datastore

# neo4j.session.label: "sessions"

#  session.timeout: 30m
```