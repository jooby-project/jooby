# cassandra

<a href="http://cassandra.apache.org">The Apache Cassandra</a> database is the right choice when you need scalability and high availability without compromising performance. Linear scalability and proven fault-tolerance on commodity hardware or cloud infrastructure make it the perfect platform for mission-critical data. Cassandra's support for replicating across multiple datacenters is best-in-class, providing lower latency for your users and the peace of mind of knowing that you can survive regional outages.

This module offers <a href="http://cassandra.apache.org">cassandra</a> database features via <a href="http://datastax.github.io/java-driver">Datastax Java Driver</a>.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-cassandra</artifactId>
 <version>{{version}}</version>
</dependency>
```

## exports


* `Cluster`
* `Session`
* `MappingManager`
* `Datastore`
* Optionally a Cassandra [Session Store]({{defdocs}}/cassandra/CassandraSessionStore.html)

## usage

Via connection string:

```java
{
  use(new Cassandra("cassandra://localhost/db"));
}
```

Via connection property:

```java
{
  use(new Cassandra("db"));
}
```

After you install the module `Session`, `MappingManager` and `Datastore` are ready to use.

```java
{
  use(new Cassandra("cassandra://localhost/db"));

  get("/doWithSession", req -> {
    Session session = require(Session.class);
    // work with session
  });

  get("/doWithMappingManager", req -> {
    MappingManager manager = require(MappingManager.class);
    Mapper<Beer> mapper = manager.mapper(Beer.class);
    // work with mapper;
  });

  get("/doWithDatastore", req -> {
    Datastore ds = require(Datastore.class);
    // work with datastore;
  });
}
```

## basic crud

This module exports `MappingManager` so you are free to use a `Mapper`. Jooby also offers the [Datastore]({{defdocs}}/cassandra/Datastore.html) service which basically wrap a `Mapper` and provides query/read operations.

The main advantage of [Datastore]({{defdocs}}/cassandra/Datastore.html) over `Mapper` is that you need just once instance regardless of your number of entities, but also it provides some useful ```query*``` methods.

Here is a basic API on top of [Datastore]({{defdocs}}/cassandra/Datastore.html):

```java
{
  use("/api/beer")
    .post(req -> {
      Datastore ds = require(Datastore.class);
      Beer beer = req.body().to(Beer.class);
      ds.save(beer);
      return beer;
    })
    .get("/:id", req -> {
      Datastore ds = require(Datastore.class);
      Beer beer = ds.get(Beer.class, req.param("id").value());
      return beer;
    })
    .get(req -> {
      Datastore ds = require(Datastore.class);
      return ds.query(Beer.class, "select * from beer").all();
    })
    .delete("/:id", req -> {
      Datastore ds = require(Datastore.class);
      ds.delete(Beer.class, req.param("id").value());
      return Results.noContent();
    });
}
```

Keep in mind your entities must be mapped as usual or as required by `Mapper`. A great example is available <a href="http://datastax.github.io/java-driver/manual/object_mapper/creating">here</a>

## accessors

Accessors provide a way to map custom queries not supported by the default entity mappers. Accessors are created at application startup time via [accessor(Class)]({{defdocs}}/cassandra/Cassandra.html#accesor-java.lang.Class-) method:

```java
{
  use(new Cassandra("cassandra://localhost/db")
   .accessor(UserAccessor.class)
  );

  get("/users", req -> {
    return require(UserAccessor.class).getAll();
  });
}
```

The accessor can be required or injected in a MVC route.

## async

Async? Of course!!! just use the Datastax async API:

```java
{
  use(new Cassandra("cassandra://localhost/db"));

  use("/api/beer")
    .post(req -> {
      Datastore ds = require(Datastore.class);
      Beer beer = req.body().to(Beer.class);
      ds.saveAsync(beer);
      return beer;
    })
    .get("/:id", req -> {
      Datastore ds = require(Datastore.class);
      ListeneableFuture<Beer> beer = ds.getAsync(Beer.class, req.param("id").value());
      return beer;
    })
    .get(req -> {
      Datastore ds = require(Datastore.class);
      return ds.queryAsync(Beer.class, "select * from beer").all();
    })
    .delete("/:id", req -> {
      Datastore ds = require(Datastore.class);
      ds.deleteAsync(Beer.class, req.param("id").value());
      return Results.noContent();
    });
}
```

## multiple contact points

Multiple contact points are separated by a comma:

```java
{
  use(new Casssandra("cassandra://host1,host2/db");
}
```

## advanced configuration

Advanced configuration is available via cluster builder callback:

```java
{
  use(new Casssandra("cassandra://localhost/db")
    .doWithClusterBuilder(builder -> {
      builder.withClusterName("mycluster");
    }));
}
```

Or via cluster callback:

```java
{
  use(new Casssandra("cassandra://localhost/db")
    .doWithCluster(cluster -> {
      Configuration configuration = cluster.getConfiguration();
      // set option
    }));
}
```

{{doc/cassandra/cassandra-session.md}}
