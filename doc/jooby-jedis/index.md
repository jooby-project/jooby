---
layout: index
title: jooby-jedis
version: 0.4.2.1
---

# jooby-jedis

[Redis](http://redis.io/) cache and key/value data store for Jooby. Exposes a [Jedis] service.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jedis</artifactId>
  <version>0.4.2.1</version>
</dependency>
```

## usage
It is pretty straightforward:

```properties
# define a database URI
db = "redis://localhost:6379"
```

```java
{
  use(new Redis());

   get("/:key/:value", req -> {
     try (Jedis jedis = req.require(Jedis.class)) {
       jedis.set(req.param("key").value(), req.param("value").value());
       return jedis.get(req.param("key").value());
     }
   });
}
```

## configuration
This module creates a [JedisPool]. A default pool is created with a max of ```128``` instances.

The pool can be customized from your ```application.conf```:

```properties
db = "redis://localhost:6379"

# increase pool size to 200
jedis.pool.maxTotal = 200
```

### two or more redis connections
In case you need two or more Redis connection, just do:

```java
{
  use(new Redis()); // default is "db"
  use(new Redis("db1"));

  get("/:key/:value", req {@literal ->} {
    try (Jedis jedis = req.require("db1", Jedis.class)) {
      jedis.set(req.param("key").value(), req.param("value").value());
      return jedis.get(req.param("key").value());
    }
  });
}
```

application.conf:

```
db = "redis://localhost:6379/0"

db1 = "redis://localhost:6379/1"
```

Pool configuration for ```db1``` is inherited from ```jedis.pool```. If you need
to tweak the pool configuration for ```db1``` just do:

```properties
db1 = "redis://localhost:6379/1"

# ONLY 10 for db1
jedis.db1.maxTotal = 10
```

For more information about [Jedis](https://github.com/xetorthio/jedis) checkout the
[wiki](https://github.com/xetorthio/jedis/wiki)

That's all folks! Enjoy it!

TBD: Object mapping? https://github.com/xetorthio/johm?
