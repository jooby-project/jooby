# queryDSL

SQL abstraction provided by <a href="http://www.querydsl.com">QueryDSL</a> using plain JDBC underneath.

This module depends on [jdbc module](doc/jdbc), make sure you read the doc of the [jdbc module](doc/jdbc) module before using this module.

## exports

* ```SQLQueryFactory```

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-querydsl</artifactId>
 <version>1.0.0.CR1</version>
</dependency>
```

## usage

```java
import org.jooby.querydsl.QueryDSL;
{
  use(new QueryDSL());

  get("/my-api", req -> {

    SQLQueryFactory queryFactory = req.require(SQLQueryFactory.class);
    // Do something with the database
    ...
  });

}
```

## dialects

Dialect is detected automatically and usually you don't need to do anything. But if the default dialect detector doesn't work and/or you have a custom dialect:

```java
{
  use(new QueryDSL().with(new MyCustomTemplates());

}
```

## multiple databases

```java
import org.jooby.querydsl.QueryDSL;
{
  use(new QueryDSL("db.main"));

  use(new QueryDSL("db.aux"));

  get("/my-api", req -> {

    SQLQueryFactory queryFactory = req.require("db.main", SQLQueryFactory.class);
    // Do something with the database
  });

}
```

## advanced configuration

This module builds QueryDSL SQLQueryFactories on top of a default ```Configuration``` object, a ```SQLTemplates``` instance and a ```javax.sql.DataSource``` from the [jdbc module](/doc/jdbc).

Advanced configuration can be added by invoking the ```doWith``` method, adding your own settings to the configuration.

```java
{
  use(new QueryDSL().doWith(conf -> {

    conf.set(...);
  });

}
```

## code generation

This module does not provide code generation for DB mapping classes. To learn how to create QueryDSL mapping classes using Maven, please consult the <a href="http://www.querydsl.com/static/querydsl/latest/reference/html_single/#d0e725">QueryDSL documentation.</a>
