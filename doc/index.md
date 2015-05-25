---
layout: index
title: doc
version: 0.5.3
---

documentation
=====
- [philosophy: do more, more easily](#philosophy:-do-more,-more-easily)
- [config, environment and logging](#config,-environment-and-logging)
  - [application.conf](#application.conf)
  - [injecting properties](#injecting-properties)
  - [special properties](#special-properties)
  - [config precedence](#config-precedence)
  - [logging](#logging)
- [modules](#modules)
- [routes](#routes)
  - [creating routes](#creating-routes)
  - [route handler](#route-handler)
  - [path patterns](#path-patterns)
  - [static files](#static-files)
  - [precedence and order](#precedence-and-order)
  - [request handling](#request-handling)
  - [mvc routes](#mvc-routes)
- [web sockets](#web-sockets)
  - [guice access](#guice-access)
  - [consumes](#consumes)
  - [produces](#produces)
- [request](#request)
  - [request params](#request-params)
  - [request headers](#request-headers)
  - [request body](#request-body)
  - [local variables](#local-variables)
  - [guice access](#guice-access)
- [response](#response)
  - [sending data](#sending-data)
  - [content negotiation](#content-negotiation)
  - [response headers](#response-headers)
- [session](#session)
  - [options](#options)
  - [session store](#session-store)
  - [cookie](#cookie)
- [err](#err)
  - [default err handler](#default-err-handler)
  - [custom err handler](#custom-err-handler)
  - [status code](#status-code)
- [parser, renderer and view engine](#parser,-renderer-and-view-engine)
  - [parser](#parser)
  - [renderer](#renderer)
  - [view engine](#view-engine)
- [appendix: jooby.conf](#appendix:-jooby.conf)
- [appendix: mime.properties](#appendix:-mime.properties)


# philosophy: do more, more easily

- Simple and effective programming model for building small and large scale web applications
- Build with developer productive in mind
- Plain Java DSL for routes (no xml, or similar)
- Reflection, annotations and dependency injection are keep to minimum and in some cases it is completely optional
- No classpath hell, default deployment model uses a normal JVM bootstrap
- Modules are easy to use and they do as less as possible


In one word? Jooby keeps it simple but yet powerful.


# config, environment and logging

Jooby delegates configuration management to [TypeSafe Config](https://github.com/typesafehub/config). If you aren't familiar with [TypeSafe Config](https://github.com/typesafehub/config) please take a few minutes to discover what [TypeSafe Config](https://github.com/typesafehub/config) can do for you.

## application.conf

By defaults Jooby will attempt to load an ```application.conf``` file from root of classpath. Inside the file you can add/override any property you want.

## injecting properties

Any property can be injected using the ```javax.inject.Named``` annotation and automatic type conversion is provided when a type:

1) Is a primitive, primitive wrapper or String

2) Is an enum

3) Has a public **constructor** that accepts a single **String** argument

4) Has a static method **valueOf** that accepts a single **String** argument

5) Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```

6) Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```

7) There is custom [Guice](https://github.com/google/guice) type converter for the type

It is also possible to inject the root ```com.typesafe.config.Config``` object or a child of it.

## special properties

### application.env

Jooby internals and the module system rely on the ```application.env``` property. By defaults, this property is set to: ```dev```.

This special property is represented at runtime with the [Env](/apidocs/org/jooby/Env.html) class.

For example, the [development stage](https://github.com/google/guice/wiki/Bootstrap) is set in [Guice](https://github.com/google/guice) when ```application.env == dev```.
A module provider, might decided to create a connection pool, cache, etc when ```application.env != dev ```.

### application.secret

If present, the session cookie will be signed with the ```application.secret```.

### default properties

Here is the list of default properties provided by  Jooby:

* **application.name**: describes the name of your application. Default is: *app.getClass().getSimpleName()*
* **application.tmpdir**: location of the temporary directory. Default is: *${java.io.tmpdir}/${application.name}*
* **application.charset**: charset to use. Default is: *UTF-8*
* **application.lang**: locale to use. Default is: *Locale.getDefault()*. A ```java.util.Locale``` can be injected.
* **application.dateFormat**: date format to use. Default is: *dd-MM-yyyy*. A ```java.time.format.DateTimeFormatter``` can be injected.
* **application.numberFormat**: number format to use. Default is: *DecimalFormat.getInstance("application.lang")*
* **application.tz**: time zone to use. Default is: *ZoneId.systemDefault().getId()*. A ```java.time.ZoneId``` can be injected.

## config precedence

Config files are loaded in the following order (first-listed are higher priority)

* system properties
* (file://[application].[mode].[conf])?
* (cp://[application].[mode].[conf])?
* ([application].[conf])?
* [modules in reverse].[conf]*


It looks kind of complex, right?
It does, but at the same time it is very intuitive and makes a lot of sense. Let's review why.

### system properties

System properties can override any other property. A sys property is set at startup time, like: 

    java -jar myapp.jar -Dapplication.secret=xyz

### file://[application].[mode].[conf] 

The use of this conf file is optional, because Jooby recommend to deploy your application as a **fat jar** and all the properties files should be bundled inside the jar.

If you find this impractical, then this option is for you.

Let's say your app includes a default property file: ```application.conf``` bundled with your **fat jar**. Now if you want/need to override two or more properties, just do this:

* find a directory to deploy your app
* inside that directory create a file: ```application.conf```
* start the app from same directory

That's all. The file system conf file will take precedence over the classpath config file, overriding any property.

A good practice is to start up your app with a **env**, like:

    java -jar myapp.jar -Dapplication.env=prod

The process is the same, except this time you can name your file as:

    application.prod.conf

### cp://[application].[mode].[conf]

Again, the use of this conf file is optional and works like previous config option, except here the **fat jar** was bundled with all your config files (dev, stage, prod, etc.)

Example: you have two config files: ```application.conf``` and ```application.prod.conf````. Both files were bundled inside the **fat jar**, starting the app in **prod** env:

    java -jar myapp.jar -Dapplication.env=prod

So here the ```application.prod.conf``` will takes precedence over the ```application.conf``` conf file.

This is the recommended option from Jooby, because your app doesn't have an external dependency. If you need to deploy the app in a new server all you need is your **fat jar**

### [application].[conf]

This is the default config files and it should be bundle inside the **fat jar**. As mentioned early, the default name is: **application.conf**, but if you don't like it or need to change it:

```java
{
   use(ConfigFactory.parseResources("myconfig.conf"));
}
```


### [modules in reverse].[conf]

As mentioned in the [modules](#modules) section a module might define his own set of properties.

```
{
   use(new M1());
   use(new M2());
}
```

In the previous example the M2 modules properties will take precedence over M1 properties.

As you can see the config system is very powerful and can do a lot for you.


## logging

Logging is done via [logback](http://logback.qos.ch). Logback bootstrap and configuration is described here [logback configuration](http://logback.qos.ch/manual/configuration.html)

It is useful that we can bundle logging  configuration files inside our *fat* jar, it works very well for small/simple apps.

For medium/complex apps and/or if you need/want to debug errors the configuration files should/must be outside the jar, so you can turn on/off loggers, change log level etc..

On such cases all you have to do is start the application with the location of the logback configuration file:

    java -Dlogback.configurationFile=logback.xml -jar myapp.jar

The ```-Dlogback.configurationFile``` property controls the configuration file to load. More information can be found [here](http://logback.qos.ch/manual/configuration.html)


# modules

Modules are a key concept for building reusable and configurable piece of software. Modules like in [Guice](https://github.com/google/guice) are used to wire services, connect data, etc...

A module is usually a small piece of software that bootstrap and configure common code and/or an external library.

### do less and be flexible

A module should do as less as possible (key difference with other frameworks). A module for a library *X* should:

* Bootstrap X
* Configure X
* Exposes raw API of X

This means a module should NOT create wrapper for a library. Instead, it should provide a way to extend, configure and use the raw library.

This principle, keep module usually small, maintainable and flexible.

A module is represented by the [Jooby.Module](/apidocs/org/jooby/Jooby.Module.html) class. The configure callback looks like:

```java
public class M1 implements Jooby.Module {
    public void configure(Env env, Config config, Binder binder) {
      binder.bind(...).to(...);
    }
}
```

The configure callback is similar to a [Guice module](https://github.com/google/guice), except you can access to the [Env](/apidocs/org/jooby/Env.html) and [Type Safe Config](https://github.com/typesafehub/config) objects.

In addition to the **configure** callback, a module in Jooby has one additional method:  **config**. The ```config``` method allow a module to specify default properties.

```java
public class M1 implements Jooby.Module {
    public void configure(Env env, Config config, Binder binder) {
      binder.bind(...).to(...);
    }

   public Config config() {
     return Config.parseResources(getClass(), "m1.properties");
   }
}
```

This is useful for setting defaults values or similar.

A module is registered at startup time:

```java
import org.jooby.Jooby;

public class MyApp extends Jooby {

  {
     // as lambda
     use((mode, config, binder) -> {
        binder.bind(...).to(...);
     });
     // as instance
     use(new M1());
     use(new M2());
  }

}
```

Cool, isn't?


# routes

A route describes the interface for making requests to your web app. It combines a HTTP method (a.k.a. HTTP verb) and a path pattern.

A route has an associated handler, which does some job and produces some kind of output (HTTP response).

## creating routes
A route definition looks like:

```java
get("/", () -> "hey jooby");
```

We created a route to handle GET request at the root of our app. Any other HTTP method can be created in the same way.

If you need a POST all you have to do is:

```java
post("/", () -> "hey jooby");
```

or need to listen to any HTTP method:

```java
use("*", "/", () -> "hey jooby");
```

It is possible to name a route explicitly:

```java
get("/", () -> "hey jooby")
   .name("salute");
```

Default route name is **anonymous**. Naming a route is useful for debugging purpose, specially if you have two or more routes mounted on the same path.

## route handler

Jooby offers serveral flavors for creating routes:

### no arg handler: ()

```java
get("/", () -> "hey jooby");
```

### one arg handler: req

```java
get("/", req -> "hey jooby");
```

### two args handler: (req, rsp)

```java
get("/", (req, rsp) -> rsp.send("hey jooby"));
```

### filter: (req, rsp, chain)


```java
get("/", (req, rsp, chain) -> {
  // do something
  chain.next(req, rsp);
});
```

## path patterns

### static patterns

```java
get("/", () -> "hey jooby");

get("/help", () -> "hey jooby");

get("/mail/inbox", () -> "hey jooby");
```

### var/regex patterns

```java
get("/user/:id", req -> "hey " + req.param("id").value());

// alternative syntax
get("/user/{id}", req -> "hey " + req.param("id").value());

// regex
get("/user/{id:\\d+}", req -> "hey " + req.param("id").intValue());
```

Reques params are covered later, for now all you need to know is that you can access to a path parameter using the [Request.param(String)](/apidocs/org/jooby/Request.param(java.lang.String)).

### ant style patterns

  ```com/t?st.html``` - matches ```com/test.html``` but also ```com/tast.html``` and ```com/txst.html```

  ```com/*.html``` - matches all ```.html``` files in the ```com``` directory

  ```com/**/test.html``` - matches all ```test.html``` files underneath the ```com``` path

  ```**``` - matches any path at any level

  ```*``` - matches any path at any level, shortcut for ```**```

## static files

Static files are located under the ```public``` directory.

```bash
├── public
    ├── assets
    |   ├── js
    |   |   └── index.js
    |   ├── css
    |   |   └── style.css
    |   └── images
    |       └── logo.png
    └── welcome.html
```

Now, let's add an asset handler:

```java
{
  assets("/assets/**");
}
```

The asset route handler resolves requests like:

```bash
GET /assets/js/index.js
GET /assets/css/style.css
```

It is possible to map a single static file to a path:

```java
{
  assets("/", "welcome.html");
}
```

A ```GET /``` will display the static file ```welcome.html```.

Here is another example that uses [Webjars](http://www.webjars.org/):

```java
{
  assets("/assets/**", "/META-INF/resources/webjars/{0}");

  assets("/assets/**");
}
```

and responds to the following requests:

```bash
GET /assets/jquery/2.1.3/jquery.js
GET /assets/bootstrap/3.3.4/css/bootstrap.css
```

but also to any other application specific resource:

```bash
GET /assets/js/index.js
GET /assets/css/style.css
```

## precedence and order

Routes are executed in the order they are defined. So the ordering of routes is crucial to the behavior of an application. Let's review this fact via some examples.

```java
get("/abc", req -> "first");

get("/abc", req -> "second");
```

A call to ```/abc``` produces a response of ```first```. If we revert the order:

```java
get("/abc", req -> "second");

get("/abc", req -> "first");
```

It produces a response of ```second```. As you can see **order is very important**.

Now, why is it allowed to have two routes for the same exactly path?

Because we want **filters** for routes.

A route handler accept a third parameter, commonly named chain, which refers to the next route handler in line:

```java
get("/abc", (req, rsp, chain) -> {
  System.out.println("first");
  chain.next(req, rsp);
});

get("/abc", (req, rsp) -> {
  rsp.send("second");
});
```

Again the order of route definition is very important. Forgetting this will cause your app behave unpredictably. We will learn more about this behavior in the examples in the next section.

## request handling

When a request is made to the server, which matches a route definition, the associated callback functions kick in to process the request and send back a response. We call this route pipe or stack.

Routes are like a plumbing pipe, requests start at the first route you define and work their way "down" the route stack processing for each path they match.

Each route handler has the capability to send a response or pass on the request to the next route handler in the current stack.

Route handlers, also have access to the chain object, which happens to be the next callback function in the pipe. To make the chain object available to the callback function, pass it along with the req and the rsp objects to it:

```java
get("/", function(req, rsp, chain) {
  chain.next(req, rsp);
});
```

If there is no matching callback function after the current callback function, next refers to the built-in 404 error handler, and it will be triggered when you call it.

Try to guess the output of:

```java
get("/", (req, rsp, chain) -> rsp.send("first"));

get("/", (req, rsp, chain) -> rsp.send("second"));

get("/", (req, rsp) -> rsp.send("third"));
```

Will the server print all of them, or "first" or, "third"?

The server will print just "first". The act of doing a ```rsp.send()``` terminates the flow of the request then and there; the request is not passed on to any other route handler.

So, how do we specify multiple handlers for a route, and use them all at the same time? Call the **chain.next()** function from the callback, without calling **send** because it terminates the request flow. Here is an example:

```java
get("/", (req, rsp, chain) -> {
  System.out.println("first");
  chain.next(req, rsp);
});

get("/", (req, rsp, chain) -> {
  System.out.println("second");
  chain.next(req, rsp);
});


get("/", (req, rsp) -> {
  rsp.send("third");
});

```

Alternative, if you *always* call **chain.next** you can just do:

```java
get("/", (req, rsp) -> {
  System.out.println("first");
});

get("/", (req, rsp) -> {
  System.out.println("second");
});


get("/", (req, rsp) -> {
  rsp.send("third");
});

```

The 3rd arg is required if you need to decide if the next route need to be executed or not. If you always call **chain.next** the 3rd arg isn't required and does exactly what the 2arg handler does: **always call chain.next**

A good example for a filter is to handle for example authentication:

```java
get("/", (req, rsp, chain) -> {
  if (condition) {
    // It is OK
    chain.next(req, rsp);
  } else {
    throw new Route.Err(403);
  }
});
```


## mvc routes

Mvc routes are like **controllers** in [Spring](http://spring.io) and/or **resources** in [Jersey](https://jersey.java.net/) with some minor enhancements and/or simplifications.

```java
@Path("/routes")
public class MyRoutes {

  @GET
  public Result home() {
    return Results.html("home").put("model", model);
  }
}
```

Annotations are identical to [Jersey/JAX-RS](https://jersey.java.net/) and they can be found under the package **org.jooby.mvc**.

Keep in mind, Jooby doesn't implement the **JAX-RS** spec that is why it has his own version of the annotations.

A mvc route can be injected by Guice:

```java
@Path("/routes")
public class MyRoutes {

  @Inject
  public MyRoutes(DepA a, DepB) {
   ...
  }

  @GET
  public Result home() {
    return Results.html("home").put("model", model);
  }
}
```

A method annotated with [GET](/apidocs/org/jooby/mvc/GET.html), [POST](/apidocs/org/jooby/mvc/POST.html),... (or any of the rest of the verbs) is considered a route handler (web method).

### registering a mvc route

Mvc routes must be registered, there is no auto-discover feature (and it won't be), classpath scanning, ..., etc.

We learnt that the order in which you define your routes has a huge importance and it defines how your app will work. This is one of the reason why mvc routes need to be explicitly registered. The other reason is bootstrap time, declaring the route explicitly helps to reduce bootstrap time.

So, how do I register a mvc route?

In the same way everything else is registered in Jooby!! from your app class:

```java
public class App extends Jooby {
  {
     use(MyRoutes.class);
  }
}
```

Again, handlers are registered in the order they are declared, so:

```java
@Path("/routes")
public class MyRoutes {
  
  @GET
  public void first() {
     log.info("first");
  }

  @GET
  public void second() {
     log.info("second");
  }

  @GET
  public String third() {
     return "third";
  }
}
```

A call to ```/routes``` will print: **first**, **second** and produces a response of **third**.


### binding req params

A mvc handler can be bound to current request parameters:

```java
   @GET
   public List<Object> search(String q) {
    return searcher.doSearch(q);
   }
```

Here **q** can be any of the available param types and it will resolved as described in the [param types and precedence](#param-types-and-precedence) section.

Optional params work in the same way, all you have to do is to declare the param as *java.util.Optional*:

```java
   @GET
   public List<Object> search(Optional<String> q) {
    return searcher.doSearch(q.orElse("*:*"));
   }
```

Multi-value params work in the same way, all you have to do is to declare the param as *java.util.List*, *java.util.Set* or *java.util.SortedSet*:

```java
   @GET
   public List<Object> search(List<String> q) {
    return searcher.doSearch(q);
   }
```

Just remember the injected collection is immutable.

File uploads (again) work in the same way, just use *org.jooby.Upload*

```java
   @POST
   public View search(Upload file) {
    ... do something with the uploaded file
   }
```

As you might already noticed, Jooby uses the method param name and bind it to the request param. If you want explicit mapping and/or the req param isn't a valid Java identifier:

```java
   @GET
   public List<Object> search(@Named("req-param") String reqParam) {
    ...
   }
```

### binding req body

Injecting a req body work in the same way:

```java
  @POST
  public View search(@Body MyObject object) {
  ... do something with my object
  }
```

All you have to do is add the ```@Body``` annotation.

### binding req headers

Works just like [req params](#binding-req-params) but you must annotated the param with *org.jooby.mvc.Header*:

```java
   @GET
   public List<Object> search(@Header String myHeader) {
    ...
   }
```

Or, if the header name isn't a valid Java identifier

```java
   @GET
   public List<Object> search(@Header("Last-Modified-Since") long lastModifedSince) {
    ...
   }
```

### mvc response

A web method might or might not send a response to the client. Some examples:

```java

@GET
public String sayHi(String name) {
  // OK(200)
  return "Hi " + name;
}

@GET
public Result dontSayGoodbye(String name) {
  // NO_CONTENT(204)
  return Results.noContent();
}

```

If you need/want to render a view, just return a *org.jooby.View* instance:

```java
@GET
public Result home() {
  return Results.html("home").put("model", model);
}
```

#### customizing the response

If you need to deal with HTTP metadata like: status code, headers, etc... use a [org.jooby.Result](/apidocs/org/jooby/Result.html)

```java
@GET
public Result handler() {
  // 201 = created
  return Results.with(model, 201);
}
```


# web sockets

The use of web sockets is pretty easy too:

```java
{
  ws("/", ws -> {
    ws.onMessage(message -> System.out.println(message.value()));

    ws.send("connected");
  });
}
```

A [web socket](/apidocs/org/jooby/WebSocket.html) consist of a **path pattern** and a [handler](/apidocs/org/jooby/WebSocket.Handler.html).

A **path pattern** can be as simple or complex as you need. All the path patterns supported by routes are supported here.

A [handler](/apidocs/org/jooby/WebSocket.Handler.html) is executed on new connections, from there we can listen for message, errors and/or send data to the client.

Keep in mind that **web socket** are not like routes. There is no stack/pipe or chain.

You can mount a socket to a path used by a route, but you can't have two or more web sockets under the same path.

## guice access

You can ask [Guice](https://github.com/google/guice) to wired an object from the [ws.require(type)](/apidocs/org/jooby/WebSocket.html#require-com.google.inject.Key-)

```java
ws("/", (ws) -> {
  A a = ws.require(A.class);
});
```

## consumes

Web socket can define a type to consume:

```java
{
  ws("/", ws -> {
    ws.onMessage(message -> {
      MyObject object = message.to(MyObject.class);
    });

    ws.send("connected");
  })
  .consumes("json");
}
```

This is just an utility method for parsing socket message to Java Object. Consumes in web sockets has nothing to do with content negotiation. Content negotiation is route concept, it doesn't apply for web sockets.

## produces

Web socket can define a type to produce: 

```java
{
  ws("/", ws -> {
   MyObject object = ..;
     ws.send(object);
  })
  .produces("json");
}
```

This is just an utility method for formatting Java Objects as text message. Produces in web sockets has nothing to do with content negotiation. Content negotiation is route concept, it doesn't apply for web sockets.


# request

The request object contains methods for reading params, headers and body (between others). In the next section we will mention the most important method of a request object, if you need more information please refer to the [javadoc](/apidocs/org/jooby/Request.html).

## request params

Retrieval of param is done via: [req.param("name")](/apidocs/org/jooby/Request.html#param-java.lang.String-) method.

The [req.param("name")](/apidocs/org/jooby/Request.html#param-java.lang.String-) **always** returns a [Mutant](/apidocs/org/jooby/Mutant.html) instance. A mutant had several utility method for doing type conversion.

Some examples:

```java
get("/", req -> {
  int iparam = req.param("intparam").intValue();

  String str = req.param("str").value();

  // custom object type using type conversion
  MyObject object = req.param("object").to(MyObject.class);

  // file upload
  Upload upload = req.param("file").to(Upload.class);

  // multi value params
  List<String> strList = req.param("strList").toList(String.class);

  // custom object type using type conversion
  List<MyObject> listObj = req.param("objList").toList(MyObject.class);

  // custom object type using type conversion
  Set<MyObject> setObj = req.param("objList").toSet(MyObject.class);

  // optional params
  Optional<String> optStr = req.param("optional").toOptional(String.class);
});
```

### param types and precedence

A request param can be present at:

1) path: */user:id*

2) query: */user?id=...* 

3) body: */user* and params are *formurlenconded* or *multipart*

(first listed are higher precedence)

Now, let's suppose a very poor API where we have a route handler that accept an **id** param in the 3 forms:

A call like:

```bash
curl -X POST -d "id=third" http://localhost:8080/user/first?id=second
```

Produces:

```java
get("/user/:id", req -> {
  // path param at idx = 0
  assertEquals("first", req.param("id").value());
  assertEquals("first", req.param("id").toList(String.class).get(0));

  // query param at idx = 1
  assertEquals("second", req.param("id").toList(String.class).get(1));

  // body param at idx = 2
  assertEquals("third", req.param("id").toList(String.class).get(2));
});
```

It is clear that an API like this should be avoided.

### param type conversion

Automatic type conversion is provided when a type:

* Is a primitive, primitive wrapper or String
* Is an enum
* Is an [Upload](/apidocs/org/jooby/Upload.html)
* Has a public **constructor** that accepts a single **String** argument
* Has a static method **valueOf** that accepts a single **String** argument
* Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```
* Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```
* It is an Optional<T>, List<T>, Set<T> or SortedSet<T> where T satisfies one of previous rules

Custom type conversion is also possible:

```java

parser((type, ctx) -> {
  if (type.getRawType() == MyType.class) {
    // convert the type here
    return ctx.param(values -> new MyType(values.get(0)));
  }
  // no luck! move to next converter
  return next.next();
});

get("/", req -> {
  MyType myType = req.param("value").to(MyType.class);
});
```

## request headers

Retrieval of request headers is done via: [request.header("name")]({{}}Request.html#header-java.lang.String-). All the explained before for [request params](#request params) apply for headers too.

```java
get("/", req -> {
  int iparam = req.header("intparam").intValue();

  String str = req.header("str").value();

  // custom object type using type conversion
  MyObject object = req.header("object").to(MyObject.class);

  // file upload
  Upload upload = req.header("file").to(Upload.class);

  // multi value params
  List<String> strList = req.header("strList").toList(String.class);

  // custom object type using type conversion
  List<MyObject> listObj = req.header("objList").toList(MyObject.class);

  // custom object type using type conversion
  Set<MyObject> setObj = req.header("objList").toSet(MyObject.class);

  // optional params
  Optional<String> optStr = req.header("optional").toOptional(String.class);
});
```

## request body

Retrieval of request body is done via [request.body()](/apidocs/org/jooby/Request.html#body).

A [parser](/apidocs/org/jooby/Parser.html) is responsible for parse or convert the HTTP request body to something else.

There are a few built-in parsers for reading body as String or Reader objects. Once the body is read it, it can't be read it again.

A detailed explanation for parser is covered later. For now, all you need to know is that they can read/parse the HTTP body.

A body parser is registered in one of two ways:

* with [parser](/apidocs/org/jooby/Jooby.html#parser-org.jooby.Parser-)

```java
{
   parser(new MyParser());
}
```

* or  from inside a module:

```java
public void configure(Mode mode, Config config, Binder binder) {
  Multibinder.newSetBinder(binder, Parser.class)
        .addBinding()
        .toInstance(new MyParser());
}
```

## local variables
Local variables are bound to the current request. They are created every time a new request is processed and destroyed at the end of the request.

```java
  req.set("var", var);
  String var = rsp.get("var");
```

## guice access

In previous section we learnt you can bind/wire your objects with [Guice](https://github.com/google/guice).

You can ask [Guice](https://github.com/google/guice) to wired an object from the [request.require(type)](/apidocs/org/jooby/Request.html#require-com.google.inject.Key-)

```java
get("/", req -> {
  A a = req.require(A.class);
});
```


# response

The response object contains methods for reading and setting headers, status code and body (between others). In the next section we will mention the most important method of a response object, if you need more information please refer to the [javadoc](/apidocs/org/jooby/Response.html).

## sending data

The [rsp.send](/apidocs/org/jooby/Response.html#send-org.jooby.Result-) method is responsible for sending and writing data into the HTTP Response.

A [renderer](/apidocs/org/jooby/Renderer.html) is responsible for converting a Java Object into something else (json, html, etc..).

Let's see a simple example:

```java
get("/", (req, rsp) -> rsp.send("hey jooby"));

get("/", req -> "hey jooby"); // or just return a value and Jooby will call send for you.
```

The **send** method will ask the [Renderer API](/apidocs/org/jooby/Renderer.html) to format an object and write a response.

The resulting ```Content-Type``` when is not set is ```text/html```.

The resulting ```Status Code``` when is not set is ```200```.

Some examples:

```java
get("/", req -> {
   // text/html with 200
   String data = ...;
   return data;
});
```

```java
get("/", (req, rsp) -> {
   // text/plain with 200 explicitly 
   String data = ...;
   rsp.status(200)
        .type("text/plain")
        .send(data);
});
```

Alternative:

```java
get("/", req -> {
   // text/plain with 200 explicitly 
   String data = ...;
   return Results.with(data, 200)
        .type("text/plain");
});
```

## content negotiation

A route can produces different results based on the ```Accept``` header: 

```java
get("/", () ->
  Results
    .when("text/html", ()  -> Results.html("viewname").put("model", model))
    .when("application/json", ()  -> model)
    .when("*", ()  -> Status.NOT_ACCEPTABLE)
);
```

Performs content-negotiation on the Accept HTTP header of the request object. It select a handler for the request, based on the acceptable types ordered by their quality values. If the header is not specified, the first callback is invoked. When no match is found, the server responds with ```406 Not Acceptable```, or invokes the default callback: ```**/*```.

## response headers

Retrieval of response headers is done via [rsp.header("name")](/apidocs/org/jooby/Response.html#header-java.lang.String-). The method always returns a [Mutant](/apidocs/org/jooby/Mutant.html) and from there you can convert to any of the supported types.

Setting a header is pretty straightforward too:

```java
rsp.header("Header-Name", value).header("Header2", value);
```


# session
Sessions are created on demand via: [req.session()](/apidocs/org/jooby/Request.html#session--).

Sessions have a lot of uses cases but most commons are: auth, store information about current
user, etc.

A session attribute must be {@link String} or a primitive. Session doesn't allow to store
arbitrary objects. It is a simple mechanism to store basic data.

## options

### No timeout
There is no timeout for sessions from server perspective. By default, a session will expire when
the user close the browser (a.k.a session cookie).

## session store

A [Session.Store](/apidocs/org/jooby/Session.Store.html) is responsible for saving session data. Sessions are kept in memory, by
default using the [Session.Mem](/apidocs/org/jooby/Session.Mem.html) store, which is useful for development, but wont scale well
on production environments. An redis, memcached, ehcache store will be a better option.

### store life-cycle

Sessions are persisted every time a request exit, if they are dirty. A session get dirty if an
attribute is added or removed from it.

The <code>session.saveInterval</code> property indicates how frequently a session will be
persisted (in millis).

In short, a session is persisted when: 1) it is dirty; or 2) save interval has expired it.

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
[cookie.name("name")](/apidocs/org/jooby/Cookie.Definition.html#name-java.lang.String-) on
[Session.Definition#cookie()](/apidocs/org/jooby/Session.Definition.html#cookie).


# err

Error handler is represented by the [Err.Handler](/apidocs/org/jooby/Err.Handler.html) class and allows you to log and/or render exceptions.

## default err handler

The [default error handler](/apidocs/org/jooby/Err.DefHandler.html) does content negotiation and optionallydisplay friendly err pages using naming convention.

```java
{
  use(new TemplateEngine()); // Hbs, Ftl, etc...
  use(new Json()); // A json body formatter

  get("/", () -> {
    ...
    throw new IllegalArgumentException();
    ...
  });
}
```

### html

If a request to ```/``` has an ```Accept: text/html``` header. Then, the default err handler will
ask to a [View.Engine](/apidocs/org/jooby/View.Engine.html) to render the ```err``` view.

The default model has these attributes:

* message: exception string
* stacktrace: exception stack-trace as an array of string
* status: status code, like ```400```
* reason: status code reason, like ```BAD REQUEST```

Here is a simply ```public/err.html``` error page:

```html
<html>
<body>
  {{ "{{status" }}}}:{{ "{{reason" }}}}
</body>
</html>
```

HTTP status code will be set too.

### no html

If a request to ```/``` has an ```Accept: application/json``` header. Then, the default err handler will
ask to a [renderer](/apidocs/org/jooby/Renderer.html) to render the ```err``` model.

```json
{
  "message": "...",
  "stacktrace": [],
  "status": 500,
  "reason": "..."
}
```

In both cases, the error model is the result of ```err.toMap()``` which creates a lightweight version of the exception.

HTTP status code will be set too.

## custom err handler

If the default view resolution and/or err model isn't enough, you can create your own err handler.

```java
{
  err((req, rsp, err) -> {
    log.err("err found: ", err);
    // do what ever you want here
    rsp.send(...);
  });
}
```

Err handler are executed in the order they were provided (like routes, parser and renderers).
The first err handler that send an output wins!

## status code

Default status code is ```500```, except for:

| Exception                                | Status Code |
| ---------------------------------------- | ----------- |
| ```java.lang.IllegalArgumentException``` |  ```400```  |
| ```java.util.NoSuchElementException```   |  ```400```  |
| ```java.io.FileNotFoundException```      |  ```404```  |

### custom status code

Just throw an [Err](/apidocs/org/jooby/Err.html):

```java
throw new Err(403);
```

or add a new entry in the ```application.conf``` file:

```properties
err.com.security.Forbidden = 403
```

```java
throw new Forbidden();
```


# parser, renderer and view engine

## parser

A [Parser](/apidocs/org/jooby/Parser.html) is responsible for parsing the HTTP params and/or body to something else.

Automatic type conversion is provided when a type:

* Is a primitive, primitive wrapper or String
* Is an enum
* Is an [Upload](/apidocs/org/jooby/Upload.html)
* Has a public **constructor** that accepts a single **String** argument
* Has a static method **valueOf** that accepts a single **String** argument
* Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```
* Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```
* It is an Optional<T>, List<T>, Set<T> or SortedSet<T> where T satisfies one of previous rules


### custom parser

Suppose we want to write a custom parser to convert a value into an ```integer``. In practice we don't need such parser bc it is provided, but of course you can override the default parser and provide your own.

Let's see how to create our custom HTTP param parser:

```java

parser((type, ctx) -> {
  // 1
  if (type.getRawType() == int.class) {
    // 2
    return ctx.param(values -> Integer.parseInt(values.get(0));
  }
  // 3
  return ctx.next();
});

get("/", req -> {
   int intValue = req.param("v").intValue();
   ...
});

```

Let's have a closer look:

1. Check if current type is what we can parse to
2. We add a param callback
3. We can't deal with current type, so we ask next parser to resolve it

Now, if we ask for HTTP body

```java
get("/", req -> {
   int intValue = req.body().intValue();
   ...
});

```

Our custom parser won't be able to parse the HTTP body, because it works on HTTP parameter. In order to extend our custom parser and use it for HTTP Body we must do:

```java

parser((type, ctx) -> {
  // 1
  if (type.getRawType() == int.class) {
    // 2
    return ctx.param(values -> Integer.parseInt(values.get(0))
       .body(body -> Integer.parseInt(body.text()));
  }
  // 3
  return ctx.next();
});

```

And now we can ask for a HTTP param and/or body.

```java
get("/", req -> {
   int intValue = req.param("v").intValue();
   ...
});

post("/", req -> {
   int intValue = req.body().intValue();
   ...
});
```

[Parser](/apidocs/org/jooby/Parser.html) API is very powerful. It let you apply a parser to a HTTP param, set of param (like a form post), file uploads and/or body. But not just that, you are free to choose if your parser applies for a Java Type and/or a Media Type, like the ```Content-Type``` header.

For example a generic JSON parser looks like:

```java

parser((type, ctx) -> {
  if (ctx.type().name().equals("application/json")) {
    return ctx.body(body -> fromJSON(body.text()));
  }
  return ctx.next();
});
```

Parsers are executed in the order they are defined. Application provided parser has precedence over built-in parsers, so it it possible to override a built-in parser too!

If a param parser isn't able to resolve a param an exception will be thrown with a ```400``` status code.

If a body parser isn't able to resolve a param an exception will be thrown with a ```415``` status code.

## renderer

A [Renderer](/apidocs/org/jooby/Renderer.html) is responsible for rendering a Java Object to a series of bytes in order to send them as HTTP response.

There are a few built-in renderers:

* InputStream: copy an inputstream to the HTTP response and set a default type of: ```application/octet-stream```
* byte[]: copy bytes to the HTTP response and set a default type of: ```application/octet-stream```
* ByteBuffer: copy bytes to the HTTP response and set a default type of: ```application/octet-stream```
* Readble: copy a readable object to the HTTP response and a default type of: ```text/html```
* ToString: copy the toString() result to the HTTP response and set a default type of: ```text/html```

### custom renderer

Suppose we want to apply a custom rendering for ```MyObject```. Renderer is as simple as:

```java

render((value, ctx) -> {
  if (value instanceOf MyObject) {
     ctx.text(value.toString());
  }
});

get("/", req -> {
   return new MyObject();
});
```

Easy right?

A generic JSON renderer will looks like:

```java

render((value, ctx) -> {
  if (ctx.accepts("json")) {
     ctx.text(toJson(value));
  }
});

get("/", req -> {
   return new MyObject();
});
```

Renderer API is simple and powerful. Renderers are executed in sequentially in the order they were defined. Application specific rendering might override built-in renderers. The renderer who write the response first wins!

## view engine

A [view engine](/apidocs/org/jooby/View.Engine.html) is a specialized [renderer](/apidocs/org/jooby/Renderer.html) that ONLY accept instances of a [view](/apidocs/org/jooby/View.html).

```java
{
  use(new MyTemplateEngine());

  get("/", req -> Results.html("viewname").put("model", model);

}
```

In order to support multiples view engine, a view engine is allowed to throw a ```java.io.FileNotFoundException``` when a template can't be resolved it. This gives the chance to the next view resolver to load the template.

There is no much to say about views & engines, any other detail or documentation should be provided in the specific module (mustache, handlebars, freemarker, etc.).


# appendix: jooby.conf

```properties
###################################################################################################
# application
###################################################################################################
application {

  # environment default is: dev
  env = dev

  # contains the simple name of the Application class. set it at runtime
  # name = App.class.getSimpleName()

  # application namespace, default to app package. set it at runtime
  # ns = App.class.getPackage().getName()

  # tmpdir
  tmpdir = ${java.io.tmpdir}/${application.name}

  # path (a.k.a. as contextPath)
  path = /

  # localhost
  host = 0.0.0.0

  # default port is: 8080
  port = 8080

  # we do UTF-8
  charset = UTF-8

  # date format
  dateFormat = dd-MM-yy

  # number format, system default. set it at runtime
  # numberFormat = DecimalFormat.getInstance(${application.lang})).toPattern()

  # lang (a.k.a locale), system default. set it at runtime
  # lang = Locale.getDefault()

  # timezone, system default. set it at runtime
  # tz = ZoneId.systemDefault().getId()
}

###################################################################################################
# session defaults
###################################################################################################
session {
  # we suggest a timeout, but usage and an implementation is specific to a Session.Store implementation
  timeout = 30m

  # save interval, how frequently we must save a none-dirty session (in millis).
  saveInterval = 60s

  cookie {
    # name of the cookie
    name = jooby.sid

    # cookie path
    path = /

    # expires when the user closes the web browser
    maxAge = -1

    httpOnly = true

    secure = false
  }
}

###################################################################################################
# server defaults
###################################################################################################
server {
  http {

    HeaderSize = 8k

    # Max response buffer size
    ResponseBufferSize = 16k

    MaxRequestSize = 200k

    IdleTimeout = 30s
  }

  threads {
    Min = 20
    Max = 200
    IdleTimeout = 60s
  }

  ws {
    # The maximum size of a text message.
    MaxTextMessageSize = 16k

    # The maximum size of a binary message.
    MaxBinaryMessageSize = 16k

    # The time in ms (milliseconds) that a websocket may be idle before closing.
    IdleTimeout = 5minutes
  }
}

###################################################################################################
# runtime
###################################################################################################

# number of available processors, set it at runtime
# runtime.processors = Runtime.getRuntime().availableProcessors()
# runtime.processors-plus1 = ${runtime.processors} + 1
# runtime.processors-plus2 = ${runtime.processors} + 2
# runtime.processors-x2 = ${runtime.processors} * 2

###################################################################################################
# status codes
###################################################################################################
err.java.lang.IllegalArgumentException = 400
err.java.util.NoSuchElementException = 400
err.java.io.FileNotFoundException = 404

```

# appendix: mime.properties

```properties
mime.ai=application/postscript
mime.aif=audio/x-aiff
mime.aifc=audio/x-aiff
mime.aiff=audio/x-aiff
mime.apk=application/vnd.android.package-archive
mime.asc=text/plain
mime.asf=video/x.ms.asf
mime.asx=video/x.ms.asx
mime.au=audio/basic
mime.avi=video/x-msvideo
mime.bcpio=application/x-bcpio
mime.bin=application/octet-stream
mime.bmp=image/bmp
mime.cab=application/x-cabinet
mime.cdf=application/x-netcdf
mime.class=application/java-vm
mime.cpio=application/x-cpio
mime.cpt=application/mac-compactpro
mime.crt=application/x-x509-ca-cert
mime.csh=application/x-csh
mime.css=text/css
mime.csv=text/comma-separated-values
mime.dcr=application/x-director
mime.dir=application/x-director
mime.dll=application/x-msdownload
mime.dms=application/octet-stream
mime.doc=application/msword
mime.dtd=application/xml-dtd
mime.dvi=application/x-dvi
mime.dxr=application/x-director
mime.eps=application/postscript
mime.etx=text/x-setext
mime.exe=application/octet-stream
mime.ez=application/andrew-inset
mime.gif=image/gif
mime.gtar=application/x-gtar
mime.gz=application/gzip
mime.gzip=application/gzip
mime.hdf=application/x-hdf
mime.hqx=application/mac-binhex40
mime.htc=text/x-component
mime.htm=text/html
mime.html=text/html
mime.ice=x-conference/x-cooltalk
mime.ico=image/x-icon
mime.ief=image/ief
mime.iges=model/iges
mime.igs=model/iges
mime.jad=text/vnd.sun.j2me.app-descriptor
mime.jar=application/java-archive
mime.java=text/plain
mime.jnlp=application/x-java-jnlp-file
mime.jpe=image/jpeg
mime.jpeg=image/jpeg
mime.jpg=image/jpeg
mime.js=application/javascript
mime.json=application/json
mime.jsp=text/html
mime.kar=audio/midi
mime.latex=application/x-latex
mime.lha=application/octet-stream
mime.lzh=application/octet-stream
mime.man=application/x-troff-man
mime.mathml=application/mathml+xml
mime.me=application/x-troff-me
mime.mesh=model/mesh
mime.mid=audio/midi
mime.midi=audio/midi
mime.mif=application/vnd.mif
mime.mol=chemical/x-mdl-molfile
mime.mov=video/quicktime
mime.movie=video/x-sgi-movie
mime.mp2=audio/mpeg
mime.mp3=audio/mpeg
mime.mpe=video/mpeg
mime.mpeg=video/mpeg
mime.mpg=video/mpeg
mime.mpga=audio/mpeg
mime.ms=application/x-troff-ms
mime.msh=model/mesh
mime.msi=application/octet-stream
mime.nc=application/x-netcdf
mime.oda=application/oda
mime.odb=application/vnd.oasis.opendocument.database
mime.odc=application/vnd.oasis.opendocument.chart
mime.odf=application/vnd.oasis.opendocument.formula
mime.odg=application/vnd.oasis.opendocument.graphics
mime.odi=application/vnd.oasis.opendocument.image
mime.odm=application/vnd.oasis.opendocument.text-master
mime.odp=application/vnd.oasis.opendocument.presentation
mime.ods=application/vnd.oasis.opendocument.spreadsheet
mime.odt=application/vnd.oasis.opendocument.text
mime.ogg=application/ogg
mime.otc=application/vnd.oasis.opendocument.chart-template
mime.otf=application/vnd.oasis.opendocument.formula-template
mime.otg=application/vnd.oasis.opendocument.graphics-template
mime.oth=application/vnd.oasis.opendocument.text-web
mime.oti=application/vnd.oasis.opendocument.image-template
mime.otp=application/vnd.oasis.opendocument.presentation-template
mime.ots=application/vnd.oasis.opendocument.spreadsheet-template
mime.ott=application/vnd.oasis.opendocument.text-template
mime.pbm=image/x-portable-bitmap
mime.pdb=chemical/x-pdb
mime.pdf=application/pdf
mime.pgm=image/x-portable-graymap
mime.pgn=application/x-chess-pgn
mime.png=image/png
mime.pnm=image/x-portable-anymap
mime.ppm=image/x-portable-pixmap
mime.pps=application/vnd.ms-powerpoint
mime.ppt=application/vnd.ms-powerpoint
mime.ps=application/postscript
mime.qml=text/x-qml
mime.qt=video/quicktime
mime.ra=audio/x-pn-realaudio
mime.ram=audio/x-pn-realaudio
mime.ras=image/x-cmu-raster
mime.rdf=application/rdf+xml
mime.rgb=image/x-rgb
mime.rm=audio/x-pn-realaudio
mime.roff=application/x-troff
mime.rpm=application/x-rpm
mime.rtf=application/rtf
mime.rtx=text/richtext
mime.rv=video/vnd.rn-realvideo
mime.ser=application/java-serialized-object
mime.sgm=text/sgml
mime.sgml=text/sgml
mime.sh=application/x-sh
mime.shar=application/x-shar
mime.silo=model/mesh
mime.sit=application/x-stuffit
mime.skd=application/x-koan
mime.skm=application/x-koan
mime.skp=application/x-koan
mime.skt=application/x-koan
mime.smi=application/smil
mime.smil=application/smil
mime.snd=audio/basic
mime.spl=application/x-futuresplash
mime.src=application/x-wais-source
mime.sv4cpio=application/x-sv4cpio
mime.sv4crc=application/x-sv4crc
mime.svg=image/svg+xml
mime.swf=application/x-shockwave-flash
mime.t=application/x-troff
mime.tar=application/x-tar
mime.tar.gz=application/x-gtar
mime.tcl=application/x-tcl
mime.tex=application/x-tex
mime.texi=application/x-texinfo
mime.texinfo=application/x-texinfo
mime.tgz=application/x-gtar
mime.tif=image/tiff
mime.tiff=image/tiff
mime.tr=application/x-troff
mime.tsv=text/tab-separated-values
mime.txt=text/plain
mime.ustar=application/x-ustar
mime.vcd=application/x-cdlink
mime.vrml=model/vrml
mime.vxml=application/voicexml+xml
mime.wav=audio/x-wav
mime.wbmp=image/vnd.wap.wbmp
mime.wml=text/vnd.wap.wml
mime.wmlc=application/vnd.wap.wmlc
mime.wmls=text/vnd.wap.wmlscript
mime.wmlsc=application/vnd.wap.wmlscriptc
mime.wrl=model/vrml
mime.wtls-ca-certificate=application/vnd.wap.wtls-ca-certificate
mime.xbm=image/x-xbitmap
mime.xht=application/xhtml+xml
mime.xhtml=application/xhtml+xml
mime.xls=application/vnd.ms-excel
mime.xml=application/xml
mime.xpm=image/x-xpixmap
mime.xsd=application/xml
mime.xsl=application/xml
mime.xslt=application/xslt+xml
mime.xul=application/vnd.mozilla.xul+xml
mime.xwd=image/x-xwindowdump
mime.xyz=chemical/x-xyz
mime.z=application/compress
mime.zip=application/zip
mime.conf=application/hocon
# fonts
mime.ttf=font/truetype
mime.otf=font/opentype
mime.eot=application/vnd.ms-fontobject
mime.woff=application/x-font-woff
mime.woff2=application/font-woff2
#source map
mime.map=text/plain

```
