# overview

A minimalist web framework for Java 8, inspired by [express.js](http://expressjs.com/) (between others).

API is short and easy to learn, around 30 classes for the core project. Java doc is available here: [apidocs](http://jooby.org/apidocs).

## technology stack

* [Multi Server]:
  * [Netty](http://netty.io/)
  * [Jetty](www.eclipse.org/jetty)
  * [Undertow](http://undertow.io/)
  * [Servlet Container](http://jooby.org/modules/servlet-container)
* [Guice](https://github.com/google/guice): for dependency injection and modularity.
* [Config](https://github.com/typesafehub/config): for powerful config files.
* [logback](http://logback.qos.ch/): for debugging and logging.
* [maven 3.x](http://maven.apache.org/): for building apps.

modules
-----

Reusable software is provided from a [module](http://jooby.org/apidocs/org/jooby/Jooby.Module.html). A module in Jooby plays the same role as in Guice, but API is a bit different.

config files
-----

Supports files in three formats: ```.properties```, ```*.json```, ```*.conf``` (and a human-friendly JSON superset). Merge multiple files across all formats, can load from files, URLs, or classpath.

Users can override config properties with Java system properties, java ```-Dmyapp.foo.bar=10```

routes
-----

A [route](http://jooby.org/apidocs/org/jooby/Route.html) looks like:

```java
  {
    get("/", () -> "Hello Jooby!");
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

      ws.onMessage(message -> ws.send("Hello " + message.value()));

      ws.send("connected");
    });
  }
```
