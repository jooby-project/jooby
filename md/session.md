# session
Sessions are created on demand via: [req.ifSession()]({{defdocs}}/Request.html#ifSession--) or [req.session()]({{defdocs}}/Request.html#session--).

Sessions have a lot of uses cases but most commons are: auth, store information about current
user, etc.

A session attribute must be ```String``` or a primitive. Session doesn't allow to store
arbitrary objects. It is a simple mechanism to store basic data.

## usage

```java
{
  get("/", req -> {
    Session session = req.session();

    // set attribute
    session.set("foo", "bar");

    // get attribute
    return session.get("foo").value();
  });
}
```

Previous example will use an `in-memory` session. The next example uses a cookie:

```java
{
  cookieSession();

  get("/", req -> {
    Session session = req.session();

    // set attribute
    session.set("foo", "bar");

    // get attribute
    return session.get("foo").value();
  });
}
```

The cookie session store depends on the `application.secret` property. We sign the cookie with the `application.secret` property.

If `memory` or `cookie` store are not an option, then you can choose one of the [high performance session store](/doc/session) provided by {{jooby}}. We provide session stores for `redis`, `memcached`, `mongodb`, `cassandra`, `couchbase`, `hazelcast` and [lot more](/doc/session).

## no timeout

There is no timeout for sessions from server perspective. By default, a session will expire when
the user close the browser (a.k.a session cookie) or the cookie session has expired via `maxAge` attribute.

Session store implementation might or might not implemented a server `timeout`.

## cookie

### max-age
The <code>session.cookie.maxAge</code> sets the maximum age in seconds. A positive value
indicates that the cookie will expire after that many seconds have passed. Note that the value is
the <i>maximum</i> age when the cookie will expire, not the cookie's current age.

A negative value means that the cookie is not stored persistently and will be deleted when the
Web browser exits.

Default maxAge is: <code>-1</code>.

### signed cookie
If the <code>application.secret</code> property has been set, then the session cookie will be
signed it with it.

### cookie's name
The <code>session.cookie.name</code> indicates the name of the cookie that hold the session ID, by
defaults: <code>jooby.sid</code>. Cookie's name can be explicitly set with
[cookie.name("name")]({{defdocs}}/Cookie.Definition.html#name-java.lang.String-) on [Session.Definition#cookie()]({{defdocs}}/Session.Definition.html#cookie).
