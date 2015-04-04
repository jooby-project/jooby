# jooby-morphia

Extends the [mongodb](https://github.com/jooby-project/jooby/tree/master/jooby-mongodb) module with object-document mapping via [Morphia](https://github.com/mongodb/morphia).

Exposes a [Morphia](https://rawgit.com/wiki/mongodb/morphia/javadoc/0.111/org/mongodb/morphia/Morphia.html) and [Datastore](https://rawgit.com/wiki/mongodb/morphia/javadoc/0.111/org/mongodb/morphia/Datastore.html) services.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-morphia</artifactId>
  <version>0.4.2.1</version>
</dependency>
```

## getting started

Before you start make sure to read the doc from [mongodb](https://github.com/jooby-project/jooby/tree/master/jooby-mongodb) module. This module extends [mongodb](https://github.com/jooby-project/jooby/tree/master/jooby-mongodb) module.

## usage

application.conf:

```properties
db = "mongo://localhost/mydb"
```

```java
{
  use(new Monphia());

  get("/", req -> {
    Datastore ds = req.require(Datastore.class);
    // work with mydb datastore
  });
}
```

## options

### morphia callback

The [Morphia](https://rawgit.com/wiki/mongodb/morphia/javadoc/0.111/org/mongodb/morphia/Morphia.html) callback let you map classes and/or set mapper options.

```java
{
  use(new Monphia()
    .doWith((morphia, config) -> {
      // work with morphia
      morphia.map(MyObject.class);
    });
  );
}
```

For more detailed information, check [here](https://github.com/mongodb/morphia/wiki/MappingObjects)

### datastore callback

This [Datastore](https://rawgit.com/wiki/mongodb/morphia/javadoc/0.111/org/mongodb/morphia/Datastore.html) callback is executed only once, it's perfect for checking indexes:

```java
{
  use(new Monphia()
    .doWith(datastore -> {
      // work with datastore
      datastore.ensureIndexes();
      datastore.ensureCap();
    });
  );
}
```

For more detailed information, check [here](https://github.com/mongodb/morphia/wiki/Datastore#ensure-indexes-and-caps)

## entity listeners

[Guice](https://github.com/google/guice) will create and inject entity listeners (when need it).


```java
public class MyListener {

  private Service service;

  @Inject
  public MyListener(Service service) {
    this.service = service;
  }

  @PreLoad void preLoad(MyObject object) {
    service.doSomething(object);
  }

}
```

NOTE: ONLY Constructor injection is supported.

That's all folks! Enjoy it!!
