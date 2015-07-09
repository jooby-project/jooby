# jooby-flyway

Run database migrations on startup and exposes a {{flyway}} instance.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-flywaydb</artifactId>
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

Previous example will connect to the ```DataSource``` exposed by the [jdbc]({{gh}}/jooby-jdbc) module
and run the ```migrate``` command on startup. This is the recommend way of using {{flyway}}, there
is an alternative approach if you have to migrate two or more databases.

If for any reason you need to maintain two or more databases:

```properties
flyway.db1.url = "..."
flyway.db1.locations = db1/migration

flyway.db2.url = "..."
flyway.db2.locations = db2/migration
```

```java
{
  use(new Flywaydb("flyway.db1"));
  use(new Flywaydb("flyway.db2"));
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
There are some defaults setting that you can see in the appendix.


For more information, please visit the {{flyway}} site.

Happy coding!!!

{{appendix}}
