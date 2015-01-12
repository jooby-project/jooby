# overview

A minimalist web framework for Java 8, inspired by [express.js](http://expressjs.com/) (between others).

Jooby API mimics (as much as possible) the [Express API](http://expressjs.com/4x/api.html).

API is short and easy to learn, around 30 classes for the core project. Java doc is available here: [apidocs](http://jooby.org/apidocs).

## technology stack

* [Undertow](http://undertow.io/): for a high performance and non-blocking IO web server. is the web server behind [WildFly](http://wildfly.org/about/) (a.k.a JBoss App Server).
* [Guice](https://github.com/google/guice): for dependency injection and modularity.
* [Config](https://github.com/typesafehub/config): for powerful config files.
* [logback](http://logback.qos.ch/): for debugging and logging.
* [maven 3.x](http://maven.apache.org/): for building apps.
-----

Jooby believes in [maven 3.x](http://maven.apache.org/) for building/running/packaging and distributing Java project.

modules
-----

Reusable software is provided from a [module](http://jooby.org/apidocs/org/jooby/Jooby.Module.html) and/or
[request module](http://jooby.org/apidocs/org/jooby/Request.Module.html). A module in Jooby plays the same role as in Guice, but API is different.

config files
-----

Supports files in three formats: ```.properties```, ```*.json```, ```*.conf``` (and a human-friendly JSON superset). It merges multiple files across all formats, can load from files, URLs, or classpath.

Users can override the config with Java system properties, java ```-Dmyapp.foo.bar=10```

routes
-----

A [route](http://jooby.org/apidocs/org/jooby/Route.html) looks like:

```java
  {
    get("/", (req, rsp) -> rsp.send("Hello Jooby!"));
  }
```

or

```java
  @Path("/")
  @GET
  public String home () {
    return "Hello Jooby!";
  }
```

web sockets
-----

A web socket looks like:

```java
  {
    ws("/", (ws) -> {

      ws.onMessage(message -> ws.send("Hello " + message.stringValue()));

      ws.send("connected");
    });
  }
```
