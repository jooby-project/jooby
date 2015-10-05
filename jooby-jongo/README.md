# jongo module
Exposes [Jongo](http://jongo.org) instances to a default database. Or [JongoFactory](/apidocs/org/jooby/mongodb/JongoFactory.html) to use alternative databases.

Please note, this module depends on: [mongodb](/doc/mongodb) module.

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jongo</artifactId>
  <version>0.11.1</version>
</dependency>
```

## usage

```java
{
  use(new Mongodb());
  use(new Jongoby());

  get("/", req -> {
    Jongo jongo = req.require(Jongo.class);
    // work with jongo...
  });
}
```

Previous example will give you a [Jongo](http://jongo.org) instance connected to the default database, provided by the [mongodb](/doc/mongodb)  module.

Access to alternate database is provided via: [JongoFactory](/apidocs/org/jooby/mongodb/JongoFactory.html).

```java
{
  use(new Mongodb());
  use(new Jongoby());

  get("/", req -> {
    Jongo jongo = req.require(JongoFactory.class).get("alternate-db");
    // work with jongo...
  });
}
```

For more information, please visit the [Jongo](http://jongo.org) web site.

Happy coding!!!
