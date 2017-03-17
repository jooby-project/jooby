# requery

Safe, clean and efficient database access via <a href="https://github.com/requery/requery">Requery.</a>

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-requery</artifactId>
 <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  use(new Jdbc());

  use(new Requery(Models.DEFAULT));

  get("/people", () -> {

    EntityStore store = require(EntityStore.class);
    return store.select(Person.class)
       .where(Person.ID.eq(req.param("id").intValue()))
       .get()
       .first();
  });

}
```

This module requires a `DataSource` connection. That's why you also need the [jdbc](/doc/jdbc) module.

## code generation

### maven

We do provide code generation via Maven profile. All you have to do is to write a ```requery.activator``` file inside the ```src/etc``` folder. File present triggers requery annotation processor and generated contents.

Generated content can be found at: ```target/generated-sources```. You can change the default output location by setting the build property ```requery.output``` in your ```pom.xml```.

### gradle

Please refer to <a href="https://github.com/requery/requery/wiki/Gradle-&amp;-Annotation-processing#annotation-processing">requery documentation</a> for Gradle support.

## schema generation

```java
{
  use(new Requery(Models.DEFAULT)
    .schema(TableCreationMode.DROP_CREATE)
  );

}
```

Optionally, schema generation could be set from `.conf` file via `requery.schema` property.

### listeners

```java
public class MyListener implements EntityStateListener<Person>
  @Inject
  public MyListener(Dependency dep) {
    this.dep = dep;
  }
  ...
}

{
  use(new Requery(Models.DEFAULT)
    .entityStateListener(MyListener.class)
  );
}
```

Support for `TransactionListener` and `StatementListener` is also provided:

```java
{
  use(new Requery(Models.DEFAULT)
    .statementListener(MyStatementListener.class)
    .transactionListener(TransactionListener.class)
  );

}
```

You can add as many listener as you need. Each listener will be created by ```Guice```.

## Type-Safe injection

If you love `DAO` like classes, we are happy to tell you that it you easily inject type-safe `EntityStore`:

```java
public class PersonDAO {
  private EntityStore<Persistable, Person> store;

  @Inject
  public PersonDAO(EntityStore<Persistable, Person> store) {
    this.store = store;
  }
```

Please note we don't inject a `raw` `EntityStore`. Instead we ask for a `Person` `EntityStore`. You can safely inject a `EntityStore` per each of your domain objects.

## async and reactive idioms

Rxjava:

```java
{
  use(Requery.reactive(Models.DEFAULT));

  get("/", () -> {
    ReactiveEntityStore store = require(ReactiveEntityStore.class);
    // work with reactive store
  });

}
```

Reactor:

```java
{
  use(Requery.reactor(Models.DEFAULT));

  get("/", () -> {
    ReactorEntityStore store = require(ReactorEntityStore.class);
    // work with reactor store
  });

}
```

Java 8:

```java
{
  use(Requery.completionStage(Models.DEFAULT));

  get("/", () -> {
    CompletionStageEntityStore store = require(CompletionStageEntityStore.class);
    // work with reactor store
  });

}
```

## advanced configuration

Advanced configuration is available via callback function:

```java
{
  use(new Requery(Models.DEFAULT)
    .doWith(builder -> {
      builder.useDefaultLogging();
      ....
    })
  );

}
```

That's all folks!!
