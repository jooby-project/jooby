# jdbi

[JDBI](http://www.jdbi.org/) is a SQL convenience library for Java.

This module extends the [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module so all the services provided by the [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc)  module are inherited.

Before start, make sure you already setup a database connection as described in the [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.

## exports

* [DBI](http://jdbi.org/maven_site/apidocs/org/skife/jdbi/v2/DBI.html)
* [Handles](http://jdbi.org/maven_site/apidocs/org/skife/jdbi/v2/Handle.html)
* SQL Objects (a.k.a DAO).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jdbi</artifactId>
  <version>1.0.1</version>
</dependency>
```

## usage

```java
{
  use(new Jdbi());

  get("/", req -> {
    DBI dbi = require(DBI.class);
    // ... work with dbi
  });

  get("/handle", req -> {
    try (Handle handle = require(Handle.class)) {
      // ... work with dbi handle
    }
  });
}
```

## sql objects

It is pretty straightforward (too):

```java

public interface MyRepository extends Closeable {
  @SqlUpdate("create table something (id int primary key, name varchar(100))")
  void createSomethingTable();

  @SqlUpdate("insert into something (id, name) values (:id, :name)")
  void insert(@ind("id") int id, @Bind("name") String name);
 
  @SqlQuery("select name from something where id = :id")
  String findNameById(@Bind("id") int id);
}

...
{
  use(new Jdbi());

  get("/handle", req -> {
    try (MyRepository h = require(MyRepository.class)) {
      h.createSomethingTable();

      h.insert(1, "Jooby");

      String name = h.findNameById(1);

      return name;
    }
  });
}
```

## configuration

If you need to configure and/or customize a [DBI](http://jdbi.org/maven_site/apidocs/org/skife/jdbi/v2/DBI.html) instance, just do:

```java
{
  use(new Jdbi().doWith((dbi, config) -> {
    // set custom option
  }));
}
```

See [JDBI](http://www.jdbi.org/) for a detailed usage.
