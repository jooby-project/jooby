# jooby-morphia

Extends the [mongodb]({{gh}}/jooby-mongodb) module with object-document mapping via [Morphia]({{morphia}}).

Exposes a [Morphia]({{morphiaapi}}/Morphia.html) and [Datastore]({{morphiaapi}}/Datastore.html) services.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-morphia</artifactId>
  <version>{{version}}</version>
</dependency>
```

## getting started

Before you start make sure to read the doc from [mongodb]({{gh}}/jooby-mongodb) module. This module extends [mongodb]({{gh}}/jooby-mongodb) module.

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

The [Morphia]({{morphiaapi}}/Morphia.html) callback let you map classes and/or set mapper options.

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

For more detailed information, check [here]({{morphia}}/wiki/MappingObjects)

### datastore callback

This [Datastore]({{morphiaapi}}/Datastore.html) callback is executed only once, it's perfect for checking indexes:

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

For more detailed information, check [here]({{morphia}}/wiki/Datastore#ensure-indexes-and-caps)

## entity listeners

{{guice}} will create and inject entity listeners (when need it).


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
