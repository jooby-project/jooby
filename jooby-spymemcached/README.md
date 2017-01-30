# spymemcached

Provides memcached access via [SpyMemcached](https://github.com/dustin/java-memcached-client)

## exports

* A ```MemcachedClient``` service

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-spymemcached</artifactId>
  <version>1.0.2</version>
</dependency>
```

## usage

```properties
 memcached.server = "localhost:11211"
```

```java
{
  use(new SpyMemcached());

  get("/", req -> {
    MemcachedClient client = require(MemcachedClient.class);
    client.set("foo", 60, "bar");
    return client.get("foo");
  });
}
```

## configuration

It is done via ```.conf``` file:

```properties
memcached.protocol = binary
```

or programmatically:

```java
{
  use(new SpyMemcached()
   .doWith(builder -> {
     builder.setProtocol(Protocol.BINARY);
   })
 );
}
```

# spymemcached session store

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-spymemcached</artifactId>
  <version>1.0.2</version>
</dependency>
```

## usage

```java
{
  use(new SpyMemcached());

  session(SpySessionStore.class);

  get("/", req -> {
   req.session().set("name", "jooby");
  });
}
```

The ```name``` attribute and value will be stored in [Memcached](http://memcached.org).

Session are persisted using the default ```Transcoder```.

## options

### timeout
By default, a [Memcached](http://memcached.org) session will expire after ```30 minutes```. Changing the default timeout is as simple as:

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

### key prefix
Default [Memcached](http://memcached.org) key prefix is ```sessions:```. Sessions in [Memcached](http://memcached.org) will looks like: ```sessions:ID```

It's possible to change the default key setting the ```memcached.sesssion.prefix``` property.

Happy coding!!

## memcached.conf

```properties
memcached {

  authWaitTime = 1s

  daemon = false

  failureMode = redistribute

  locator = ARRAY_MOD

  maxReconnectDelay = 30s

  protocol = text

  shutdownTimeout = -1

}

#default HTTP session settings

memcached.session.prefix = "sessions:"

memcached.session.timeout = ${session.timeout}
```
