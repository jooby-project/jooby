# ebean

Object-Relational-Mapping via {{ebean}}. It configures and exports ```EbeanServer``` instances.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.
 
## exports

* ```EbeanServer```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-ebean</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  use(new Jdbc());

  use(new Ebeanby().doWith((ServerConfig conf) -> {
   conf.addClass(Pet.class);
  }));

  get("/pets", req -> {
    EbeanServer ebean = require(EbeanServer.class);
    return ebean.createQuery(Pet.class)
       .findList();
  });
}
```

## enhancement

The enhancement process comes in two flavors:

1) Runtime: via a JVM Agent

2) Build time: via Maven plugin

### recommended setup

The recommended setup consist of setting up both: **runtime and build time enhancement**.

The runtime enhancer increases developer productivity, it let you start your app from IDE
and/or ```mvn jooby:run```. All you have to do is to add the agent dependencies to your
classpath:

```xml
<dependency>
  <groupId>org.avaje.ebeanorm</groupId>
  <artifactId>avaje-ebeanorm-agent</artifactId>
  <scope>test</scope>
</dependency>

<dependency>
  <groupId>org.avaje</groupId>
  <artifactId>avaje-agentloader</artifactId>
  <scope>test</scope>
</dependency>
```

Did you see the ```test scope```? We don't want to use the runtime enhancer while
running in ```prod```. Instead, we want to use the build time enhancer.

All you have to do is to add ```avaje-ebeanorm-mavenenhancer``` to your ```pom.xml``` as described
in the [official doc](http://ebean-orm.github.io/docs#enhance_maven).

Alternative, and because we want to keep our ```pom.xml``` small, you can drop a ```ebean.activator```
file inside the ```src/etc``` folder. The presence of the file ```src/etc/ebean.activator```
will trigger the ```avaje-ebeanorm-mavenenhancer``` plugin.

## configuration

Configuration is done via ```.conf```, for example:

```properties
ebean.ddl.generate=false
ebean.ddl.run=false

ebean.debug.sql=true
ebean.debug.lazyload=false

ebean.disableClasspathSearch = false
```

Or programmatically:

```java
{
  use(new Ebeanby().doWith(ebean -> {
    ebean.setDisableClasspathSearch(false);
  }));
}
```

## starter project

We do provide an [ebean-starter](https://github.com/jooby-project/ebean-starter) project. Go and [fork it](https://github.com/jooby-project/ebean-starter).

That's all folks!!

{{appendix}}
