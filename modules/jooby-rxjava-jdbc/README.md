[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-rxjava-jdbc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-rxjava-jdbc)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-rxjava-jdbc.svg)](https://javadoc.io/doc/org.jooby/jooby-rxjava-jdbc/1.2.0)
[![jooby-rxjava-jdbc website](https://img.shields.io/badge/jooby-rxjava-jdbc-brightgreen.svg)](http://jooby.org/doc/rxjava-jdbc)
# rxjdbc

<a href="https://github.com/davidmoten/rxjava-jdbc">rxjava-jdbc</a> efficient execution, concise code, and functional composition of database calls using JDBC and RxJava Observable.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-rxjava-jdbc</artifactId>
 <version>1.2.0</version>
</dependency>
```

## exports

* A ```Database``` object 
* A [Hikari](https://github.com/brettwooldridge/HikariCP) ```DataSource``` object 

## usage

```java
import org.jooby.rx.RxJdbc;
import org.jooby.rx.Rx;
{
  // required
  use(new Rx());
  use(new Jdbc());

  use(new RxJdbc());

  get("/reactive", req ->
    require(Database.class)
      .select("select name from something where id = :id")
      .parameter("id", 1)
      .getAs(String.class)
  );

}
```

The [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) mapper converts ```Observable``` to [deferred](/apidocs/org/jooby/Deferred.html) instances. More at [rx module](/doc/rxjava).

## multiple db connections

```java
import org.jooby.rx.RxJdbc;
import org.jooby.rx.Rx;
{
  use(new Jdbc("db.main"));
  use(new RxJdbc("db.main"));

  use(new Jdbc("db.audit"));
  use(new RxJdbc("db.audit"));

  get("/", req ->
    Databse db = require("db.main", Database.class);
    Databse audit = require("db.audit", Database.class);
    // ...
  ).map(Rx.rx());

}
```

For more details on how to configure the Hikari datasource, please check the [jdbc module](/doc/jdbc).
