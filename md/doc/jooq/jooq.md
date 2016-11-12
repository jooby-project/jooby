# jOOQ

<a href="http://www.jooq.org">jOOQ</a> generates Java code from your database and lets you build type safe SQL queries through its fluent API.

This module depends on [jdbc](/doc/jdbc) module, make sure you read the doc of the [jdbc](/doc/jdbc) module.

## exports

* ```DSLContext```

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-jooq</artifactId>
 <version>{{version}}</version>
</dependency>
```

## usage

```java
{
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
  use(new jOOQ("db.main"));

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
  use(new jOOQ().doWith(conf -> {

    conf.set(...);
  });

}
```

## code generation

Unfortunately, this module doesn't provide any built-in facility for code generation. If you need help to setup the code generator please checkout the <a href="http://www.jooq.org/doc/latest/manual/code-generation/">jOOQ documentation</a> for more information.
