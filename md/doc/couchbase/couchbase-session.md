# couchbase session store

A [Session.Store]({{defdocs}}/couchbase/CouchbaseSessionStore) powered by <a href="http://www.couchbase.com">Couchbase</a>.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-couchbase</artifactId>
 <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  use(new Couchbase("couchbase://localhost/bucket"));

  session(CouchbaseSessionStore.class);

  get("/", req -> {
    Session session = req.session();
    session.put("foo", "bar");
    ..
  });
}
```

Session data is persisted in Couchbase and document looks like:

```
{
  "session::{SESSION_ID}": {
    "foo": "bar"
  }
}
```

## options

### timeout

By default, a session will expire after ```30 minutes```. Changing the default timeout is as simple as:

```properties
# 8 hours

session.timeout = 8h

# 15 seconds

session.timeout = 15

# 120 minutes

session.timeout = 120m
```

Expiration is done via Couchbase expiry/ttl option.

If no timeout is required, use ```-1```.

### custom bucket

The session document are persisted in the application/default bucket, if you need/want a different bucket then use {@link Couchbase#sessionBucket(String)}, like:

```java
{
  use(
      new Couchbase("couchbase://localhost/myapp")
          .sessionBucket("session")
  );

}
```
