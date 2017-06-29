# neo4j session store

A [Session.Store]({{defdocs}}/neo4j/Neo4jSessionStore) powered by <a href="https://neo4j.com/">Neo4j</a>.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-neo4j</artifactId>
 <version>{{version}}</version>
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
