# overview

Jooby is an micro-web framework for Java 8 (or higher) inspired by [Express](http://expressjs.com/). 

Jooby API mimics (as much as possible) the [Express API](http://expressjs.com/4x/api.html).

API is short and easy to learn, around 30 classes for the core project. Java doc is available here: [apidocs](http://jooby.org/apidocs).

## technology stack

### http nio server

Jooby believes in [Jetty 9.x](https://www.eclipse.org/jetty) for NIO HTTP. Since version 9.x Jetty use a NIO connector for HTTP.

If you want to learn more about NIO in Jetty 9.x [go here](http://stackoverflow.com/questions/25195128/how-do-jetty-and-other-containers-leverage-nio-while-sticking-to-the-servlet-spe)

### dependency injection

Jooby believes in [Guice 4.x](https://github.com/google/guice) for dependency injection.

### config files

Jooby believes in [Type Safe Config](https://github.com/typesafehub/config) for config files.

### logging

Jooby believes in [logback](http://logback.qos.ch/) for logging.

### maven 3.x

Jooby believes in [maven 3.x](http://maven.apache.org/) for building/running/packaging and distributing Java project.

## modules

Reusable software is provided from a [module](http://jooby.org/apidocs/org/jooby/Jooby.Module.html) and/or
[request module](http://jooby.org/apidocs/org/jooby/Request.Module.html). A module in Jooby play the same role as in Guice, but API is different.

## config files

Supports files in three formats: ```.properties```, ```*.json```, ```*.conf``` (and a human-friendly JSON superset). It merges multiple files across all formats, can load from files, URLs, or classpath.

Users can override the config with Java system properties, java ```-Dmyapp.foo.bar=10```

## routes

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

## web sockets

A web socket looks like:

```java
  {
    ws("/", (ws) -> {

      ws.onMessage(message -> ws.send("Hello " + message.stringValue()));

      ws.send("connected");
    });
  }
```
