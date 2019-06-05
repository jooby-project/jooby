[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-ebean/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-ebean/1.6.2)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-ebean.svg)](https://javadoc.io/doc/org.jooby/jooby-ebean/1.6.2)
[![jooby-ebean website](https://img.shields.io/badge/jooby-ebean-brightgreen.svg)](http://jooby.org/doc/ebean)
# ebean

Object-Relational-Mapping via [Ebean ORM](http://ebean-orm.github.io). It configures and exports ```EbeanServer``` instances.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.
 
## exports

* ```EbeanServer```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-ebean</artifactId>
  <version>1.6.2</version>
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

## ebean.conf
These are the default properties for ebean:

```properties
ebean.defaultServer = true

ebean.register = true

ebean.ddl.generate=false

ebean.ddl.run=false

ebean.debug.sql=true

ebean.debug.lazyload=false

ebean.disableClasspathSearch = true

# -------------------------------------------------------------

# Transaction Logging

# -------------------------------------------------------------

# Use java util logging to log transaction details

ebean.loggingToJavaLogger=false

# General logging level: (none, explicit, all)

ebean.logging=all

# Sharing log files: (none, explicit, all)

ebean.logging.logfilesharing=all

# location of transaction logs

ebean.logging.directory=logs

# Specific Log levels (none, summary, binding, sql)

ebean.logging.iud=sql

ebean.logging.query=sql

ebean.logging.sqlquery=sql

ebean.logging.txnCommit=none
```
