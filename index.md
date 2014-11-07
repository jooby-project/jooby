---
layout: index
---


[![Build Status](https://travis-ci.org/jooby-project/jooby.svg?branch=master)](https://travis-ci.org/jooby-project/jooby)

# jooby

A micro-web framework for Java 8.

```java

import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", (req, rsp) ->
      rsp.send("Hey Jooby!")
    );
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}

```

# versioning

Jooby uses [semantic versioning](http://semver.org/) for releases.

API is considered unstable while release version is: ```0.x.x``` and it might changes and/or broke without previous notification.

# requirements

* Java 8
* Maven 3.x

# quickstart

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

    INFO  [2014-11-04 09:20:12,526] Logging initialized @645ms
    INFO  [2014-11-04 09:20:12,574] jetty-9.2.3.v20140905
    INFO  [2014-11-04 09:20:12,599] Started o.e.j.s.h.ContextHandler@26b3fd41{/,null,AVAILABLE}
    INFO  [2014-11-04 09:20:12,612] Started ServerConnector@53e8321d{HTTP/1.1}{0.0.0.0:8080}
    INFO  [2014-11-04 09:20:12,736] Started ServerConnector@74ea2410{SSL-HTTP/1.1}{0.0.0.0:8443}
    INFO  [2014-11-04 09:20:12,736] Started @859ms
    INFO  [2014-11-04 09:20:12,736] 
    Routes:
      GET /favicon.ico    [*/*]     [*/*]    (anonymous)
      GET /assets/**/*    [*/*]     [*/*]    (static files)
      GET /               [*/*]     [*/*]    (anonymous)

Open a browser and type:

    http://localhost:8080/

Jooby! is up and running!!!

# getting started

## exploring the newly created project

A new directory was created: ```my-app```. Now, let's see how it looks like:

    /public
           /assets/js/index.js
           /assets/css/style.css
           /images
          welcome.html
    /config
           application.conf
           logback.dev.xml
    /src/main/java
                  /com/mycompany/App.java

The **public** directory contains ```*.html```, ```*.js```, ```*.css```, ```*.png```, ... etc., files.

The **config** directory contains ```*.conf```, ```*.properties```, ```*.json```, ... etc., files.

The **src/main/java** contains ```*.java``` (of course) files.

**NOTE**: The three directory are part of the classpath.

**NOTE**: So this is Maven, Why don't use the default directory layout?

Good question, in a Java project not all the team members are backend developers. The **public** folder
was specially created for frontend developer or web designers with no experience in Java. 

This is a matter of taste and if you find it problematic, you can use the default directory layout of Maven.


### App.java

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
    new App().start(args); // 3. start the application.
  }

}

```

Steps involved are:

1) extends Jooby

2) define some routes

3) call the ```start``` method

### running

Just open a console and type:

    mvn jooby:run

The maven plugin will compile the code (if necessary) and startup the application.

Of course, you can generate the IDE metadata from Maven and/or import as a Maven project on your favorite IDE.
Then all you have to do is run the: ```App.java``` class. After all, this is plain Java with a ```main``` method.


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


# modules

Modules are reusable and configurable piece of software. Modules like in [Guice](https://github.com/google/guice) are used to wire services, connect data, etc...

## app module
An application module is represented by the [Jooby.Module](http://jooby.org/apidocs/org/jooby/Jooby.Module.html) class. The configure callback looks like:

```java
public class M1 implements Jooby.Module {
    public void configure(Mode mode, Config config, Binder binder) {
      binder.bind(...).to(...);
    }
}
```

Configure callback is similar to a [Guice module](https://github.com/google/guice), except you can acess to the [Mode](http://jooby.org/apidocs/org/jooby/Mode.html) and [Type Safe Config](https://github.com/typesafehub/config) objects.

In addition to the **configure** callback, a module in Jooby has two additional and useful methods:  **start** and **close**. If your module need/have to start an expensive resource, you should do it in the start callback and dispose/shutdown in the close callback.

From a module, you can bind your objects to the default [Guice scope](https://github.com/google/guice/wiki/Scopes) and/or to the Singleton scope.

An app module (might) defines his own set of defaults properties:

```java
public class M1 implements Jooby.Module {
    public void configure(Mode mode, Config config, Binder binder) {
      binder.bind(...).to(...);
    }

   public Config config() {
     return Config.parseResources(getClass(), "m1.properties");
   }
}
```
This is useful for setting defaults values or similar.

**Finally**, an app module is registered at startup time:

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

  ...
}
```

Now, let's say M1 has a ```foo=bar``` property and M2 ```foo=foo``` then ```foo=foo``` wins! because last registered module can override/hide a property from previous module.

Cool, isn't?


## request module
A request module is represented by the [Request.Module]({{apidocs}}/Request.Module.html). The configure callback looks like:

```java
public class RM1 implements Request.Module {
   public void configure(Binder binder) {
      binder.bind(...).to(...);
   }
}
```

A request module is useful to wire and provide request scoped objects. In jooby, if you need a request scoped object you must bind it from a request module. 

A **new child injector** is created every time a new request is processed by Jooby.

### scope

If you need a single instance per request you need to bind the object with the **@Singleton** annotation. Otherwise, a new object will be created every time Guice need to inject a dependency.

```java
public class RM1 extends Request.Module {
   public void configure(Binder binder) {
      // request scoped
      binder.bind(...).to(...).in(Singleton.class);
     // or
     binder.bind(...).toInstance(...);
   }
}
```

Annotations like **RequestScoped** or **SessionScoped** are not supported in Jooby and they are ignored. A **request scoped** objects must be **explicitly declared inside a request module**.

There are two way of registering a request module:

1) by calling [Jooby.use(module)]({{apidocs}}/org/jooby/Jooby.html#use-org.jooby.Request.Module-)

```java
{
  // as lambda
  use(binder -> {
    binder.bind(...).to(...);
  });

  // as instance
  use(new RM1());
}
```

2) from [Jooby.Module.configure(module)]({{apidocs}}/org/jooby/Jooby.htmll#configure-org.jooby.Mode-com.typesafe.config.Config-com.google.inject.Binder-)

```java
public class M1 implements Jooby.Module {
    public void configure(Mode mode, Config config, Binder binder) {
      Multibinder<Request.Module> rm = Multibinder.newSetBinder(binder, Request.Module.class);
      rm.addBinding().toInstance(b -> {
        b.bind(...).toInstance(...);
      });
    }
}
```


# config files

Jooby delegates configuration management to [TypeSafe Config](https://github.com/typesafehub/config). If you aren't familiar with [TypeSafe Config](https://github.com/typesafehub/config) please take a few minutes to discover what [TypeSafe Config](https://github.com/typesafehub/config) can do for you.

## application.conf

By defaults Jooby will attempt to load an ```application.conf``` file from root of classpath. Inside the file you can add/override any property you want.

## injecting properties

Any property can be injected using the ```javax.inject.Named``` annotation and automatic type conversion is provided when a type:

1) Is a primitive, primitive wrapper or String

2) Is an enum

1) Has a public **constructor** that accepts a single **String** argument

2) Has a static method **valueOf** that accepts a single **String** argument

3) Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```

4) Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```

5) There is custom Guice type converter for the type

It is also possible to inject a ```com.typesafe.config.Config``` object.

## special properties

### application.mode

Jooby internals and the module system rely on the ```application.mode``` property. By defaults, this property is set to ```dev```.

For example, the [development stage](https://github.com/google/guice/wiki/Bootstrap) is set in [Guice](https://github.com/google/guice) when ```application.mode == dev```. A module provider, might decided to create a connection pool, cache, etc when ```application.mode != dev ```.

This special property is represented at runtime with the [Mode]({{apidocs}}/org/jooby/Mode.html) class.

### application.secret

The session cookie is signed with an ```application.secret```, while you are in **dev** you aren't required to provide an ```application.secret```. A secret is required when environment isn't **dev** and if you fail to provide a secret your application wont startup.

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

System properties can override any other property. A sys property is be set at startup time, like: 

    java -jar myapp.jar -Dapplication.secret=xyz

### file://[application].[mode].[conf] 

The use of this conf file is optional, because Jooby recommend to deploy your as a **fat jar** and all the properties files should be bundled inside the jar.

If you find this impractical, then this option will work for you.

Let's said your app includes a default property file: ```application.conf``` bundled with your **fat jar**. Now if you want/need to override two or more properties, just do this

* find a directory to deploy your app
* inside that directory create a file: ```application.conf```
* start the app from same directory

That's all. The file system conf file will takes precedence over the classpath path config files overriding any property.

A good practice is to start up your app with a **mode**, like:

    java -jar myapp.jar -Dapplication.mode=prod

The process is the same, except but this time you can name your file as: 

    application.prod.conf

### cp://[application].[mode].[conf]

Again, the use of this conf file is optional and works like previous config option, except that here the **fat jar** was bundled with all your config files (dev, stage, prod, etc.)

Example: you have two config files: ```application.conf``` and ```application.prod.conf````. Both files were bundled with the **fat jar**, starting the app in **prod** mode is:

    java -jar myapp.jar -Dapplication.mode=prod

So here the ```application.prod.conf``` will takes precedence over the ```application.conf``` conf file.

This is the recommended option from Jooby, because your app doesn't have a external dependency. If you need to deploy the app in a new server all you need is your **fat jar**

### [application].[conf]

This is your default config files and it should be bundle inside the **fat jar**. As mentioned early, the default name is: **application.conf**, but if you don't like it or need to change it just call **use** in Jooby:

```java
  {
     use(ConfigFactory.parseResources("myconfig.conf"));
  }
```


### [modules in reverse].[conf]

As mentioned in the [modules](#modules) section a module might defines his own set of properties.

```
  {
     use(new M1());
     use(new M2());
  }
```

In the previous example the M2 modules properties will take precedence over M1 properties.

As you can see the config system is very powerful and can do a lot for you.



# logging

Logging is done via [logback](http://logback.qos.ch). Logging configuration files looks like: ```logback.[mode].xml```.

Configuration files can be bundle with the **fat jar**. This for example is allowed:

    /config
               logback.dev.xml
               logback.stage.xml
               logback.prod.xml

In **dev** the file with the **logback.dev.xml** will be used for logging. The others can be selected in one of two ways:

* **application.mode**

```
    java -Dapplication.mode=stage -jar myapp.jar // logback.stage.xml

    java -Dapplication.mode=prod -jar myapp.jar // logback.prod.xml
 
```

* **logback.configurationFile**

```
    java -DconfigurationFile=logback.stage.xml -jar myapp.jar // logback.stage.xml

    java -DconfigurationFile=logback.prod.xml -jar myapp.jar // logback.prod.xml
```

## bootstrap

It is useful that we can bundle logging  configuration files inside our jar, it works very well for small/simple apps.

For medium/complex apps and/or if you need want to debug errors the configuration files should /must be outside the jar, so you can turn on/off loggers, change log level etc..

For such cases all you have to do is to put the ```logback.[mode].xml``` file outside the jar and in the same directory where the jar is.

    cd /myapp-dir
    ls
    myapp.jar logback.prod.xml

The bootstrap process look for a file in the same directory where you app was launched (user.dir property) if the file is found there it will be selected. Otherwise, it fallback to the root of the classpath.

If at the time you started your app the console shows a lot of logs statement, that is because log wasn't configured properly. Either, the config file is missing or it has syntax errors.
 


# routes

A route describes the interface for making requests to your web app. It combines a HTTP verb (a.k.a. HTTP request method) and a path pattern.

A route has an associated  handler, which does the job of performing ab action in the app and sending a HTTP response.

## defining routes
A route definition looks like:

```java
get("/", (req, rsp) -> rsp.send("hey jooby"));
```

We just created a route to handle GET request at the root of our app. Any other verb can be created in the same way.

If you need a POST all you have to do is:

```java
post("/", (req, rsp) -> rsp.send("hey jooby"));
```
And of course if you want or need to listen to any verb:

```java
use("*", "/", (req, rsp) -> rsp.send("hey jooby"));
```

It is possible to name a route explicitly:

```java
get("/", (req, rsp) -> rsp.send("hey jooby"))
   .name("salute");
```

By default a route named as **anonymous**. Naming a route is useful for debugging purpose, specially if you two or more routes mounted on the same path.

## path patterns

### static patterns

```java
get("/", (req, rsp) -> rsp.send("hey jooby"));

get("/help", (req, rsp) -> rsp.send("hey jooby"));

get("/mail/inbox", (req, rsp) -> rsp.send("hey jooby"));
```

### var/regex patterns

```java
get("/user/:id", (req, rsp) -> rsp.send("hey " + req.param("id").stringValue()));

// alternative syntax
get("/user/{id}", (req, rsp) -> rsp.send("hey " + req.param("id").stringValue()));

// regex
get("/user/{id:\\d+}", (req, rsp) -> rsp.send("hey " + req.param("id").intValue()));
```

[request params](#request params) are covered later, for now all you need to know is that you can access to a path parameter using the [Request.param(String)]({{apidocs}}/org/jooby/Request.param(java.lang.String)).

### ant style patterns

  ```com/t?st.html``` - matches ```com/test.html``` but also ```com/tast.html``` and ```com/txst.html```

  ```com/*.html``` - matches all ```.html``` files in the ```com``` directory

  ```com/**/test.html``` - matches all ```test.html``` files underneath the ```com``` path

  ```**/*``` - matches any path at any level

  ```*``` - matches any path at any level, shortcut for ```**/*```

## order

Routes are executed in the order they are defined. So the ordering of routes is crucial to the behavior of an app. Let's review this fact via some examples.

```java
get("/abc", (req, rsp) -> rsp.send("first"));

get("/abc", (req, rsp) -> rsp.send("second"));
```

A call to ```/abc``` produces a response of ```first```. If we revert the order:

```java
get("/abc", (req, rsp) -> rsp.send("second"));

get("/abc", (req, rsp) -> rsp.send("first"));
```

It produces a response of ```second```. As you can see **order is very important**. Second route got ignored and due that we are trying to send a response after we sent one already a warning will be logged.

Now, why is it allowed to have two routes for the same exactly path?

Because we want to **filter** or **intercept** routes.

A route handler accept a third parameter, commonly named chain, which refers to the next route handler in line. We will learn more about it in the next section:

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

The two (2) args route handler is represented by [Route.Handler]({{apidocs}}/org/jooby/Route.Handler).

The three (3) args route handler is represented by [Route.Filter]({{apidocs}}/org/jooby/Route.Filter).

Try to guess the output of:

```java
get("/", (req, rsp, chain) -> rsp.send("first"));

get("/", (req, rsp, chain) -> rsp.send("second"));

get("/", (req, rsp) -> rsp.send("third"));
```

Will the server print all of them, or "first" or, "third"?

The server will print just "one". The act of doing a ```rsp.send()``` terminates the flow of the request then and there; the request is not passed on to any other route handler.

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

The 3rd arg is required if you need to decided if the next route need to be executed or not. If you always call **chain.next** the 3rd arg isn't require and does is exactly what the 2arg handler does: **always call chain.next**

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


## request

The request object contains methods for reading params, headers and body (between others). In the next section we will mention the most important method of a request object, if you need more information please refer to the [javadoc]({{apidocs}}/org/jooby/Request.html).

### request params

The method is defined by the [req.param("name")]({{apidocs}}/org/jooby/Request.html#param-java.lang.String-) method.

The [req.param("name")]({{apidocs}}/org/jooby/Request.html#param-java.lang.String-) **always** returns a [Mutant]({{apidocs}}/org/jooby/Mutant.html) instance. A mutant had several utility method for doing type conversion.

Some examples:

```java
get("/", (req, rsp) -> {
  int iparam = req.param("intparam").intValue();

  String str = req.param("str").stringValue();

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

#### param types and precedence

A request param can be present at (first listed are higher precedence):

1) path: */user:id*

2) query: */user?id=...* 

3) body: */user* and params are *formurlenconded* or *multipart*

Now, let's suppose a very poor API design and we have a route handler that accept an **id** param in the 3 forms:

A call like:

    curl -X POST -d "name=third" http://localhost:8080/user/first?name=second

Produces:

```java
get("/user/:id", (req, rsp) -> {
  // path param at idx = 0
  assertEquals("first", req.param("id").stringValue());
  assertEquals("first", req.param("id").toList(String.class).get(0));

  // query param at idx = 1
  assertEquals("second", req.param("id").toList(String.class).get(1));

  // body param at idx = 2
  assertEquals("third", req.param("id").toList(String.class).get(2));
});
```

An API like this should be avoided and we mention here this is possible so you can take note and figure it out if something doesn't work as you expect.

#### param type conversion

Automatic type conversion is provided when a type:

* Is a primitive, primitive wrapper or String
* Is an enum
* Is an [Upload]({{apidocs}}/org/jooby/Upload.html)
* Has a public **constructor** that accepts a single **String** argument
* Has a static method **valueOf** that accepts a single **String** argument
* Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```
* Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```
* There is custom Guice type converter for the type
* It is an Optional<T>, List<T>, Set<T> or SortedSet<T> where T satisfies one of previous rules

### request headers

Retrieval of request headers is done via: [request.header("name")]({{}}Request.html#header-java.lang.String-). All the explained before for [request params](#request params) apply for headers too.

```java
get("/", (req, rsp) -> {
  int iparam = req.header("intparam").intValue();

  String str = req.header("str").stringValue();

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

### request body

Retrieval of request body is done via [request.body(type)]({{apidocs}}/org/jooby/Request.html#body-com.google.inject.TypeLiteral-).

A [body parser]({{apidocs}}/org/jooby/Body.Parser.html) is responsible for parse or convert the HTTP request body to something else.

There are a few built-in parsers for reading body as String or Reader objects. Once the body is read it, it can't be read it again. Jooby distribution includes a [Jackson module](http://jackson.codehaus.org/) that provides support for **json**.

A detailed explanation for body parser is covered later. For now, all you need to know is that they can read/parse the HTTP body.

A body parser is registered in one of two ways:

* with [use]({{apidocs}}/org/jooby/Jooby.html#use-org.jooby.Body.Parser-)

```java
{
   use(new Json());
}
```

* or  from inside an app module:

```java
public void configure(Mode mode, Config config, Binder binder) {
  Multibinder.newSetBinder(binder, Body.Formatter.class)
        .addBinding()
        .toInstance(new MyFormatter());
}
```

### guice access

In previous section we learn you can bind/wire your objects with [Guice](https://github.com/google/guice).

We also learn that a new child injector is created and binded to the current request.

You can ask [Guice](https://github.com/google/guice) to wired an object from the [request.getInstance(type)](http://jooby.org/apidocs/org/jooby/Request.html#getInstance-com.google.inject.Key-)

```java
get("/", (req, rsp) -> {
  A a = req.getInstance(A.class);
});
```


## response

The response object contains methods for reading and setting headers, status code and body (between others). In the next section we will mention the most important method of a request object, if you need more information please refer to the [javadoc]({{apidocs}}/org/jooby/Response.html).

### sending data

The [rsp.send](http://jooby.org/apidocs/org/jooby/Response.html#send-org.jooby.Body-) method is responsible for sending and writing a body into the HTTP Response.

A [body formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter) is responsible for converting a Java Object into something else (json, html, etc..).

Let's see a simple example:

```java
get("/", (req, rsp) -> {
   rsp.send(data);
});
```

The **send** method select the best [body formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter) to use base on the ```Accept``` header and if the current data type is supported.

The resulting ```Content-Type``` when not set is the first returned by the  [formatter.types()](http://jooby.org/apidocs/org/jooby/Body.Formatter#types) method.

The resulting ```Status Code``` when not set is ```200```.

Some examples:

```java
get("/", (req, rsp) -> {
   // text/html with 200
   String data = ...;
   rsp.send(data);
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

### response headers

Retrieval of response headers is done via [rsp.header("name")](http://jooby.org/apidocs/org/jooby/Response.html#header-java.lang.String-). The method always returns a [Mutant](http://jooby.org/apidocs/org/jooby/Mutant.html) and from there you can convert to any of the supported types.

Setting a header is pretty straight forward too:

   rsp.header("Header-Name", value).header("Header2", value);

### locals
Locals variables are bound to the current request. They are created every time a new request is processed and destroyed at the end of the request.

    rsp.locals("var", var);
    String var = rsp.local("var");



# working with data

## body.parser

A [Body.Parser](http://jooby.org/apidocs/org/jooby/Body.Parser.html) is responsible for parsing the HTTP body to something else.

A [Body.Parser](http://jooby.org/apidocs/org/jooby/Body.Parser.html) has three(3) methods:

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

Let's said we need to implement a JSON body parser (in real life you wont ever implement a json parser, this is just to demonstrate how they work):

```java
public class Json implements Body.Parser {

  public List<MediaType> types() {
    return ImmutableList.of(MediaType.json);
  }

  public boolean canParse(TypeLiteral<?> type) {
    return true; 
  }

  public <T> T parse(TypeLiteral<?> type, Body.Reader reader) throws Exception {
    ... parse it!
  }
}
```

Using it:

```java
{
  use(new Json()); // now Jooby has a json parser

  post("/", (req, rsp) -> {
    MyObject obj = req.body(MyObject.class);
    rsp.send(obj.getFirstName());
  });
}
```

**How it works**?

A route by default consumes ```*/*``` (any media type). Jooby will find/choose the **parser** who best matches the ```Content-Type``` header.

The ```Content-Type``` header is compared against the [parser.types()](http://jooby.org/apidocs/org/jooby/Body.Parser.html#types--) method.

Once an acceptable media type is found it call the **canParse** method of the [parser](http://jooby.org/apidocs/org/jooby/Body.Parser.html).

### consumes

The **consumes** method control what a route can consume or parse explicitly.

```java
{
  post("/", (req, rsp) -> {
    MyObject obj = req.body(MyObject.class);
  })
   .consumes("application/json");
}
```

**200** response:

    curl -X POST -H 'Content-Type: application/json' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/

**415** response bc **application/xml** isn't supported:

    curl -X POST -H 'Content-Type: application/xml' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/


In general, you hardly will use **consumes** in your routes. It is been created to give you more control on your routes and (more or less) explicitly document what is acceptable for your route. In real life, you won't use it too much but it will depends on your app requirements. For example if you need more than **json** for your routes (xml, yaml, etc..).

Another small advantage of using **consumes** is that the ```415``` response can be detected early (at the time a route is resolved) and not later or lazy (at the time you ask for type conversion).

Keep in mind, you still need a **parser** for your media types. For example:

```java
{
  post("/", (req, rsp) -> {
    MyObject obj = req.body(MyObject.class);
  })
   .consumes("application/json", "application/xml");
}
```

Require two parsers one for **json** and one for **xml**.

## body.formatter

A [Body.Formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter.html) is responsible for format a Java Object to a series of bytes in order to send them as HTTP response.

A [Body.Formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter.html) has three(3) methods:

* **types**: list of [media types](http://jooby.org/apidocs/org/jooby/MediaType.html) supported by the body formatter.
* **canFormat**(*type)*: test if the Java type is supported by the body formatter.
* **formatter***(data, writer)*: do the actual formatting using the data and writer.

In the next example we will try to send **MyObject** as HTTP response.

```java
get("/", (req, rsp) -> {
   MyObject obj = ...
   rsp.send(obj);
});
```

A call like:

    curl http://localhost:8080/

Give us a ```text/html``` and body content is ```obj.toString()```

    curl -H 'Accept: application/json' http://localhost:8080/

Results in ```406 - Not Acceptable```. That is because Jooby has no idea how to format ```application/json```. For that, we need a **json** formatter.

Let's said we need to implement a JSON body formatter (in real life you wont ever implement a json formatter, this is just to demonstrate how they work):

```java
public class Json implements Body.Formatter {

  public List<MediaType> types() {
    return ImmutableList.of(MediaType.json);
  }

  public boolean canFormat(TypeLiteral<?> type) {
    return true; 
  }

  public void format(Object data, Body.Writer writer) throws Exception {
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

The ```Accept``` header is compared against the [formatter.types()](http://jooby.org/apidocs/org/jooby/Body.Formatter.html#types--) method.

Once an acceptable media type is found it call the **canFormat** method of the [formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter.html).

### produces

The **produces** method control what a route can accept or format explicitly.

```java
{
  post("/", (req, rsp) -> {
    MyObject obj = ...
    rsp.send(obj);
  })
   .produces("application/json");
}
```

**200** response:

    curl -H 'Accept: application/json' http://localhost:8080/

**406** response bc **application/xml** isn't supported:

    curl 'Accept: application/xml' http://localhost:8080/

In general, you hardly will use **produces** in your routes. It is been created to give you more control on your routes and (more or less) explicitly document what is acceptable for your route. In real life, you won't use it too much but it will depends on your app requirements.

Another small advantage of using **produces** is that the ```406``` response can be detected early (at the time a route is resolved) and not lazily (at the time you ask for type conversion).

Keep in mind, you still need a **formatter** for your media types. For example:

```java
{
  post("/", (req, rsp) -> {
    MyObject obj = ...
    rsp.send(obj);
  })
   .produces("application/json", "application/xml");
}
```

Require two formatters one for **json** and one for **xml**.

## view engine

A [view engine](http://jooby.org/apidocs/org/jooby/View.Engine.html) is a specialized [body formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter.html) that ONLY accept instances of a [view](http://jooby.org/apidocs/org/jooby/View.html).

```java
{
  use(new MyTemplateEngine());

  get("/", (req, rsp) -> rsp.send(View.of("viewname", model));

}
```

There is no much to say about views & engines, any other detail or documentation should be provided in the specific module (mustache, handlebars, freemarker, etc.).

## response.format

As you learn before, content negotiation is done and executed every time a request is processed. Sometimes this isn't enough and that's why [rsp.format](http://jooby.org/apidocs/org/jooby/Response.html#format--) exists:

```java
get("/", (req, rsp)  ->
  rsp.format()
    .when("text/html", ()  -> View.of("viewname", model))
    .when("application/json", ()  -> model)
    .when("*", ()  -> Status.NOT_ACCEPTABLE)
    .send()
);
```

Performs content-negotiation on the Accept HTTP header of the request object. It select a handler for the request, based on the acceptable types ordered by their quality values. If the header is not specified, the first callback is invoked. When no match is found, the server responds with ```406 Not Acceptable```, or invokes the default callback: ```**/*```.



# web sockets

The use of web sockets is pretty easy too:

```java
{
   ws("/", (ws) -> {
     ws.onMessage(message -> System.out.println(message.stringValue()));
     
     ws.send("connected");
   });
}
```

A [web socket](http://jooby.org/apidocs/org/jooby/WebSocket.html) consist of a **path pattern** and a [handler](http://jooby.org/apidocs/org/jooby/WebSocket.Handler.html).

A **path pattern** can be as simple or complex as you need. All the path patterns supported by routes are supported here.

A [handler](http://jooby.org/apidocs/org/jooby/WebSocket.Handler.html) is executed on new connections, from there we can listen for message, errors and/or send data to the client.

Keep in mind that **web socket** are not like routes. There is no stack/pipe, chain or request modules.

You can mount a socket to a path used by a route, but you can't have two or more web sockets under the same path.

## guice access

You can ask [Guice](https://github.com/google/guice) to wired an object from the [ws.getInstance(type)](http://jooby.org/apidocs/org/jooby/WebSocket.html#getInstance-com.google.inject.Key-)

```java
ws("/", (ws) -> {
  A a = ws.getInstance(A.class);
});
```

But remember, there isn't a child injector and/or request objects.

## consumes

Web socket can defined a type to consumes: 

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

Web socket can defined a type to produces: 

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



# mvc routes

TODO


# available modules

## body parser & formatter

### [jackson json](https://github.com/jooby-project/jooby/tree/master/jooby-jackson)

## view engine

### [handlebars](https://github.com/jooby-project/jooby/tree/master/jooby-hbs)

## persistence

### [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc)
### [hibernate](https://github.com/jooby-project/jooby/tree/master/jooby-hbm)


# faq


# want to contribute?

* Fork the project on Github.
* Wondering what to work on? See task/bug list and pick up something you would like to work on.
* Write unit tests.
* Create an issue or fix one from [issues](https://github.com/jooby-project/jooby/issues).
* If you know the answer to a question posted to our [group](https://groups.google.com/forum/#!forum/jooby-project) - don't hesitate to write a reply.
* Share your ideas or ask questions on [group](https://github.com/jooby-project/jooby/issues) - don't hesitate to write a reply - that helps us improve javadocs/FAQ.
* If you miss a particular feature - browse or ask on the [group](https://groups.google.com/forum/#!forum/jooby-project) - don't hesitate to write a reply, show us some sample code and describe the problem.
* Write a blog post about how you use or extend [jooby][http://jooby.org].
* Please suggest changes to javadoc/exception messages when you find something unclear.
* If you have problems with documentation, find it non intuitive or hard to follow - let us know about it, we'll try to make it better according to your suggestions. Any constructive critique is greatly appreciated. Don't forget that this is an open source project developed and documented in spare time.

# help and support

* [jooby.org](http://jooby.org)
* [google group](https://groups.google.com/forum/#!forum/jooby-project)
* [issues](https://github.com/jooby-project/jooby/issues)

# related projects

 * [Jetty](https://www.eclipse.org/jetty/)
 * [Guice](https://github.com/google/guice)
 * [Type Safe](https://github.com/typesafehub/config)
 * [Logback](http://logback.qos.ch)

# author
 [Edgar Espina] (https://twitter.com/edgarespina)

# license
[Apache License 2](http://www.apache.org/licenses/LICENSE-2.0.html)
