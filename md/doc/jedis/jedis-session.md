# redis session store

## usage

```java
{
  use(new Redis());

  session(RedisSessionStore.class);

  get("/", req -> {
   req.session().set("name", "jooby");
  });
}
```

The ```name``` attribute and value will be stored in a {{redis}}. Sessions are persisted as [hashes](http://redis.io/topics/data-types#hashes).

## options

### timeout

By default, a {{redis}} session will expire after ```30 minutes```. Changing the default timeout is as simple as:

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

Default redis key prefix is ```sessions```. Sessions in [redis] will look like: ```sessions:ID```

It's possible to change the default key setting the ```jedis.sesssion.prefix``` properties
