# jongo

{{jongo}} query in Java as in Mongo shell.

> NOTE: This module depends on: [mongodb driver]({{gh}}/jooby-mongodb) module.

## exports

* {{jongo}} instances to a default database

* [JongoFactory]({{defdocs}}/mongodb/JongoFactory.html) to use alternative databases.

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jongo</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  use(new Mongodb());
  use(new Jongoby());

  get("/", req -> {
    Jongo jongo = req.require(Jongo.class);
    // work with jongo...
  });
}
```

Previous example will give you a {{jongo}} instance connected to the default database, provided by the [mongodb]({{gh}}/jooby-mongodb)  module.

Access to alternate database is provided via: [JongoFactory]({{defdocs}}/mongodb/JongoFactory.html).

```java
{
  use(new Mongodb());
  use(new Jongoby());

  get("/", req -> {
    Jongo jongo = req.require(JongoFactory.class).get("alternate-db");
    // work with jongo...
  });
}
```

For more information, please visit the {{jongo}} web site.
