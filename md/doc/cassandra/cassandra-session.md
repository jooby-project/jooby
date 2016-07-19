# cassandra session store

A [Session.Store]({{defdocs}}/cassandra/CassandraSessionStore.html) powered by <a href="http://cassandra.apache.org">Cassandra</a>.

## usage

```java
{
  use(new Cassandra("cassandra://localhost/db"));

  session(CassandraSessionStore.class);

  get("/", req -> {
    Session session = req.session();
    session.put("foo", "bar");
    ..
  });

}
```

Session data is persisted in Cassandra using a ```session``` table.

## options

### timeout

By default, a session will expire after ```30 minutes```. Changing the default timeout is as simple as:

```
# 8 hours
session.timeout = 8h
# 15 seconds
session.timeout = 15
# 120 minutes
session.timeout = 120m
```

Expiration is done via Cassandra ttl option.

If no timeout is required, use ```-1```.
