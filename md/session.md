# session
Sessions are created on demand via: [req.ifSession()]({{defdocs}}/Request.html#ifSession--) or [req.session()]({{defdocs}}/Request.html#session--).

Sessions have a lot of uses cases but most commons are: auth, store information about current
user, etc.

A session attribute must be ```String``` or a primitive. Session doesn't allow to store
arbitrary objects. It is a simple mechanism to store basic data.

## no timeout

There is no timeout for sessions from server perspective. By default, a session will expire when
the user close the browser (a.k.a session cookie).

## session store

A [Session.Store]({{defdocs}}/Session.Store.html) is responsible for saving session data. Sessions are kept in memory, by
default using the [Session.Mem]({{defdocs}}/Session.Mem.html) store, which is useful for development, but wont scale well
on production environments. A [redis](/doc/session/#redis-session-store), [memcached](/doc/session/#spymemcached-session-store), [ehcache](/doc/session/#ehcache-session-store) store will be a better option. Checkout the available [session storage](/doc/session).

### store life-cycle

Sessions are persisted every time a request exit, if they are dirty. A session get dirty if an
attribute is added or removed from it.

The <code>session.saveInterval</code> property indicates how frequently a session will be
persisted (in millis).

In short, a session is persisted when:

1) it is dirty; or

2) save interval has expired it.

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

The <code>session.cookie.name</code> indicates the name of the cookie that hold the session ID,
by defaults: <code>jooby.sid</code>. Cookie's name can be explicitly set with
[cookie.name("name")]({{defdocs}}/Cookie.Definition.html#name-java.lang.String-) on
[Session.Definition#cookie()]({{defdocs}}/Session.Definition.html#cookie).
