# neo4j driver

[Neo4j](https://neo4j.com/) driver for Jooby.

## exports

* [BoltDBAccess](https://neo4j.com/developer/java/#jcypher)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-neo4j</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

application.conf:

```properties
neo4j.db {
  uri = "bolt://localhost:7687"
  username = "neo4j"
  password = "neo4j1"
}
```

```java
  {
    use(new Neo4j());
    get("/", req -> {
      BoltDBAccess db = require(BoltDBAccess.class);
      // work with db
    });
  }
```

Default connection info property is ```db``` but of course you can use any other name:

application.conf:

```properties
neo4j.mydb {
  uri = "bolt://localhost:7687"
  username = "neo4j"
  password = "neo4j1"
}
```

```java
  {
    use(new Neo4j("mydb"));
    get("/", req -> {
      BoltDBAccess db = require(BoltDBAccess.class);
      // work with db
    });
  }
```

## properties

Properties can be set via ```.conf``` file:

```properties
neo4j.db.arrayBlockSize = "120"
```

or programmatically:

```java
{
  use(new Neo4j()
    .properties((properties, config) -> {
      properties.put(DBProperties.ARRAY_BLOCK_SIZE, "120")
    })
  );
}
```

### connection URI

Default connection URI is defined by the ```neo4j.db.uri``` property. Neo4j URI looks like:

```properties
neo4j.db.uri = bolt://host1[:port1]
```

For more detailed information please check: [Neo4jBoltDrivers](https://neo4j.com/docs/developer-manual/current/drivers/).

## neo4j.conf

```properties
###################################################################################################

# neo4j

###################################################################################################

neo4j.db {
  server_root_uri = "bolt://localhost:7687"
  array_block_size = "120"
  pagecache_memory = "1M"
  string_block_size = "120"
  username = "****"
  password = "****"
}
```