# couchbase

<a href="http://www.couchbase.com">Couchbase</a> is a NoSQL document database with a distributed architecture for performance, scalability, and availability. It enables developers to build applications easier and faster by leveraging the power of SQL with the flexibility of JSON.

This module provides <a href="http://www.couchbase.com">couchbase</a> access via <a href="https://github.com/couchbase/couchbase-java-client">Java SDK</a>

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-couchbase</artifactId>
 <version>1.1.3</version>
</dependency>
```

## exports

* `CouchbaseEnvironment` 
* `CouchbaseCluster` 
* `AsyncBucket` 
* `Bucket` 
* `AsyncDatastore` 
* `Datastore` 
* `AsyncRepository` 
* `Repository` 
* Optionally a couchbase `Session.Store` 

## usage

Via couchbase connection string:

```java
{
  use(new Couchbase("couchbase://locahost/beers"));

  get("/", req -> {
    Bucket beers = require(Bucket.class);
    // do with beer bucket
  });
}
```

Via property:

```java
{
  use(new Couchbase("db"));

  get("/", req -> {
    Bucket beers = require(Bucket.class);
    // do with beer bucket
  });
}
```

The ```db``` property is defined in the ```application.conf``` file as a couchbase connection string.

## create, read, update and delete

Jooby provides a more flexible, easy to use and powerful CRUD operations via [AsyncDatastore]({{defdocs/couchbase/AsyncDatastore.html)/[AsyncDatastore]({{defdocs/couchbase/Datastore.html) objects.

```java
import org.jooby.couchbase.Couchbase;
import org.jooby.couchbase.N1Q;
...
{
  use(new Couchbase("couchbase://localhost/beers"));

  use("/api/beers")
    /** List beers */
    .get(req -> {
      Datastore ds = require(Datastore.class);
      return ds.query(N1Q.from(Beer.class));
    })
    /** Create a new beer */
    .post(req -> {
      Datastore ds = require(Datastore.class);
      Beer beer = req.body().to(Beer.class);
      return ds.upsert(beer);
    })
    /** Get a beer by ID */
    .get(":id", req -> {
      Datastore ds = require(Datastore.class);
      return ds.get(Beer.class, req.param("id").value());
    })
    /** Delete a beer by ID */
    .delete(":id", req -> {
      Datastore ds = require(Datastore.class);
      return ds.delete(Beer.class, req.param("id").value());
    });
}
```

As you can see benefits over `AsyncRepository`/`Repository` are clear: you don't have to deal with `EntityDocument` just send or retrieve POJOs.

Another good reason is that [AsyncDatastore]({{defdocs/couchbase/AsyncDatastore.html)/[AsyncDatastore]({{defdocs/couchbase/Datastore.html) supports query operations with POJOs too.

### design and implementation choices

The [AsyncDatastore]({{defdocs/couchbase/AsyncDatastore.html)/[AsyncDatastore]({{defdocs/couchbase/Datastore.html) simplifies a lot the integration between Couchbase and POJOs. This section describes how IDs are persisted and how mapping works.

A document persisted by an [AsyncDatastore]({{defdocs/couchbase/AsyncDatastore.html)/[AsyncDatastore]({{defdocs/couchbase/Datastore.html) looks like:

```java
{
  "model.Beer::1": {
    "name": "IPA",
    ...
    "id": 1,
    "_class": "model.Beer"
  }
}
```

The couchbase document ID contains the fully qualified name of the class, plus ```::``` plus the entity/business ID: ```mode.Beer::1```.

The business ID is part of the document, here the business ID is: ```id:1```. The business ID is required while creating POJO from couchbase queries.

Finally, a ```_class``` attribute is also part of the document. It contains the fully qualified name of the class and its required while creating POJO from couchbase queries.

### mapping pojos

Mapping between document/POJOs is done internally with a custom `EntityConverter`. The `EntityConverter` uses an internal copy of ```ObjectMapper``` object from [Jackson](https://github.com/FasterXML/jackson). So in **theory** anything that can be handle by [Jackson](https://github.com/FasterXML/jackson) will work.

In order to work with a POJO, you must defined an ID. There are two options:

* Add an ```id``` field to your POJO:

```java

public class Beer {
  private String id;
}
```

* Use a business name (not necessarily id) and add ```Id``` annotation:

```java
import import com.couchbase.client.java.repository.annotation.Id;
...
public class Beer {

  @Id
  private String beerId;
}
```

Auto-increment IDs are supported via [GeneratedValue](/apidocs/org/jooby/couchbase/GeneratedValue.html):

```java
public class Beer {
  private Long id;
}
```

Auto-increment IDs are generated using {@link Bucket#counter(String, long, long)} function and they must be ```Long```. We use the POJO fully qualified name as counter ID.

Any other field will be mapped too, you don't need to annotate an attribute with {@link Field}. If you don't want to persist an attribute, just ad the ```transient``` Java modifier:

```java
public class Beer {
  private String id;

  private transient ignored;
}
```

Keep in mind that if you annotated your POJO with [Jackson](https://github.com/FasterXML/jackson) annotations they will be ignored... because we use an internal copy of [Jackson](https://github.com/FasterXML/jackson) that comes with ```Java Couchbase SDK```

## reactive usage

Couchbase SDK allows two programming model: ```blocking``` and ```reactive```. We already see how to use the blocking API, now is time to see how to use the ```reactive``` API:

```java
{
  use(new Couchbase("couchbase://localhost/beers"));

  get("/", req -> {
    AsyncBucket bucket = require(AsyncBucket.class);
    // do with async bucket ;)
  });
}
```

Now, what to do with Observables? Do we have to block? Not necessarily if we use the [Rx](https://github.com/jooby-project/jooby/tree/master/jooby-rxjava) module:

```java
...
import org.jooby.rx.Rx;
...
{
  // handle observable route responses
  use(new Rx());

  use(new Couchbase("couchbase://localhost/beers"));

  get("/api/beer/:id", req -> {

    AsyncDatastore ds = require(AsyncDatastore.class);
    String id = req.param("id").value();
    Observable<Beer> beer = ds.get(Beer.class, id);
    return beer;
  });
}
```

The [Rx](https://github.com/jooby-project/jooby/tree/master/jooby-rxjava) module deal with observables so you can safely return `Observable` from routes (Jooby rocks!).

## multiple buckets

If for any reason your application requires more than 1 bucket... then:

```java
{
  use(new Couchbase("couchbase://localhost/beers")

    .buckets("extra-bucket"));
  get("/", req -> {

    Bucket bucket = require("beers", Bucket.class);
    Bucket extra = require("extra-bucket", Bucket.class);
  });
}
```

Easy, right? Same principle apply for Async* objects

## multiple clusters

Again, if for any reason your application requires multiple clusters... then:

```java
{
  CouchbaseEnvironment env = ...;

  use(new Couchbase("couchbase://192.168.56.1")
     .environment(env));

  use(new Couchbase("couchbase://192.168.57.10")
     .environment(env));
}
```

You must shared the `CouchbaseEnvironment` as documented <a href="http://developer.couchbase.com/documentation/server/4.0/sdks/java-2.2/managing-connections.html#story-h2-4">here</a>.

## options

### bucket password

You can set a global bucket password via: ```couchbase.bucket.password``` property, or local bucket password (per bucket) via ```couchbase.bucket.[name].password``` property.

### environment configuration

Environment configuration is available via: ```couchbase.env``` namespace, here is an example on how to setup ```kvEndpoints```:

```
couchbase.env.kvEndpoints = 3
```

### cluster manager

A `ClusterManager` service is available is you set an cluster username and password:

```
couchbase.cluster.username = foo
couchbase.cluster.password = bar
```



# couchbase session store

A [Session.Store](/apidocs/org/jooby/couchbase/CouchbaseSessionStore) powered by <a href="http://www.couchbase.com">Couchbase</a>.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-couchbase</artifactId>
 <version>1.1.3</version>
</dependency>
```

## usage

```java
{
  use(new Couchbase("couchbase://localhost/bucket"));

  session(CouchbaseSessionStore.class);

  get("/", req -> {
    Session session = req.session();
    session.put("foo", "bar");
    ..
  });
}
```

Session data is persisted in Couchbase and document looks like:

```
{
  "session::{SESSION_ID}": {
    "foo": "bar"
  }
}
```

## options

### timeout

By default, a session will expire after ```30 minutes```. Changing the default timeout is as simple as:

```properties
# 8 hours

session.timeout = 8h

# 15 seconds

session.timeout = 15

# 120 minutes

session.timeout = 120m
```

Expiration is done via Couchbase expiry/ttl option.

If no timeout is required, use ```-1```.

### custom bucket

The session document are persisted in the application/default bucket, if you need/want a different bucket then use {@link Couchbase#sessionBucket(String)}, like:

```java
{
  use(
      new Couchbase("couchbase://localhost/myapp")
          .sessionBucket("session")
  );

}
```
