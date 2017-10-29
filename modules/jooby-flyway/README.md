[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-flyway/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-flyway)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-flyway.svg)](https://javadoc.io/doc/org.jooby/jooby-flyway/1.2.1)
[![jooby-flyway website](https://img.shields.io/badge/jooby-flyway-brightgreen.svg)](http://jooby.org/doc/flyway)
# flyway

Evolve your Database Schema easily and reliably across all your instances.

This module run [Flyway](http://flywaydb.org) on startup and apply database migration.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module so all the services provided by the [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-flyway</artifactId>
  <version>1.2.1</version>
</dependency>
```

## usage

```java
{
  use(new Jdbc());

  use(new Flywaydb());
}
```

If for any reason you need to maintain two or more databases:

```java
{
  use(new Jdbc("db1"));
  use(new Flywaydb("db1"));

  use(new Jdbc("db2"));
  use(new Flywaydb("db2"));
}
```

## migration scripts

[Flyway](http://flywaydb.org) looks for migration scripts at the ```db/migration``` classpath location.
We recommend to use [Semantic versioning](http://semver.org) for naming the migration scripts:

```
v0.1.0_My_description.sql
v0.1.1_My_small_change.sql
```

## commands
It is possible to run [Flyway](http://flywaydb.org) commands on startup, default command is: ```migrate```.

If you need to run multiple commands, set the ```flyway.run``` property:

```properties
flyway.run = [clean, migrate, validate, info]
```

## configuration

Configuration is done via ```application.conf``` under the ```flyway.*``` path.
There are some defaults setting that you can see in the appendix.


For more information, please visit the [Flyway](http://flywaydb.org) site.
