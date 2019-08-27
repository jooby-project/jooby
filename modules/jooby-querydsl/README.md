[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-querydsl/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-querydsl/1.6.4)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-querydsl.svg)](https://javadoc.io/doc/org.jooby/jooby-querydsl/1.6.4)
[![jooby-querydsl website](https://img.shields.io/badge/jooby-querydsl-brightgreen.svg)](http://jooby.org/doc/querydsl)
# queryDSL

SQL abstraction provided by <a href="http://www.querydsl.com">QueryDSL</a> using plain JDBC underneath.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.

## exports

* ```SQLQueryFactory```

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-querydsl</artifactId>
 <version>1.6.4</version>
</dependency>
```

## usage

```java
import org.jooby.querydsl.QueryDSL;
{
  use(new Jdbc());
  use(new QueryDSL());

  get("/my-api", req -> {

    SQLQueryFactory queryFactory = require(SQLQueryFactory.class);
    // Do something with the database
    ...
  });

}
```

## dialects

Dialect is detected automatically and usually you don't need to do anything. But if the default dialect detector doesn't work and/or you have a custom dialect:

```java
{
  use(new Jdbc());
  use(new QueryDSL().with(new MyCustomTemplates());
}
```

## multiple databases

```java
import org.jooby.querydsl.QueryDSL;
{
  use(new Jdbc("db.main"));
  use(new QueryDSL("db.main"));

  use(new Jdbc("db.aux"));
  use(new QueryDSL("db.aux"));

  get("/my-api", req -> {
    SQLQueryFactory queryFactory = require("db.main", SQLQueryFactory.class);
    // Do something with the database
  });
}
```

## advanced configuration

This module builds QueryDSL SQLQueryFactories on top of a default ```Configuration``` object, a ```SQLTemplates``` instance and a ```javax.sql.DataSource``` from the [jdbc module](/doc/jdbc).

Advanced configuration can be added by invoking the ```doWith``` method, adding your own settings to the configuration.

```java
{
  use(new Jdbc());
  use(new QueryDSL().doWith(conf -> {
    conf.set(...);
  });
}
```

## code generation

This module does not provide code generation for DB mapping classes. To learn how to create QueryDSL mapping classes using Maven, please consult the <a href="http://www.querydsl.com/static/querydsl/latest/reference/html_single/#d0e725">QueryDSL documentation.</a>
