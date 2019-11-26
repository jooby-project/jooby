[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-jdbi/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-jdbi/1.6.5)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jdbi.svg)](https://javadoc.io/doc/org.jooby/jooby-jdbi/1.6.5)
[![jooby-jdbi website](https://img.shields.io/badge/jooby-jdbi-brightgreen.svg)](http://jooby.org/doc/jdbi)
# jdbi

[JDBI](http://www.jdbi.org/) is a SQL convenience library for Java.

> **DEPRECATED**: try the [jdbi3](https://github.com/jooby-project/jooby/tree/master/jooby-jdbi3) module.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.

## exports

* [DBI](http://jdbi.org/maven_site/apidocs/org/skife/jdbi/v2/DBI.html)
* [Handles](http://jdbi.org/maven_site/apidocs/org/skife/jdbi/v2/Handle.html)
* SQL Objects (a.k.a DAO).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jdbi</artifactId>
  <version>1.6.5</version>
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
