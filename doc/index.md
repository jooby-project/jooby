---
layout: index
title: doc
version: 0.4.2.1
---

documentation
=====

- [philosophy](#philosophy)
- [logging](#logging)
- [environment and config](#environment-and-config)
- [modules](#modules)
- [routes](#routes)
  - [mvc routes](#mvc-routes)
- [web sockets](#web-sockets)
  - [mvc routes](#mvc-routes)
- [request](#request)
- [response](#response)
- [session](#session)
- [working with data](#working-with-data)

# philosophy

- Simple and effective programming model for building small and large scale web applications
- Build with developer productive in mind
- Plain Java DSL for routes (no xml, or similar)
- Reflection, annotations and dependency injection are keep to minimum and in some cases it is completely optional.
- No classpath hell, default deployment model uses a normal JVM bootstrap.
- Modules are easy to use and they do as less as possible, most of the time it just bootstrap and configuration. External library isn't wrapped with something else, they just exposes raw API ready to use.


In one word? Jooby keeps it simple but yet powerful.


# logging

Logging is done via [logback](http://logback.qos.ch). Logback bootstrap and configuration is described here [logback configuration](http://logback.qos.ch/manual/configuration.html)


## bootstrap

It is useful that we can bundle logging  configuration files inside our jar, it works very well for small/simple apps.

For medium/complex apps and/or if you need/want to debug errors the configuration files should/must be outside the jar, so you can turn on/off loggers, change log level etc..

On such cases all you have to do is start the application with the location of the logback configuration file:

    java -Dlogback.configFile=logback.xml -jar myapp.jar

The ```-Dlogback.configFile``` property controls the configuration file to load. More information can be found [here](http://logback.qos.ch/manual/configuration.html)


# environment and config

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

7) There is custom Guice type converter for the type

It is also possible to inject the root ```com.typesafe.config.Config``` object or a child of it.

## special properties

### application.env

Jooby internals and the module system rely on the ```application.env``` property. By defaults, this property is set to ```dev```.

For example, the [development stage](https://github.com/google/guice/wiki/Bootstrap) is set in [Guice](https://github.com/google/guice) when ```application.env == dev```. A module provider, might decided to create a connection pool, cache, etc when ```application.env != dev ```.

This special property is represented at runtime with the [Env]({{apidocs}}/org/jooby/Env.html) class.

### application.secret

If present, the session cookie will be signed with the ```application.secret```.

### default properties

Here is the list of default properties provided by  Jooby:

* **application.name**: describes the name of your application. Default is: *app.getClass().getSimpleName()*
* **application.charset**: charset to use. Default is: *UTF-8*
* **application.lang**: locale to use. Default is: *Locale.getDefault()*. A ```java.util.Locale``` can be injected.
* **application.dateFormat**: date format to use. Default is: *dd-MM-yyyy*. A ```java.time.format.DateTimeFormatter``` can be injected.
* **application.numberFormat**: number format to use. Default is: *DecimalFormat.getInstance("application.lang")*
* **application.tz**: time zone to use. Default is: *ZoneId.systemDefault().getId()*. A ```java.time.ZoneId``` can be injected.

## precedence

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

If you find this impractical, then this option will work for you.

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


# modules

Modules are a key concept for building reusable and configurable piece of software. Modules like in [Guice](https://github.com/google/guice) are used to wire services, connect data, etc...

A module is usually a small piece of software that bootstrap and configure common code and/or an external library.

## do less and be flexible

A module should do as less as possible (key difference with other frameworks). A module for a library *X* should:

* Bootstrap X
* Configure X
* Exposes raw API of X

This means a module should NOT create wrapper for a library. Instead, it should provide a way to extend, configure and use the raw library.

This principle, keep module usually small, maintainable and flexible.

## api

A module is represented by the [Jooby.Module](http://jooby.org/apidocs/org/jooby/Jooby.Module.html) class. The configure callback looks like:

```java
public class M1 implements Jooby.Module {
    public void configure(Env env, Config config, Binder binder) {
      binder.bind(...).to(...);
    }
}
```

The configure callback is similar to a [Guice module](https://github.com/google/guice), except you can access to the [Env](http://jooby.org/apidocs/org/jooby/Env.html) and [Type Safe Config](https://github.com/typesafehub/config) objects.

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

## registering a module

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

## defining routes
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

Jooby offers serveral flavors for creating with routes:

### Zero arg handler

```java
get("/", () -> "hey jooby");
```

### One arg handler: req

```java
get("/", req -> "hey jooby");
```

### Two args handler: req and rsp

```java
get("/", (req, rsp) -> rsp.send("hey jooby"));
```

### Three args handler: req, rsp and chain (a.k.a as Filter)

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
get("/user/{id:\d+}", req -> "hey " + req.param("id").intValue());
```

Reques params are covered later, for now all you need to know is that you can access to a path parameter using the [Request.param(String)]({{apidocs}}/org/jooby/Request.param(java.lang.String)).

### ant style patterns

  ```com/t?st.html``` - matches ```com/test.html``` but also ```com/tast.html``` and ```com/txst.html```

  ```com/*.html``` - matches all ```.html``` files in the ```com``` directory

  ```com/**/test.html``` - matches all ```test.html``` files underneath the ```com``` path

  ```**``` - matches any path at any level

  ```*``` - matches any path at any level, shortcut for ```**```

## order

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


# mvc routes

Mvc routes are like **controllers** in [Spring](http://spring.io) and/or **resources** in [Jersey](https://jersey.java.net/) with some minor enhancements and/or simplifications.

```java
@Path("/routes")
public class MyRoutes {

  @GET
  public View home() {
    return View.of("home", "model", model);
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
  public View home() {
    return View.of("home", "model", model);
  }
}
```

A method annotated with [GET](http://jooby.org/apidocs/org/jooby/mvc/GET.html), [POST](http://jooby.org/apidocs/org/jooby/mvc/POST.html),... (or any of the rest of the verbs) is considered a route handler (web method).

## registering a mvc route

Mvc routes must be registered, there is no auto-discover feature (and it won't be), classpath scanning, ..., etc.

We learnt that the order in which you define your routes has a huge importance and it defines how your app will work. This is one of the reason why mvc routes need to be explicitly registered. The other reason is bootstrap time, declaring the route explicitly helps to reduce bootstrap time.

So, how do I register a mvc route? Easy: in the same way everything else is registered in Jooby... from your app class:

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


## binding req params

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

As you might already noticed, Jooby uses the method param name and binded it to the request param. If you want explicit mapping and/or the req param isn't a valid Java identifier:

```java
   @GET
   public List<Object> search(@Named("req-param") String reqParam) {
    ...
   }
```

## binding req body

Injecting a req body work in the same way:

```java
  @POST
  public View search(MyObject object) {
  ... do something with the uploaded file
  }
```

## binding req headers

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

## mvc response

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
public View home() {
  return View.of("home", "model", model);
}
```

### customizing the response

If you need to deal with HTTP metadata like: status code, headers, etc... use a [org.jooby.Result](http://jooby.org/apidocs/org/jooby/Result.html)

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
   ws("/", (ws) -> {
     ws.onMessage(message -> System.out.println(message.value()));
     
     ws.send("connected");
   });
}
```

A [web socket](http://jooby.org/apidocs/org/jooby/WebSocket.html) consist of a **path pattern** and a [handler](http://jooby.org/apidocs/org/jooby/WebSocket.Handler.html).

A **path pattern** can be as simple or complex as you need. All the path patterns supported by routes are supported here.

A [handler](http://jooby.org/apidocs/org/jooby/WebSocket.Handler.html) is executed on new connections, from there we can listen for message, errors and/or send data to the client.

Keep in mind that **web socket** are not like routes. There is no stack/pipe or chain.

You can mount a socket to a path used by a route, but you can't have two or more web sockets under the same path.

## guice access

You can ask [Guice](https://github.com/google/guice) to wired an object from the [ws.require(type)](http://jooby.org/apidocs/org/jooby/WebSocket.html#require-com.google.inject.Key-)

```java
ws("/", (ws) -> {
  A a = ws.require(A.class);
});
```

## consumes

Web socket can define a type to consume:

```
{
   ws("/", (ws) -> {
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

```
{
   ws("/", (ws) -> {
     MyObject object = ..;
     ws.send(object);
   })
   .produces("json");
}
```

This is just an utility method for formatting Java Objects as text message. Produces in web sockets has nothing to do with content negotiation. Content negotiation is route concept, it doesn't apply for web sockets.



{{request.md}}

{{response.md}}

## session
Sessions are created on demand via: {@link Request#session()}.

Sessions have a lot of uses cases but most commons are: auth, store information about current
user, etc.

A session attribute must be {@link String} or a primitive. Session doesn't allow to store
arbitrary objects. It is a simple mechanism to store basic data.

### options

#### No timeout
There is no timeout for sessions from server perspective. By default, a session will expire when
the user close the browser (a.k.a session cookie).

### session store

A {@link Session.Store} is responsible for saving session data. Sessions are kept in memory, by
default using the {@link Session.Mem} store, which is useful for development, but wont scale well
on production environments. An redis, memcached, ehcache store will be a better option.

#### store life-cycle

Sessions are persisted every time a request exit, if they are dirty. A session get dirty if an
attribute is added or removed from it.

The <code>session.saveInterval</code> property indicates how frequently a session will be
persisted (in millis).

In short, a session is persisted when: 1) it is dirty; or 2) save interval has expired it.

### cookie

#### max-age
The <code>session.cookie.maxAge</code> sets the maximum age in seconds. A positive value
indicates that the cookie will expire after that many seconds have passed. Note that the value is
the <i>maximum</i> age when the cookie will expire, not the cookie's current age.

A negative value means that the cookie is not stored persistently and will be deleted when the
Web browser exits.

Default maxAge is: <code>-1</code>.

#### signed cookie
If the <code>application.secret</code> property has been set, then the session cookie will be
signed it with it.

#### cookie's name

The <code>session.cookie.name</code> indicates the name of the cookie that hold the session ID,
by defaults: <code>jooby.sid</code>. Cookie's name can be explicitly set with
{@link Cookie.Definition#name(String)} on {@link Session.Definition#cookie()}.


# working with data

## body parser

A [BodyParser](http://jooby.org/apidocs/org/jooby/BodyParser.html) is responsible for parsing the HTTP body to something else.

A [BodyParser](http://jooby.org/apidocs/org/jooby/BodyParser.html) has three(3) methods:

* **types**: list of [media types](http://jooby.org/apidocs/org/jooby/MediaType.html) supported by the body parser.
* **canParse**(*type)*: test if the Java type is supported by the body parser.
* **parse***(type, reader)*: do the actual parsing using the type and reader.

In the next example we will try to read the HTTP Body as **MyObject**.

```java
post("/", (req, rsp) -> {
   MyObject obj = req.body(MyObject.class);
   ...
});
```

A call like:

    curl -X POST -H 'Content-Type: application/json' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/

Results in ```415 - Unsupported Media Type```. That is because Jooby has no idea how to parse ```application/json```. For that, we need a **json** parser.

Let's said we need to implement a JSON body parser (in real life you wont ever implement a json parser, this is just to demonstrate how it works):

```java
public class Json implements BodyParser {

  public List<MediaType> types() {
    return ImmutableList.of(MediaType.json);
  }

  public boolean canParse(TypeLiteral<?> type) {
    return true; 
  }

  public <T> T parse(TypeLiteral<?> type, Context ctx) throws Exception {
    ... parse it!
  }
}
```

Using it:

```java
{
  use(new Json()); // now Jooby has a json parser

  post("/", req -> {
    MyObject obj = req.body(MyObject.class);
    return obj;
  });
}
```

**How it works**?

A route by default consumes ```*/*``` (any media type). Jooby will find/choose the **parser** which best matches the ```Content-Type``` header.

The ```Content-Type``` header is compared against the [parser.types()](http://jooby.org/apidocs/org/jooby/BodyParser.html#types--) method.

Once an acceptable media type is found it call the **canParse** method of the [parser](http://jooby.org/apidocs/org/jooby/BodyParser.html).

### consumes

The **consumes** method control what a route can consume or parse explicitly.

```java
{
  post("/", req -> {
    MyObject obj = req.body(MyObject.class);
  })
   .consumes("application/json");
}
```

**200** response:

    curl -X POST -H 'Content-Type: application/json' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/

**415** response bc **application/xml** isn't supported:

    curl -X POST -H 'Content-Type: application/xml' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/


In general, you hardly will use **consumes** in your routes. It has been created to give you more control on your routes and (more or less) explicitly document what is acceptable for your route. In real life, you won't use it too much but it will depend on your app requirements. For example if you need more than **json** for your routes (xml, yaml, etc..).

Another small advantage of using **consumes** is that the ```415``` response can be detected early (at the time a route is resolved) and not later or lazy (at the time you ask for type conversion).

Keep in mind, you still need a **parser** for your media types. For example:

```java
{
  post("/", req -> {
    MyObject obj = req.body(MyObject.class);
  })
   .consumes("application/json", "application/xml");
}
```

Require two parsers one for **json** and one for **xml**.

## body formatter

A [BodyFormatter](http://jooby.org/apidocs/org/jooby/BodyFormatter.html) is responsible for format a Java Object to a series of bytes in order to send them as HTTP response.

A [BodyFormatter](http://jooby.org/apidocs/org/jooby/BodyFormatter.html) has three(3) methods:

* **types**: list of [media types](http://jooby.org/apidocs/org/jooby/MediaType.html) supported by the body formatter.
* **canFormat**(*type)*: test if the Java type is supported by the body formatter.
* **formatter***(data, writer)*: do the actual formatting using the data and writer.

In the next example we will try to send **MyObject** as HTTP response.

```java
get("/", req -> {
   MyObject obj = ...
   return obj;
});
```

A call like:

    curl http://localhost:8080/

Give us a ```text/html``` and body content is ```obj.toString()```

    curl -H 'Accept: application/json' http://localhost:8080/

Results in ```406 - Not Acceptable```. That is because Jooby has no idea how to format ```application/json```. For that, we need a **json** formatter.

Let's said we need to implement a JSON body formatter (in real life you wont ever implement a json formatter, this is just to demonstrate how they work):

```java
public class Json implements BodyFormatter {

  public List<MediaType> types() {
    return ImmutableList.of(MediaType.json);
  }

  public boolean canFormat(TypeLiteral<?> type) {
    return true; 
  }

  public void format(Object data, Context ctx) throws Exception {
    ... format and write it!
  }
}
```

Using it:

```java
{
  use(new Json()); // now Jooby has a json formatter

  post("/", (req, rsp) -> {
     MyObject obj = ...
     rsp.send(obj);
  });
}
```

**How it works**?

A route by default produces ```*/*``` (any media type). Jooby will find/choose the **formatter** who best matches the ```Accept``` header.

The ```Accept``` header is compared against the [formatter.types()](http://jooby.org/apidocs/org/jooby/BodyFormatter.html#types--) method.

Once an acceptable media type is found it call the **canFormat** method of the [formatter](http://jooby.org/apidocs/org/jooby/BodyFormatter.html).

### produces

The **produces** method control what a route can accept or format explicitly.

```java
{
  post("/", req -> {
    MyObject obj = ...
    return obj;
  })
   .produces("application/json");
}
```

**200** response:

    curl -H 'Accept: application/json' http://localhost:8080/

**406** response bc **application/xml** isn't supported:

    curl 'Accept: application/xml' http://localhost:8080/

In general, you hardly will use **produces** in your routes. It has been created to give you more control on your routes and (more or less) explicitly document what is acceptable for your route. In real life, you won't use it too much but it will depend on your app requirements.

Another small advantage of using **produces** is that the ```406``` response can be detected early (at the time a route is resolved) and not lazily (at the time you ask for type conversion).

Keep in mind, you still need a **formatter** for your media types. For example:

```java
{
  post("/", req -> {
    MyObject obj = ...
    return obj;
  })
   .produces("application/json", "application/xml");
}
```

Require two formatters one for **json** and one for **xml**.

## view engine

A [view engine](http://jooby.org/apidocs/org/jooby/View.Engine.html) is a specialized [body formatter](http://jooby.org/apidocs/org/jooby/BodyFormatter.html) that ONLY accept instances of a [view](http://jooby.org/apidocs/org/jooby/View.html).

```java
{
  use(new MyTemplateEngine());

  get("/", req -> View.of("viewname", "model", model);

}
```

There is no much to say about views & engines, any other detail or documentation should be provided in the specific module (mustache, handlebars, freemarker, etc.).

## response format (a.k.a content negotiation)

A route can produces different results base on the ```Accept``` header: 

```java
get("/", () ->
  Results
    .when("text/html", ()  -> View.of("viewname", "model", model))
    .when("application/json", ()  -> model)
    .when("*", ()  -> Status.NOT_ACCEPTABLE)
);
```

Performs content-negotiation on the Accept HTTP header of the request object. It select a handler for the request, based on the acceptable types ordered by their quality values. If the header is not specified, the first callback is invoked. When no match is found, the server responds with ```406 Not Acceptable```, or invokes the default callback: ```**/*```.

