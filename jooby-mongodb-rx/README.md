# mongodb-rx

<a href="http://mongodb.github.io/mongo-java-driver-rx/">MongoDB RxJava Driver: </a> provides composable asynchronous and event-based observable sequences for MongoDB.

A MongoDB based driver providing support for <a href="http://reactivex.io">ReactiveX (Reactive Extensions)</a> by using the <a href="https://github.com/ReactiveX/RxJava">RxJava library</a>. All database calls return an <a href="http://reactivex.io/documentation/observable.html">Observable</a> allowing for efficient execution, concise code, and functional composition of results.


> This module depends on [rx module](/doc/rxjava), please read the documentation of [rx module](/doc/rxjava) before using ```mongodb-rx```.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-mongodb-rx</artifactId>
 <version>1.0.0.CR5</version>
</dependency>
```

## exports

* ```MongoClient``` 
* ```MongoDatabase``` (when mongo connection string has a database) 
* ```MongoCollection``` (when mongo connection string has a collection) 
* [Route.Mapper](/apidocs/org/jooby/Route.Mapper.html) for mongo observables 

## usage

```java
import org.jooby.rx.Rx;
import org.jooby.mongodb.MongoRx;

{
  // required
  use(new Rx());

  use(new MongoRx());

  get("/", req -> {
    MongoClient client = req.require(MongoClient.class);
    // work with client:
  });

}
```

The ```mongo-rx``` module connects to ```mongodb://localhost```. You can change the connection string by setting the ```db``` property in your ```application.conf``` file:

```
db = "mongodb://localhost/mydb"
```

Or at creation time:

```java
{
  // required
  use(new Rx());

  use(new MongoRx("mongodb://localhost/mydb"));
}
```

If your connection string has a database, then you can require a ```MongoDatabase``` object:

```java
{
  // required
  use(new Rx());

  use(new MongoRx("mongodb://localhost/mydb"));

  get("/", req -> {

    MongoDatabase mydb = req.require(MongoDatabase.class);
    return mydb.listCollections();
  });

}
```

And if your connection string has a collection:

```java
{
  // required
  use(new Rx());

  use(new MongoRx("mongodb://localhost/mydb.mycol"));

  get("/", req -> {
    MongoCollection mycol = req.require(MongoCollection.class);
    return mycol.find();
  });

}
```

## query the collection

The module let you return ```MongoObservable``` from routes:

```java
{
  // required
  use(new Rx());

  use(new MongoRx());

  get("/pets", req -> {
    MongoDatabase db = req.require(MongoDatabase.class);
    return db.getCollection("pets")
       .find();
  });

}
```

Previous example will list all the ```Pets``` from a collection. Please note you don't have to deal with ```MongoObservable```, instead the module converts ```MongoObservable``` to Jooby [async semantics](/doc/async/).

## multiple databases

Multiple databases are supported by adding multiple [MongoRx](/apidocs/org/jooby/mongo/MongoRx.html) instances to your application:

```java
{
  // required
  use(new Rx());

  use(new MongoRx("db1"));

  use(new MongoRx("db2"));

  get("/do-with-db1", req -> {
    MongoDatabase db1 = req.require("db1", MongoDatabase.class);
    // work with db1
  });

  get("/do-with-db2", req -> {
    MongoDatabase db2 = req.require("db2", MongoDatabase.class);
    // work with db2
  });

}
```

The keys ```db1``` and ```db2``` are connection strings in your ```application.conf```:

```
db1 = "mongodb://localhost/db1"

db2 = "mongodb://localhost/db2"
```

## observable adapter

```ObservableAdapter``` provides a simple way to adapt all Observables returned by the driver. On such use case might be to use a different Scheduler after returning the results from MongoDB therefore freeing up the connection thread.

```java
{
  // required
  use(new Rx());

  use(new MongoRx()
      .observableAdapter(o -> o.observeOn(Schedulers.io())));
}
```

Any computations on Observables returned by the ```MongoDatabase``` or ```MongoCollection``` will use the IO scheduler, rather than blocking the MongoDB Connection thread.

Please note the [observableAdapter(Function)](/apidocs/org/jooby/mongodb/MongoRx.html#observableAdapter-java.util.function.Function-) works if (and only if) your connection string points to a database. It won't work on ```mongo://localhost``` connection string because there is no database in it.

## driver options

Driver options are available via <a href="https://docs.mongodb.com/v3.0/reference/connection-string/">connection string</a>.

It is also possible to configure specific options:

```
db = "mongodb://localhost/pets"

mongo {
  readConcern: default
  writeConcern: ACKNOWLEDGED

  cluster {
    replicaSetName: name
    requiredClusterType: REPLICA_SET
  }

  pool {
    maxSize: 100
    minSize: 10
  }

}
```

Each option matches a ```MongoClientSettings``` method.
