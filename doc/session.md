# session
Sessions are created on demand via: [req.ifSession()]({{defdocs}}/Request.html#ifSession--) or [req.session()]({{defdocs}}/Request.html#session--).

Sessions have a lot of uses cases but the most commons are: authentication, storing information about current
user, etc.

A session attribute must be a ```String``` or a primitive. The session doesn't allow storing of
arbitrary objects. It's intended as a simple mechanism to store basic data.

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

The previous example will use an `in-memory` session. The next example uses a cookie:

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

The cookie session store depends on the `application.secret` property. The cookie will be signed with the value of this property.

As an alternative to the `memory` or the `cookie` stores, you can choose any one of the [high performance session stores](/doc/session) provided by {{jooby}}. There are provided session stores for `redis`, `memcached`, `mongodb`, `cassandra`, `couchbase`, `hazelcast` and [a lot more](/doc/session).

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

## no timeout

There is no timeout for sessions from the perspective of the server. By default, a session will expire when
the user close the browser (a.k.a session cookie) or the cookie session has expired via the `maxAge` attribute.

Session store implementations might or might not implement a server `timeout`.

## cookie

### max-age
The <code>session.cookie.maxAge</code> sets the maximum age in seconds. A positive value
indicates that the cookie will expire after that many seconds have passed. Note that the value is
the <i>maximum</i> age when the cookie will expire, not the cookie's current age.

A negative value means that the cookie is not stored persistently and will be deleted when the
browser exits.

Default maxAge is: <code>-1</code>.

### signed cookie
If the <code>application.secret</code> property has been set, the session cookie will be
signed with it.

### cookie name
The <code>session.cookie.name</code> indicates the name of the cookie that hold the session ID, by
default: <code>jooby.sid</code>. The cookie's name can be explicitly set with
[cookie.name("name")]({{defdocs}}/Cookie.Definition.html#name-java.lang.String-) on [Session.Definition#cookie()]({{defdocs}}/Session.Definition.html#cookie).
