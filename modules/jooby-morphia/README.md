[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-morphia/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-morphia/1.6.3)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-morphia.svg)](https://javadoc.io/doc/org.jooby/jooby-morphia/1.6.3)
[![jooby-morphia website](https://img.shields.io/badge/jooby-morphia-brightgreen.svg)](http://jooby.org/doc/morphia)
# morphia

Extends the [mongodb](https://github.com/jooby-project/jooby/tree/master/jooby-mongodb) module with object-document mapping via [Morphia](https://github.com/mongodb/morphia).

## exports

* [Morphia](https://rawgit.com/wiki/mongodb/morphia/javadoc/0.111/org/mongodb/morphia/Morphia.html)
* [Datastore](https://rawgit.com/wiki/mongodb/morphia/javadoc/0.111/org/mongodb/morphia/Datastore.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-morphia</artifactId>
  <version>1.6.3</version>
</dependency>
```

## getting started

Before you start make sure to read the doc from [mongodb](https://github.com/jooby-project/jooby/tree/master/jooby-mongodb) module. This module extends [mongodb](https://github.com/jooby-project/jooby/tree/master/jooby-mongodb) module.

## usage

application.conf:

```properties
db = "mongodb://localhost/mydb"
```

```java
{
  use(new Monphia());

  get("/", req -> {
    Datastore ds = require(Datastore.class);
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
    })
  );
}
```

For more detailed information, check [here]([Morphia](https://github.com/mongodb/morphia)/wiki/MappingObjects)

### datastore callback

The [Datastore](https://rawgit.com/wiki/mongodb/morphia/javadoc/0.111/org/mongodb/morphia/Datastore.html) callback is executed only once, it's perfect for checking indexes:

```java
{
  use(new Monphia()
    .doWith(datastore -> {
      // work with datastore
      datastore.ensureIndexes();
      datastore.ensureCap();
    })
  );
}
```

For more detailed information, check [here]([Morphia](https://github.com/mongodb/morphia)/wiki/Datastore#ensure-indexes-and-caps)

### auto-incremental ID
This modules comes with auto-incremental ID generation, usage:

```java
{
  use(new Monphia().with(IdGen.GLOBAL); // or IdGen.LOCAL
}
```

ID must be of type: ```Long``` and annotated with [GeneratedValue](/apidocs/org/jooby/mongodb/GeneratedValue.html):

```java
@Entity
public class MyEntity {
  @Id @GeneratedValue Long id;
}
```

There two ID gen:

* GLOBAL: generates a global and unique ID regardless of entity type
* LOCAL: generates an unique ID per entity type

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

> NOTE: ONLY Constructor injection is supported.

That's all folks! Enjoy it!!
