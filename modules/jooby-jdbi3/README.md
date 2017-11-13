[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jdbi3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jdbi3)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jdbi3.svg)](https://javadoc.io/doc/org.jooby/jooby-jdbi3/1.2.3)
[![jooby-jdbi3 website](https://img.shields.io/badge/jooby-jdbi3-brightgreen.svg)](http://jooby.org/doc/jdbi3)
# jdbi

<a href="https://jdbi.github.io">Jdbi</a> provides a convenience interface for SQL operations in Java.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.

## exports

* A ```Jdbi``` object

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-jdbi3</artifactId>
 <version>1.2.3</version>
</dependency>
```

## usage

```java
{
  use(new Jdbc());

  use(new Jdbi3());

  get("/pets", () -> {
    Jdbi jdbi = require(Jdbi.class);
    return jdbi.withHandle(handle -> {
      // work with handle
      return ...;
    })
  });
}
```

## transaction per request

```java
{
  use(new Jdbc());

  use(new Jdbi3()
      .transactionPerRequest()
  );

  get("/pets", () -> {
    // This code run inside a transaction
    Handle handle = require(Handle.class);
    // work with handle
    return ...;
  });
}
```

The `transactionPerRequest()` opens a Handle then start, commit/rollback a transaction and finally close a Handle.

The Handle is accessible via `require` call inside a route handler:

```java
{
  get("/pets", () -> {
    // This code run inside a transaction
    Handle handle = require(Handle.class);
    // work with handle
    return ...;
  });
}
```

You can optionally name the Handle:

```java
{
  use(new Jdbi3()
      .transactionPerRequest(new TransactionalRequest()
          .handle("trxPerReq")
      )
  );

  get("/pets", () -> {
    // This code run inside a transaction
    Handle handle = require("trxPerReq", Handle.class);
    // work with handle
    return ...;
  });
}
```

### sqlObjects

You can attach `SqlObjects` to the transaction per request `Handle`:

```java
{
  use(new Jdbi3()
      .transactionPerRequest(new TransactionalRequest()
          .attach(PetRepository.class)
      )
  );

  get("/pets", () -> {
    // Now you can request a pet repository:
    PetRepository petRepo = require(PetRepository.class);
    // work with petRepo
    return ...;
  });
}
```

## multiple database connections

```java
{
  use(new Jdbc("main"));
  use(new Jdbi3("main"));

  use(new Jdbc("audit"));
  use(new Jdbi3("audit"));

  get("/", () -> {
    Jdbi main = require("main", Jdbi.class);
    Jdbi audit = require("audit", Jdbi.class);
    ...
  });

}
```

## configuration

Configuration is done via configuration callback:

```java
{
  use(new Jdbc());

  use(new Jdbi3().doWith(jdbi -> {
    jdbi.installPlugin(new SqlObjectPlugin());
  }));
}
```

## starter project

We do provide a [jdbi-starter](https://github.com/jooby-project/jdbi-starter) project. Go and [fork it](https://github.com/jooby-project/jdbi-starter).

That’s all folks!!
