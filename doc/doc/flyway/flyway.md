# flyway

Evolve your database schema easily and reliably across all your instances.

This module run {{flyway}} on startup and apply database migrations.

> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module to acquire a database connection.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-flyway</artifactId>
  <version>{{version}}</version>
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

{{flyway}} looks for migration scripts at the ```db/migration``` classpath location.
We recommend to use [Semantic versioning](http://semver.org) for naming the migration scripts:

```
v0.1.0_My_description.sql
v0.1.1_My_small_change.sql
```

## commands
It is possible to run {{flyway}} commands on startup, default command is: ```migrate```.

If you need to run multiple commands, set the ```flyway.run``` property:

```properties
flyway.run = [clean, migrate, validate, info]
```

## configuration

Configuration is done via ```application.conf``` under the ```flyway.*``` path.
There are some defaults setting that you can see in the appendix section.


For more information, please visit the {{flyway}} site.

{{appendix}}
