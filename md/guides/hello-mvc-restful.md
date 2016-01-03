[![Build Status](https://travis-ci.org/jooby-guides/{{guide}}.svg?branch=final)](https://travis-ci.org/jooby-guides/{{guide}})

# hello mvc restful

You will learn how to build a simple **JSON RESTFUL** web service with {{jooby}}.

In this guide we will write some routes using the **MVC programming model**, if you want to compare the same guide using the **script programming model** checkout the: [hello restful guide]({{guides}}/hello-restful)

The service will be available at:

```
http://localhost:8080/greeting
```

and produces a **JSON** response:

```json
{
  "id": 1,
  "name": "Hello World!"
}
```

# requirements

Make sure you have all these software installed it in your computer:

* A text editor or IDE
* {{java}} or later
* {{maven}}
* {{git}}

# ready

Open a terminal (console for Windows users) and paste:

```bash
git clone {{gh-guides}}/{{guide}}.git

cd {{guide}}
```

An almost empty application is ready to run, you can try now with:

```
mvn jooby:run
```

Open a browser and type:

```
http://localhost:8080
```

> **TIP**: If you are using an IDE that automatically compiles your source code while you save it... ```mvn jooby:run``` will detects those changes and restart the application for you!! more at {{joobyrun}}.

# quick preview

Before moving forward let's have a look at ```src/main/java/hellorestful/App.java```:

```java

public class App extends Jooby { // 1 extends Jooby

  {
    // 2 define some routes
    get("/", () -> "Welcome to the {{guide}} guide!");
  }

  public static void main(final String[] args) throws Exception {
    // 3 start the app
    new App().start(args);
  }

}
```

Do you see the comments in the source code?

1. A **Jooby** app always extends ```org.jooby.Jooby```
2. We define some routes in the instance initializer (this is **NOT static code**)
3. A **Jooby** app need to be instantiated and then started. 

# getting dirty

Now we already see how a **Jooby** app looks like, we are going to create a simple greeting **RESTFUL** web service at ```http://localhost:8080/greeting```

The ```Greeting.java```:

```java
public class Greeting {

  public int id;

  public String name;

  public Greeting(final int id, final String name) {
    this.id = id;
    this.name = name;
  }

  public String toString() {
    return name;
  }
}
```

The ```Greetings.java```:

```java
import java.util.concurrent.atomic.AtomicInteger;
import org.jooby.mvc.Path;
import org.jooby.mvc.GET;

@Path("/greeting")
public class Greetings {

  static AtomicInteger idgen = new AtomicInteger();

  @GET
  public Greeting salute() {
    return new Greeting(idgen.incrementAndGet(), "Hello World!");
  }
}
```

A **MVC** route might looks familiar for {{spring}} or {{jersey}} developers.

They require a ```@Path``` annotation at class or method level, then for each public method annotated with ```GET```, ```POST```, etc.. **Jooby** creates a new route.

A key difference with {{spring}} is that a **MVC route** MUST be registered in ```App.java```:

```java
...

{
  use(Grettings.class);
}
```

You might be wonder why, right? Here are some reasons:

* routes in **Jooby** are executed in the order they are defined. If we allow classpath-scanning/autodiscovering it is impossible to known when the route should run.
* the order is so important that routes from a MVC class, are registered in the order they appears in the source code!
* removal of classpath-scanning makes **Jooby** to startup as fast as possible (key difference with other frameworks)

Open a browser and type:

```
http://localhost:8080/greeting
```

You'll see ```Hello World!``` in your browser, not bad ugh?

Not bad at all! But, don't we suppose to build a **JSON** **RESTFUL** web service?

Absolutely, but before that, let's see how to add a simple and optional HTTP parameter.

## adding a name parameter

We are going to improve our service by allowing a name parameter:

```java
  ...
  @GET
  public Greeting salute(String name) {
    return new Greeting(idgen.incrementAndGet(), "Hello " + name + "!");
  }
  ...
```

HTTP parameter are accessible via method parameter. Here a parameter named: ```name``` must be present in the HTTP request.

Try it:

```
http://localhost:8080/greeting?name=Jooby
```

What if you call the service without a ```name```? You will get a ```Bad Request(400)``` response. Let's fix that with an ```Optional``` parameter:

```java
import java.util.Optional;
  ...
  @GET
  public Greeting salute(Optional<String> name) {
    return new Greeting(idgen.incrementAndGet(), "Hello " + name.orElse("World") + "!");
  }
  ...
```

Same as before, except we declare the parameter as ```java.util.Optional```

Try it again with or without a ```name``` parameter!!

## path parameter

If you want or prefer a ```path``` parameter, you can replace the path pattern with: ```/greeting/:name``` or allow both of them:

```java
...
{
  ...
  @GET @Path({"/", "/:name"})
  public Greeting salute(Optional<String> name) {
    return new Greeting(idgen.incrementAndGet(), "Hello " + name.orElse("World") + "!");
  }
  ...
}
```

Try it:

```
http://localhost:8080/greeting?name=Jooby
```

Or:

```
http://localhost:8080/greeting/Jooby
```

Nice ugh? 

## json

As you already known, {{jooby}} is a micro-web framework in order to write a **JSON** response we need one of the available [json modules](/doc/parser-and-renderer).

Here we will use [jackson](https://github.com/jooby-project/jooby/tree/master/jooby-jackson) but keep in mind the process is exactly the same if you choose any other module.

First of add the [jackson](https://github.com/jooby-project/jooby/tree/master/jooby-jackson) dependency to your ```pom.xml```:

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jackson</artifactId>
</dependency>
```

If ```mvn jooby:run``` is running, please restart it (we need to force a restart bc we added a new dependency).

Let's ```use``` the module in our ```App.java:```

```java
import org.jooby.json.Jackson;
...

{
  use(new Jackson());

  use(Greetings.class);
}
```

Our service method didn't change at all! we just *import* the [jackson module](https://github.com/jooby-project/jooby/tree/master/jooby-jackson) using ```use(Module)``` method!!

Now, try the service again and you will get a nice **JSON** response:

```json
{
  "id": 1,
  "name": "Hello World!"
}
```

{{guides/guide.footer.md}}
