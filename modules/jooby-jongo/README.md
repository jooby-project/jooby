[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jongo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jongo)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jongo.svg)](https://javadoc.io/doc/org.jooby/jooby-jongo/1.3.0)
[![jooby-jongo website](https://img.shields.io/badge/jooby-jongo-brightgreen.svg)](http://jooby.org/doc/jongo)
# jongo

[Jongo](http://jongo.org) query in Java as in Mongo shell.

> NOTE: This module depends on: [mongodb driver](https://github.com/jooby-project/jooby/tree/master/jooby-mongodb) module.

## exports

* [Jongo](http://jongo.org) instances to a default database

* [JongoFactory](/apidocs/org/jooby/mongodb/JongoFactory.html) to use alternative databases.

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jongo</artifactId>
  <version>1.3.0</version>
</dependency>
```

## usage

```java
{
  use(new Mongodb());
  use(new Jongoby());

  get("/", req -> {
    Jongo jongo = require(Jongo.class);
    // work with jongo...
  });
}
```

Previous example will give you a [Jongo](http://jongo.org) instance connected to the default database, provided by the [mongodb](https://github.com/jooby-project/jooby/tree/master/jooby-mongodb)  module.

Access to alternate database is provided via: [JongoFactory](/apidocs/org/jooby/mongodb/JongoFactory.html).

```java
{
  use(new Mongodb());
  use(new Jongoby());

  get("/", req -> {
    Jongo jongo = require(JongoFactory.class).get("alternate-db");
    // work with jongo...
  });
}
```

For more information, please visit the [Jongo](http://jongo.org) web site.
