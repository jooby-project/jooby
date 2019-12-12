[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-pac4j2/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-pac4j2/1.6.6)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-pac4j2.svg)](https://javadoc.io/doc/org.jooby/jooby-pac4j2/1.6.6)
[![jooby-pac4j2 website](https://img.shields.io/badge/jooby-pac4j2-brightgreen.svg)](http://jooby.org/doc/pac4j2)
# pac4j module

Authentication module via: <a href="https://github.com/pac4j/pac4j">Pac4j 2.x</a>.

## exports

* Pac4j `UserProfile` object
* Pac4j `Config` object
* Pac4j `WebContext` object
* Pac4j `ProfileManager` object
* Routes for Pac4j callback, security filter and logout

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-pac4j2</artifactId>
 <version>1.6.6</version>
</dependency>
```

## usage

Display a basic login-form and restrict access to all the routes defined after the [Pac4j](/apidocs/org/jooby/pac4j/Pac4j.html) module:

```java
{
  get("/public", () -> {
    ...
  });

  use(new Pac4j());

  get("/private", () -> {
    ...
  });
}
```

## clients

A <a href="http://www.pac4j.org/docs/clients.html">Client</a> represents an authentication mechanism. It performs the login process and returns (if successful) a user profile

Clients are configured at bootstrap time using the [Pac4j](/apidocs/org/jooby/pac4j/Pac4j.html) DSL:

```java
{
  use(new Pac4j()
    .client(conf -> {
      return new FacebookClient(conf.getString("fb.key"), conf.getString("fb.secret"));
    })
  );
}
```

You can chain calls to add multiple clients:

```java
{
  use(new Pac4j()
    .client(conf -> {
      return new FormClient("/login", new SimpleTestSimpleTestUsernamePasswordAuthenticator());
    })
    .client(conf -> {
      return new FacebookClient(conf.getString("fb.key"), conf.getString("fb.secret"));
    })
    .client(conf -> {
      return new TwitterClient(conf.getString("twitter.key"), conf.getString("twitter.secret"));
    })
  );
}
```

## protecting urls

By default [Pac4j](/apidocs/org/jooby/pac4j/Pac4j.html) restrict access to all the routes defined after the [Pac4j](/apidocs/org/jooby/pac4j/Pac4j.html) module. You can specify what url must be protected using a path pattern:

```java
{
  use(new Pac4j()
    .client("/admin/**", conf -> {
      return new FormClient("/login", new SimpleTestSimpleTestUsernamePasswordAuthenticator());
   }));
}
```

Now all the routes under ```/admin``` are protected by [Pac4j](/apidocs/org/jooby/pac4j/Pac4j.html).

## user profile

After login the user profile (current logged user) is accessible via ```require``` calls:

```java
{
  use(new Pac4j().form());

  get("/profile", () -> {
    CommonProfile profile = require(CommonProfile.class);
    ...
  });

}
```

Access to specific profile type depends on the authentication client:

```java
{
  use(new Pac4j()
    .client(conf -> {
      return new FacebookClient(conf.getString("fb.key"), conf.getString("fb.secret"));
    })
  );

  get("/profile", () -> {
    FacebookProfile profile = require(FacebookProfile.class);
    ...
  });

}
```

Pac4j API is also available:

```java
{
  use(new Pac4j()
    .client(conf -> {
      return new FacebookClient(conf.getString("fb.key"), conf.getString("fb.secret"));
    })
  );

  get("/profile", req -> {

    ProfileManager pm = require(ProfileManager.class);
    List<Commonprofile> profiles = pm.getAll(req.ifSession().isPresent());
    ...
  });
}
```

## authorizer

Authorizers are provided via ```client``` DSL. You can provider an instance of an auhtorizer or class reference to an authorizer.

```java
{
  use(new Pac4j()
    .client("*", MyAuthorizer.class, conf -> {
      return new FacebookClient(conf.getString("fb.key"), conf.getString("fb.secret"));
    })
  );

}
```

Here ```MyAuthorizer``` will be provisioned by Guice.

## advanced usage

For advanced usage is available via [doWith](/apidocs/org/jooby/pac4j/Pac4j.html#doWith(--java.util.function.Consumer--)) method:

```java
{
  use(new Pac4j()
    .doWith(pac4j -> {
      pac4j.setSecurityLogic(...);
      pac4j.setHttpActionAdapter(...);
    })
  );

}
```

## starter project

We do provide a [pac4j-starter](https://github.com/jooby-project/pac4j-starter) project. Go and [fork it](https://github.com/jooby-project/pac4j-starter).

That's all folks!!


## pac4j.conf
These are the default properties for pac4j2:

```properties
pac4j {

  # default callback, like http://localhost:8080/auth

  callback {

    method = "*"

    path = /callback

    url = "http://"${application.host}":"${application.port}${application.path}${pac4j.callback.path}

    renewSession = false

    redirectTo = ""

  }

  # security filters

  securityFilter {

    method = "*"

  }

  # logout options

  logout {

    path = /logout

    redirectTo = ${application.path}

    method = "*"

    pattern = "/.*"

    destroySession = true

    local = true

    central = false

  }

  excludePaths = ["/favicon.ico"]

}
```
