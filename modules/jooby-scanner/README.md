# scanner

<a href="https://github.com/lukehutch/fast-classpath-scanner">FastClasspathScanner</a> is an uber-fast, ultra-lightweight classpath scanner for Java, Scala and other JVM languages.

This module provides `class-path` scanning services for `MVC routes`, `modules`, `services` and `applications`.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-scanner</artifactId>
 <version>1.1.3</version>
</dependency>
```

## usage

```java
{
  use(new Scanner());
}
```

This modules scan the application `class-path` and automatically discover and register ```MVC routes/controllers```, [Jooby.Module](/apidocs/org/jooby/Jooby.Module.html) and [Jooby.Module](/apidocs/org/jooby/Jooby.html) applications.

## scan spec

By default, it scans the application package:

> That's the package where your bootstrap application belong to.

Multi-package scanning is available at construction time:

```java
{
  use(new Scanner("foo", "bar"));
}
```

More <a href="https://github.com/lukehutch/fast-classpath-scanner/wiki/2.-Constructor#specifying-more-complex-scanning-criteria">complex scanning criteria</a> are supported, not just package. To see what else is available refer to <a href="https://github.com/lukehutch/fast-classpath-scanner/wiki/2.-Constructor#specifying-more-complex-scanning-criteria">documentation</a>.

## services

The next example scans and initialize any class in the application package annotated with ```Named```:

```java
{
  use(new Scanner()
    .scan(Named.class)
  );
}
```

The next example scans and initialize any class in the application package that `implements/extends` `MyService`:

```java
{
  use(new Scanner()
    .scan(MyService.class)
  );
}
```

Guava `Services` are also supported:

```java
{
  use(new Scanner()
    .scan(com.google.common.util.concurrent.Service.class)
  );

  get("/guava", req -> {
    ServiceManager sm = require(ServiceManager.class);
    ...
  });

}
```

They are added to `ServiceManager` and `started/stopped` automatically.

Raw/plain Guice `Module` are supported too:

```java
{
  use(new Scanner()
    .scan(Module.class)
  );

}
```

Of course, you can combine two or more strategies:

```java
{
  use(new Scanner()
    .scan(MyService.class)
    .scan(Named.class)
    .scan(Singleton.class)
    .scan(MyAnnotation.class)
  );

}
```

In all cases, services are created as ```singleton``` and `started/stopped` automatically when `PostConstruct` and `PreDestroy` annotations are present.
