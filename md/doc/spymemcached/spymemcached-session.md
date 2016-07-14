# spymemcached session store

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

The ```name``` attribute and value will be stored in {{memcached}}.

Session are persisted using the default ```Transcoder```.

## options

### timeout
By default, a {{memcached}} session will expire after ```30 minutes```. Changing the default timeout is as simple as:

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
Default {{memcached}} key prefix is ```sessions:```. Sessions in {{memcached}} will looks like: ```sessions:ID```

It's possible to change the default key setting the ```memcached.sesssion.prefix``` property.
