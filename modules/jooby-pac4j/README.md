[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-pac4j/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-pac4j)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-pac4j.svg)](https://javadoc.io/doc/org.jooby/jooby-pac4j/1.3.0)
[![jooby-pac4j website](https://img.shields.io/badge/jooby-pac4j-brightgreen.svg)](http://jooby.org/doc/pac4j)
# pac4j

Authentication module via: [Pac4j 1.x](https://github.com/pac4j/pac4j).


> **DEPRECATED**: This module has been replaced by [Pac4j 2.x](/doc/pac4j2).


## exports

* ```Clients```
* ```WebContext``` as [RequestScoped](/apidocs/org/jooby/RequestedScoped.html)
* [auth filter](/apidocs/org/jooby/Route.Filter.html) per ```Client```
* [auth callback](/apidocs/org/jooby/Route.Filter.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-pac4j</artifactId>
  <version>1.3.0</version>
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
[Pac4j 1.x](https://github.com/pac4j/pac4j) is a powerful library that supports multiple [clients](http://www.pac4j.org/docs/clients.html) and/or authentication protocols. In
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
[Auth](/apidocs/org/jooby/pac4j/Auth.html) module, like:
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

[Jooby](http://jooby.org) relies on [AuthStore](/apidocs/org/jooby/pac4j/AuthStore.html) for saving and retrieving a ```CommonProfile```. By default,
the ```CommonProfile``` is stored in the [Session]({{defcods}}/pac4j/Session.html) via [auth session store]({{defcods}}/pac4j/AuthSessionStore.html)

After a successful authentication the ```CommonProfile``` is accessible as a request scoped attribute:

```java
{
  use(new Auth().form());

  get("/private", req -> require(HttpProfile.class));
}
```

facebook (or any oauth, openid, etc...)

```java
{
  use(new Auth().client(new FacebookClient(key, secret));

  get("/private", req -> require(FacebookProfile.class));
}
```

Custom [AuthStore](/apidocs/org/jooby/pac4j/AuthStore.html) is provided via [Auth#store](/apidocs/org/jooby/pac4j/Auth.html) method:

```java
{
  use(new Auth().store(MyDbStore.class));

  get("/private", req -> require(HttpProfile.class));
}
```

## logout

A default ```/logout``` handler is provided it too. The handler will remove the profile
from [AuthStore](/apidocs/org/jooby/pac4j/AuthStore.html) by calling the [AuthStore#unset](/apidocs/org/jooby/pac4j/AuthStore.html) method. The default login
will redirect to ```/```.

A custom logout and redirect urls can be set via ```.conf``` file or programmatically:

```java
{
  use(new Auth().logout("/mylogout", "/redirectTo"));
}
```

That's all folks! Enjoy it!!!

## auth.conf
These are the default properties for pac4j:

```properties
auth {

  # default callback, like http://localhost:8080/auth

  callback = "http://"${application.host}":"${application.port}${contextPath}"/auth"

  # login options

  login {

    # Where to go after a successful login? Default is: ${application.path}

    redirectTo = ""

  }

  # logout options

  logout {

    url = /logout

    redirectTo = ${application.path}

  }

  # form auth

  form.loginUrl = /login

}
```
