# spymemcached

Provides memcached access via {{spymemcached}}

## exports

* A ```MemcachedClient``` service

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-spymemcached</artifactId>
  <version>{{version}}</version>
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

{{doc/spymemcached/spymemcached-session.md}}

Happy coding!!

{{appendix}}
