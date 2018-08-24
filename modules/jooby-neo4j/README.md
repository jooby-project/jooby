[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-neo4j/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-neo4j/1.5.1)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-neo4j.svg)](https://javadoc.io/doc/org.jooby/jooby-neo4j/1.5.1)
[![jooby-neo4j website](https://img.shields.io/badge/jooby-neo4j-brightgreen.svg)](http://jooby.org/doc/neo4j)
# neo4j

<a href="https://neo4j.com">Neo4j</a> is a highly scalable native graph database that leverages data relationships as first-class entities, helping enterprises build intelligent applications to meet today’s evolving data challenges.

This module give you access to <a href="https://neo4j.com">neo4j</a> and <a href="https://github.com/Wolfgang-Schuetzelhofer/jcypher">jcypher</a> APIs.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-neo4j</artifactId>
 <version>1.5.1</version>
</dependency>
```

## exports
* GraphDatabaseService for embedded neo4j instances. 
* Driver and Session objects for remote instances 
* IDBAccess object

## usage

```java
{
  use(new Neo4j());

  get("/driver", () -> {
    // work with driver
    Driver driver = require(Driver.class);
  });

  get("/session", () -> {
    // work with session
    Session session = require(Session.class);
  });

  get("/dbaccess", () -> {
    // work with driver
    BoltDBAccess dbaccess = require(BoltDBAccess.class);
  });
}
```

application.conf

```
db.url = "bolt://localhost:7687"
 db.user = myuser
 db.password = mypassword
```

## embedded
In addition to remote access using ```bolt``` protocol, this module provide access to ```embedded``` neo4j instances:

In memory mode:

```java
{
  use(new Neo4j("mem"));
}
```

File system mode:

```java
{
  use(new Neo4j("fs"));
}
```

Optionally you can specify the desired path:

```java
{
  use(new Neo4j(Paths.get("path", "mydb")));
}
```

The embedded mode allow you to access `GraphDatabaseService` instances:

```java
{
  use(new Neo4j("mem"));

  get("/", () -> {
    GraphDatabaseService db = require(GraphDatabaseService.class);
  });

}
```

As well as `EmbeddedDBAccess`:

```java
{
  use(new Neo4j("mem"));

  get("/", () -> {
    EmbeddedDBAccess db = require(EmbeddedDBAccess.class);
  });

}
```

## runtime modules

This option is available for ```embedded``` Neo4j instances and we allow to configure one or more runtime modules via ```.conf``` file:

```
com.graphaware.runtime.enabled = true
com.graphaware.module = [{
  class: com.graphaware.neo4j.expire.ExpirationModuleBootstrapper
  nodeExpirationProperty: _expire
}, {
  class: com.graphaware.neo4j.expire.AnotherModule
  modProp: modValue
}]
```

You first need to ```enabled``` the graph runtime framework by setting the ```com.graphaware.runtime.enabled``` property.

Then you need to add one or more modules under the ```com.graphaware.module``` property path.

## two or more connections

Two or more connection is available by setting and installing multiples {@link Neo4j} modules:

```java
{
  use(new Neo4j("db1"));

  use(new Neo4j("db2"));

  get("/", () -> {
    Driver db1 = require("db1", Driver.class);
    BoltDBAccess bolt1 = require("db1", BoltDBAccess.class);

    Driver db2 = require("db2", Driver.class);
    BoltDBAccess bolt2 = require("db2", BoltDBAccess.class);
  });
}
```

application.conf:

```
db1.url = "bolt://localhost:7687"
db1.user = db1user
db1.password = db1pass

db2.url = "bolt://localhost:7687"
db2.user = db2user
db2.password = db2pass
```

## options

<a href="https://neo4j.com">Neo4j</a> options are available via ```.conf``` file:

```
neo4j.dbms.read_only = true
neo4j.unsupported.dbms.block_size.array_properties = 120
```

# neo4j session store

A [Session.Store](/apidocs/org/jooby/neo4j/Neo4jSessionStore) powered by <a href="https://neo4j.com/">Neo4j</a>.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-neo4j</artifactId>
 <version>1.5.1</version>
</dependency>
```

## usage

```java
{
  session(Neo4jSessionStore.class);

  get("/", req -> {

   req.session().set("name", "jooby");
  });

}
```

The ```name``` attribute and value will be stored in a <a href="https://neo4j.com/">Neo4j</a> database.

## options

### timeout

By default, a neo4j session will expire after ```30 minutes```. Changing the default timeout is as simple as:

```
# 8 hours
session.timeout = 8h
# 15 seconds
session.timeout = 15
# 120 minutes
session.timeout = 120m
```

It uses <a href="https://github.com/graphaware/neo4j-expire">GraphAware's Expire</a> library to automatically remove expired sessions.

For embedded databases you need to configure the expire module, like:

```
com.graphaware.runtime.enabled = true
com.graphaware.module = [{
  class: com.graphaware.neo4j.expire.ExpirationModuleBootstrapper
  nodeExpirationProperty: _expire
}]
```

The `Neo4jSessionStore` uses the ```_expire``` attribute to evict sessions.

If you connect to a remote server make sure the expire module was installed. More information at <a href="https://github.com/graphaware/neo4j-expire"></a>.

If no timeout is required, use ```-1```.

### session label

It's possible to provide the session label using the ```neo4j.session.label``` property.
