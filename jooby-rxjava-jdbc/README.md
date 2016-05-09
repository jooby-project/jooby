# rxjdbc

<a href="https://github.com/davidmoten/rxjava-jdbc">rxjava-jdbc</a> efficient execution, concise code, and functional composition of database calls using JDBC and RxJava Observable.

This module depends on [jdbc module](/doc/jdbc), make sure you read the doc of the [jdbc module](/doc/jdbc) module.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-rxjdbc</artifactId>
 <version>1.0.0.CR3</version>
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
  use(new RxJdbc());

  get("/reactive", req ->
    req.require(Database.class)
      .select("select name from something where id = :id")
      .parameter("id", 1)
      .getAs(String.class)
  ).map(Rx.rx());

}
```

The [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) mapper converts ```Observable``` to [deferred](/apidocs/org/jooby/Deferred.html) instances. More at [rx module](/doc/rxjava).

## multiple db connections

```java
import org.jooby.rx.RxJdbc;
import org.jooby.rx.Rx;
{
  use(new RxJdbc("db.main"));

  use(new RxJdbc("db.audit"));

  get("/", req ->

    Databse db = req.require("db.main", Database.class);
    Databse audit = req.require("db.audit", Database.class);
    // ...
  ).map(Rx.rx());

}
```

For more details on how to configure the Hikari datasource, please check the [jdbc module](/doc/jdbc).

Happy coding!!!
