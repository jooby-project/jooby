[![Build Status](https://travis-ci.org/jooby-guides/{{guide}}.svg?branch=master)](https://travis-ci.org/jooby-guides/{{guide}})

# hello restful

You will learn how to build a simple **JSON RESTFUL** web service with {{jooby}}.

In this guide we will write some routes using the **script programming model**, if you want to compare the same guide using the **mvc programming model** checkout the: [hello mvc restful guide]({{guides}}/hello-mvc-restful)

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

# ready

Open a terminal (console for Windows users) and paste:

```bash
mvn archetype:generate -B -DgroupId={{pkgguide}} -DartifactId={{guide}} -Dversion=1.0 -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby -DarchetypeVersion={{version}}
```

An almost empty application is ready to run, you can try now with:

```
cd {{guide}}

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
    get("/", () -> "Hello World!");
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

And the new route:

```java
import java.util.concurrent.atomic.AtomicInteger;
...

{
  AtomicInteger idgen = new AtomicInteger();

  get("/greeting", () -> new Greeting(idgen.incrementAndGet(), "Hello World!"));
}
```

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
{
  ...
  get("/greeting", req -> {
    String name = "Hello " + req.param("name").value() + "!";

    return new Greeting(idgen.incrementAndGet(), name);
  });
}
```

HTTP parameter are accessible via ```req.param(String)``` method, that is why we change a bit our route to access the ```HTTP request```.

Try it:

```
http://localhost:8080/greeting?name=Jooby
```

What if you call the service without a ```name```? You will get a ```Bad Request(400)``` response. Let's fix that with an ```Optional``` parameter:

```java
...
{
  ...
  get("/greeting", req -> {
    String name = "Hello " + req.param("name").value("World") + "!";

    return new Greeting(idgen.incrementAndGet(), name);
  });
}
```

Same as before, we ask for the HTTP parameter but this time we set a default value: ```World```. Optional parameters can be retrieve with:

```
Optional<String> name = req.param("name").toOptional();
```

or from helper methods, like: ```.value(String)```.

Try it again with or without a ```name``` parameter!!

## path parameter

If you want or prefer a ```path``` parameter, you can replace the path pattern with: ```/greeting/:name``` or allow both of them:

```java
...
{
  ...
  get("/greeting", "/greeting/:name", req -> {
    String name = "Hello " + req.param("name").value("World") + "!";

    return new Greeting(idgen.incrementAndGet(), name);
  });
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

  ...

  get("/greeting", "/greeting/:name", req -> {
    String name = "Hello " + req.param("name").value("World") + "!";

    return new Greeting(idgen.incrementAndGet(), name);
  });
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
