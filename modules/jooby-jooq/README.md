[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jooq/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jooq)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jooq.svg)](https://javadoc.io/doc/org.jooby/jooby-jooq/1.5.0)
[![jooby-jooq website](https://img.shields.io/badge/jooby-jooq-brightgreen.svg)](http://jooby.org/doc/jooq)
# jOOQ

<a href="http://www.jooq.org">jOOQ</a> generates Java code from your database and lets you build type safe SQL queries through its fluent API.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.

## exports

* ```DSLContext```

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-jooq</artifactId>
 <version>1.5.0</version>
</dependency>
```

## usage

```java
{
  use(new Jdbc());
  use(new jOOQ());

  get("/jooq", req -> {

    try (DSLContext ctx = require(DSLContext.class)) {
      return ctx.transactionResult(conf -> {
        DSLContext trx = DSL.using(conf);
        return trx.selectFrom(TABLE)
            .where(ID.eq(1))
            .fetchOne(NAME);
      });
    }
  });

}
```

## multiple db connections

```java
{
  use(new Jdbc("db.main"));
  use(new jOOQ("db.main"));

  use(new Jdbc("db.audit"));
  use(new jOOQ("db.audit"));

  get("/main", req -> {

    try (DSLContext ctx = require("db.main", DSLContext.class)) {
      ...
    }
  });

  get("/audit", req -> {

    try (DSLContext ctx = require("db.audit", DSLContext.class)) {
      ...
    }
  });

}
```

## advanced configuration

This module setup a ```Configuration``` object with a ```DataSource``` from [jdbc](/doc/jdbc) module and the ```DefaultTransactionProvider```. More advanced configuration is provided via ```#doWith(BiConsumer)```:

```java
{
  use(new Jdbc());
  use(new jOOQ().doWith(conf -> {

    conf.set(...);
  });

}
```

## code generation

Unfortunately, this module doesn't provide any built-in facility for code generation. If you need help to setup the code generator please checkout the <a href="http://www.jooq.org/doc/latest/manual/code-generation/">jOOQ documentation</a> for more information.
