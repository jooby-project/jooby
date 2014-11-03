
[![Build Status](https://travis-ci.org/jooby-project/jooby.svg?branch=master)](https://travis-ci.org/jooby-project/jooby)

jooby
=====
An Express inspired web framework for Java 8 (or higher).

```java

import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", (req, rsp) ->
      rsp.send("Hey Jooby!")
    );
  }

  public static void main(final String[] args) throws Exception {
    new App().start();
  }
}

```

status
=====
version: 0.1.0-SNAPSHOT


requirements
=====
* Java 8
* Maven 3.x


quickstart
=====

Just paste this into a terminal:

    mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app -Dversion=1.0-SNAPSHOT \
    -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby \
    -DarchetypeVersion=0.1.0-SNAPSHOT

You might want to edit/change:

* -DgroupId: A Java package's name

* -DartifactId: A project's name in lower case and without spaces

* -Dversion: A project's version, like ```1.0-SNAPSHOT``` or ```1.0.0-SNAPSHOT```


Let's try it!:

    mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app -Dversion=1.0-SNAPSHOT \
    -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby \
    -DarchetypeVersion=0.1.0-SNAPSHOT
    cd my-app
    mvn jooby:run

You should see something similar to this at the end of the output:

    INFO  [2014-10-27 16:40:17,241] Logging initialized @3193ms
    INFO  [2014-10-27 16:40:17,321] jetty-9.2.3.v20140905
    INFO  [2014-10-27 16:40:17,340] Started o.e.j.s.h.ContextHandler@604616a9{/,null,AVAILABLE}
    INFO  [2014-10-27 16:40:17,357] Started ServerConnector@55802087{HTTP/1.1}{0.0.0.0:8090}
    INFO  [2014-10-27 16:40:17,358] Started @3314ms
    INFO  [2014-10-27 16:40:17,358]
    Routes:
      GET /assets/**/*    [*/*]     [*/*]    (static files)
      GET /               [*/*]     [*/*]    (anonymous)

Open a browser and type:

    http://localhost:8080/

Jooby! is up and running!!!

contents (work in progress)
=====

- [getting started](#getting-started)
  - [exploring the newly created project](#exploring the newly created project)
    - [directory layout](#directory layout)
    - [App.java](#App.java)
    - [running](#running)
- [overview](#overview)
- [routes](#routes)
  - [path patterns](#path patterns)
    - [variables](#variables)
  - [type of routes](#type of routes)
    - [inline](#inline)
    - [external](#external)
    - [mvc routes](#mvc routes)
  - [route handler vs filter](#route handler vs filter)
  - [request params](#request params)
    - [param types](#param types)
      - [path](#path)
      - [query](#query)
      - [body](#body)
    - [param precedence](#param precedence)
    - [optional params](#optional params)
  - [request headers](#request headers)
- [web sockets](#web sockets)

getting started
=====

exploring the newly created project
=====

directory layout
-----

A new directory was created: ```my-app```. Now, let's see how it looks like:

    /public
           /assets/js/index.js
           /assets/css/style.css
           /images
          welcome.html
    /config
           application.conf
           logback.xml
    /src/main/java
                  /com/mycompany/App.java

The **public** directory contains ```*.html```, ```*.js```, ```*.css```, ```*.png```, ... etc., files.

The **config** directory contains ```*.conf```, ```*.properties```, ```*.json```, ... etc., files.

The **src/main/java** contains ```*.java``` (of course) files.

**NOTE**: The three directory are part of the classpath.

**NOTE**: So this is Maven, Why don't use the default directory layout?

Good question, Java backend developers usually work with frontend developers and they wont have to look
deeply in the layout to find the resources they need (src/main/resources or src/main/webapp).

This is a matter of taste and if you find it problematic, you can just use the default directory layout of Maven.


App.java
-----


```java

import org.jooby.Jooby;

public class App extends Jooby { // 1

  {
    // 2 routes
    get("/favicon.ico");
    assets("/assets/**");

    get("/", file("welcome.html"));
  }

  public static void main(final String[] args) throws Exception {
    new App().start(); // 3. start the application.
  }

}

```

It is pretty simple to add/configure a Jooby application. Steps involved are:

1) extends Jooby

2) define some routes

3) call the ```start``` method

running
-----
Just open a console and type:

    mvn jooby:run

The plugin will compile the code if necessary and startup the application.

Of course, you can generate the IDE metadata from Maven and/or import as a Maven project on the IDE of your choice. The all you have to do is run the:

    App.java

class. After all, it is plain Java with a ```main``` method.

## overview

Jooby is an [Express](http://expressjs.com/) inspired micro web framework for Java 8 (or higher).

Jooby API mimics (as much as possible) the [Express API](http://expressjs.com/4x/api.html).

API is short and easy to learn, around 30 classes.
The most notable classes are: [Jooby.Module], [Route] and [WebSocket].

### dependencies
  * [Jetty](https://www.eclipse.org/jetty/) for HTTP NIO Web Server
  * [Guice](https://github.com/google/guice) for Dependency Injection
  * [Config](https://github.com/typesafehub/config) for configuration management

Jooby is organized as a set of reusable modules (a.k.a middleware in [Express](http://expressjs.com/)).
A module should do as minimum as possible and it should NOT make strong/hard decisions for you, or when it does, it must be 100% configurable.
For example, a module for the popular [Hibernate]() library should:

1) create a session factory

2) expose a way to control transactions

3) expose raw Hibernate API

but NOT:

1) wrap/translate Hibernate exceptions

2) wrap Hibernate API or similar

If a module does as minimum as possible, developers have the power! of setup/configure and take real advantage of native library features without noise.


## routes

Like in [Express](http://expressjs.com/) routes can be chained/stacked and executed in the same order they are defined.

A route is represent by [Route] and there are two types of handlers: [Route.Handler] and [Route.Filter].

A handler is basically the callback executed while a route is the whole thing: verb, path, handler, etc...


```java
  {
     get("/", (req, rsp) ->
       log.info("first")
     );

     get("/", (req, rsp) ->
       log.info("second")
     );

     get("/", (req, rsp) ->
       rsp.send("last")
     );
  }
```

A call to:

    http://localhost:8080

will print

    first
    second

and display: ```last``` in the browser

### path patterns

Jooby supports Ant-style path patterns:


  ```com/t?st.html``` - matches ```com/test.html``` but also ```com/tast.html``` and ```com/txst.html```

  ```com/*.html``` - matches all ```.html``` files in the ```com``` directory

  ```com/**/test.html``` - matches all ```test.html``` files underneath the ```com``` path

  ```**/*``` - matches any path at any level

  ```*``` - matches any path at any level, shorthand for ```**/*```

##### variables

In addition to Ant-style path pattern, variables pattern are also possible:

  ```/user/{id}``` - matches ```/user/*``` and give you access to the ```id``` var

  ```/user/:id``` - matches ```/user/*``` and give you access to the ```id``` var

  ```/user/{id:\\d+}``` - /user/[digits] and give you access to the numeric ```id``` var

```java
  {
     get("/users/:id", (req, rsp) ->
       rsp.send(req.param("id").stringValue())
     );

    // or with braces
    get("/users/{id}", (req, rsp) ->
       rsp.send(req.param("id").stringValue())
    );
  }
```

### type of routes
Routes are classified in 3 groups: 1) inline; 2) external; or 3) Mvc routes.

We will cover inline vs external routes here and Mvc routes are covered later.

#### inline
Inline routes use lambdas and are useful for quickstart and/or small/simple applications.

```java
{
  get("/", (req, rsp) -> rsp.send(req.path()));

  post("/", (req, rsp) -> rsp.send(req.path()));

  ... etc...
}

```

#### external
External routes are declared in a separated class and looks like:

```java
{
  get("/", new ExternalRoute());
}

...
public class ExternalRoute implements Route.Handler {

  public void handle(Request req, Response rsp) throws Exeption {
    rsp.send(req.path());
  }

}
```

Of course this is also possible with Java 8:

```java
{
  // static external route
  get("/", ExternalRoute::callback);
}

...
public class ExternalRoute {

  public static void callback(Request req, Response rsp) throws Exeption {
    rsp.send(req.path());
  }
}
```

#### mvc routes
Mvc routes are very similar to controllers in [Spring](http://spring.io/) or resources in [Jersey](https://jersey.java.net/).
They are covered later.

### route handler vs filter

There are two types of handlers [Route.Handler] and [Route.Filter]. The difference between them rely in the way
they allow/denied the execution of the next route in the chain. The next examples are identical:

```java
  {
     get("/", (req, rsp) -> {
       log.info("first");
     });

     get("/", (req, rsp) -> {
       log.info("second");
     });

     get("/", (req, rsp) ->
       rsp.send("last")
     );
  }
```

```java
  {
     get("/", (req, rsp, chain) -> {
       log.info("first");
       chain.next(req, rsp);
     });

     get("/", (req, rsp, chain) -> {
       log.info("second");
       chain.next(req, rsp);
     });

     get("/", (req, rsp) ->
       rsp.send("last")
     );
  }
```

A [Route.Handler] **always** call the next route in the chain, while a [Route.Filter] might or mightn't call the next route in chain.

### request params

The API for retriving a param is defined by:

    req.param("name")

**Always** returns a [Variant](https://github.com/jooby-project/jooby/blob/master/jooby/src/main/java/org/jooby/Variant.java).
If the param is missing or type conversion fails, a ```400 Bad Request``` response will be generated.
You can test the presence of param using [Variant.isPresent()](https://github.com/jooby-project/jooby/blob/master/jooby/src/main/java/org/jooby/Variant.java#L345)

Request params are plain string (or bytes). Params convertion is done sing these rules:

1) Be a primitive type/ or primitive wrapper

2) Have a static method named ```valueOf```, ```fromString```, ```forName```

3) Be an [Upload](https://github.com/jooby-project/jooby/blob/master/jooby/src/main/java/org/jooby/Upload.java).

4) Have a custom [Guice Type Converter](https://github.com/google/guice/wiki/CustomInjections)

5) Be List<T>, Set<T> or SortedSet<T>, where T satisfies 1, 2 or 3 above. The resulting collection is read-only.

6) Be Optional<T>, where T satisfies 1, 2, 3 or 4.

#### param types

##### path
Path params also belong to the requested URI, but they looks like: ```/user:id``` or ```/user/{id}```

    String id = req.param("id").stringValue();

##### query
Query params also belong to the requested URI, but they looks like: ```/user?id=123```

    String id = req.param("id").stringValue();

##### body
Form params are available when a ```application/x-www-form-urlencoded``` or ```multipart/form-data``` request is processed.

    String id = req.param("id").stringValue();

The same API works for file uploads:

   Upload upload = req.param("myfile").to(Upload.class);

And/or body parts:

   // multipart request named: jsonObject with a content type of application/json
   MyObject object = req.param("jsonObject").to(MyObject.class);

#### param precedence
Param resolution is done in this order:

1) path

2) query

3) body (form/multipart)

For example:

    curl -X POST -d "name=third" http://localhost:8080/user/first?name=second

Give us:

    get("/user:name", (req, rsp) -> {
      List<String> name = req.param("name").toList(String.class);
      // path
      assertEquals("first", name.get(0));
      // query
      assertEquals("second", name.get(1));
      // body
      assertEquals("third", name.get(2));
    });

And of course this is a valid call (no error):

    assertEquals("first", req.param("name").stringValue());

This is supported, but try to avoid this from your APIs because it is hard to read/follow.

#### optional params
Param are required by default, if you need/require an optional param, just use:

   Optional<String> optString = req.param("name").toOptional(String.class);

### request headers
It's identical on how request params work, except that API call is:

    String header = req.header("header").stringValue();

## web sockets

Writing WebSockets is pretty easy in Jooby:

```java
import org.joobby.Jooby;

public class App extends Jooby {

  {
    // 1.
    ws("/", (socket) -> {
       // 2. connected

       socket.onMessage(message -> {
         // 4. listen for a message
         System.out.println("received " + message.stringValue());
       });

       // 3. send a message
       socket.send("connected");
    });
  }

}
```

A websocket is listen at ```/```. The path can be as simple or complex as you need:

```java
    "/user/:id"
    "/events/*"
    "/events/**/*"
    etc...
```

The socket callback will be executed everytime a client is connected. From there you can send messages, listen for messages, or errors.

### data types

```java
    ws("/", (socket) -> {

       socket.onMessage(message -> {
         // client sent a JSON formatted message and it is parsed as MyObject
         MyObject object = message.to(MyObject.class);
       });

       MyObject value = ...;
       // MyObject will be serialized as JSON
       socket.send(value);
    })
    .consumes("application/json")
    .produces("application/json");
```

Please note that consumes/produces don't do content negotiation (like they do in routes). On WebSockets they are used to choose/pick a body (de)serializer.


For more information checkout the [WebSocket doc](http://jooby.org/apidocs/org/jooby/WebSocket.html)
