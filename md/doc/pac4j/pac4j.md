# pac4j

Authentication module via: {{pac4j}}.

## exports

* ```Clients```
* ```WebContext``` as [RequestScoped]({{defdocs}}/RequestedScoped.html)
* [auth filter]({{defdocs}}/Route.Filter.html) per ```Client```
* [auth callback]({{defdocs}}/Route.Filter.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-pac4j</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
{

  get("/public", () -> ..);

  use(new Auth());

  get("/private", () -> ..);
}
```

Previous example adds a very basic but ready to use form login auth every time you try to
access to ```/private``` or any route defined below the auth module.


## clients
{{pac4j}} is a powerful library that supports multiple clients and/or authentication protocols. In
the next example, we will see how to configure the most basic of them, but also some complex protocols.

### basic auth

If basic auth is all you need, then:


```java
{
  use(new Auth().basic());
}
```

A ```IndirectBasicAuthClient``` depends on ```UsernamePasswordAuthenticator```, default is
```SimpleTestUsernamePasswordAuthenticator``` which is great for development, but nothing good
for other environments. Next example setup a basic auth with a custom:
```UsernamePasswordAuthenticator```:

```java
{
  use(new Auth().basic("*", MyUsernamePasswordAuthenticator.class));
}
```

### form auth
Form authentication will be activated by calling ```form```:

```java
{
  use(new Auth().form());
}
```

Form is the default authentication method so previous example is the same as:

```java
{
  use(new Auth());
}
```

Like basic auth, form auth depends on a ```UsernamePasswordAuthenticator```.

A login form will be ready under the path: ```/login```. Again, it is a very basic login
form useful for development. If you need a custom login page, just add a route before the
[Auth]({{defdocs}}/pack4j/Auth.html) module, like:
</p>

```java
{
  get("/login", () -> Results.html("login"));

  use(new Auth());
}
```

Simply and easy!

### oauth, openid, etc...

Twitter, example:

```java
{
  use(new Auth()
    .client(conf ->
       new TwitterClient(conf.getString("twitter.key"), conf.getString("twitter.secret"))));
}
```

Keep in mind you will have to add the require Maven dependency to your project, beside that it is
pretty straight forward.


## protecting urls

By default a ```Client``` will protect all the urls defined below the module, because routes in
are executed in the order they where defined.

You can customize what urls are protected by specifying a path pattern:

```java
{
  use(new Auth().form("/private/**"));

  get("/hello", () -> "no auth");

  get("/private", () -> "auth");
}
```

Here the ```/hello``` path is un-protected, because the client will intercept everything
under ```/private```.

## user profile

{{jooby}} relies on [AuthStore]({{defdocs}}/pac4j/AuthStore.html) for saving and retrieving an ```UserProfile```. By default,
the ```UserProfile``` is stored in the [Session]({{defcods}}/Session.html) via [auth session store]({{defcods}}/pack4j/AuthSessionStore.html)

After a successful authentication the ```UserProfile``` is accessible as a request scoped attribute:

```java
{
  use(new Auth().form());

  get("/private", req -> req.require(HttpProfile.class));
}
```

facebook (or any oauth, openid, etc...)

```java
{
  use(new Auth().client(new FacebookClient(key, secret));

  get("/private", req -> req.require(FacebookProfile.class));
}
```

Custom [AuthStore]({{defdocs}}/pac4j/AuthStore.html) is provided via [Auth#store]({{defdocs}}/pac4j/Auth.html) method:

```java
{
  use(new Auth().store(MyDbStore.class));

  get("/private", req -> req.require(HttpProfile.class));
}
```

## logout

A default ```/logout``` handler is provided it too. The handler will remove the profile
from [AuthStore]({{defdocs}}/pac4j/AuthStore.html) by calling the [AuthStore#unset]({{defdocs}}/pac4j/AuthStore.html) method. The default login
will redirect to ```/```.

A custom logout and redirect urls can be set via ```.conf``` file or programmatically:

```java
{
  use(new Auth().logout("/mylogout", "/redirectTo"));
}
```

That's all folks! Enjoy it!!!

{{appendix}}
