# jooby-jdbi

Exposes [DBI](http://jdbi.org/maven_site/apidocs/org/skife/jdbi/v2/DBI.html), [Handles](http://jdbi.org/maven_site/apidocs/org/skife/jdbi/v2/Handle.html) and SQL Objects (a.k.a DAO). This module extends the 
[jdbc](/doc/jdbc) module so all the services
provided by the [jdbc](/doc/jdbc) 
module are inherited.

Before start, make sure you already setup a database connection as described in the 
[jdbc](/doc/jdbc) module.

See [JDBI](http://www.jdbi.org/) for a detailed usage.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jdbi</artifactId>
  <version>0.7.0</version>
</dependency>
```

## usage

It is pretty straightforward:

```java
{
  use(new Jdbi());

  get("/", req -> {
    DBI dbi = req.require(DBI.class);
    // ... work with dbi
  });

  get("/handle", req -> {
    try (Handle handle = req.require(Handle.class)) {
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
    try (MyRepository h = req.require(MyRepository.class)) {
      h.createSomethingTable();

      h.insert(1, "Jooby");

      String name = h.findNameById(1);

      return name;
    }
  });
}
```

## auto-magic in-clause expansion

This modules support expansion of in-clauses and/or expansion of multi-value arguments (iterables and arrays).

```java
List<Integer> ids = Lists.newArrayList(1, 2, 3);
h.createQuery("select * from something where id in (:ids)")
  .bind("ids", ids)
  .list();
```

The SQL expression:

```sql
select * from something where id in (:ids)
```

Will be expanded/translated to:

```sql
select * from something where id in (?, ?, ?)
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

That's all folks! Enjoy it!!!
