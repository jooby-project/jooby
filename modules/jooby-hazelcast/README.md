[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-hazelcast/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-hazelcast)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-hazelcast.svg)](https://javadoc.io/doc/org.jooby/jooby-hazelcast/1.4.1)
[![jooby-hazelcast website](https://img.shields.io/badge/jooby-hazelcast-brightgreen.svg)](http://jooby.org/doc/hazelcast)
# hazelcast

Provides cache solution and session storage via [Hazelcast](http://hazelcast.org).

## exports

* ```HazelcastInstance```
* Optionally, a [session store](/apidocs/org/jooby/hazelcast/HcastSessionStore.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hazelcast</artifactId>
  <version>1.4.1</version>
</dependency>
```

## usage

```java
{
  use(new Hcast());

  get("/", req -> {
    HazelcastInstance hcast = require(HazelcastInstance.class);
    ...
  });
}
```

## configuration

Any property under ```hazelcast.*``` will be automatically add it while bootstrapping a ```HazelcastInstance```.

Configuration can be done programmatically via: ```doWith(Consumer)```

```java
{
  use(new Hcast()
   .doWith(config -> {
     config.setXxx
   })
  );
}
```

# hazelcast session store

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hazelcast</artifactId>
  <version>1.4.1</version>
</dependency>
```

## usage

```java
{
  use(new Hcast());

  session(HcastSessionStore.class);

  get("/", req -> {
   req.session().set("name", "jooby");
  });
}
```

## options

### timeout

By default, a [Hazelcast](http://hazelcast.org) session will expire after ```30 minutes```. Changing the default timeout is as simple as:

```properties
# 8 hours

session.timeout = 8h

# 15 seconds

session.timeout = 15

# 120 minutes

session.timeout = 120m

# no timeout

session.timeout = -1
```

### name
Default session's name is ```sessions```. It's possible to change the default name by setting the property: ```hazelcast.sesssion.name```.

Happy coding!!!

## hcast.conf
These are the default properties for hazelcast:

```properties
# logging

hazelcast.logging.type = slf4j

# session store, key prefix and timeout in seconds

hazelcast.session.name = sessions

hazelcast.session.timeout = ${session.timeout}
```
