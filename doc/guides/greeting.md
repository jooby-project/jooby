{{#unless website}}[![Build Status](https://travis-ci.org/jooby-guides/{{guide}}.svg?branch=master)](https://travis-ci.org/jooby-guides/{{guide}}){{/unless}}
# greeting

You will learn how to build a simple **JSON API** with {{javadoc "Jooby"}}.

The service will be available at:

```
http://localhost:8080/greeting
```

and respond with the `JSON` response:

```json
{
  "id": 1,
  "name": "Hello World!"
}
```

{{jooby}} offers two programming models:

* **script**, where routes are written via **DSL** and lambdas.
* **mvc**, where routes are written as class method and annotations.

In this guide you will learn how to write a simple **JSON API** using both models.

# requirements

Make sure you have the following installed on your computer:

* A text editor or IDE
* {{java}} or later
* {{maven}}

# ready

Open a terminal/console and paste:

```bash
mvn archetype:generate -B -DgroupId={{pkgguide}} -DartifactId={{guide}} -Dversion=1.0 -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby -DarchetypeVersion={{version}}
```

A simple `hello world` application is now ready to run. Try:

```
cd {{guide}}

mvn jooby:run
```

Open a browser and type:

```
http://localhost:8080
```

> **TIP**: If you make a change, `jooby:run` will automatically restart and reload your application. More at {{link "/doc/devtools" "development tools"}}.

# quick preview

Before moving forward, let's have a look at `App.java`:

```java
package {{guide}};

import org.jooby.Jooby;

public class App extends Jooby { // 1 extends Jooby

  {
    // 2 define some routes
    get("/", () -> "Hello World!");
  }

  public static void main(final String[] args) {
    // 3 run this app
    run(App::new, args);
  }

}
```

That's all you need to get a simple **Hello World** {{javadoc "Jooby"}} application up and running.

# script route

Now that we've seen what a {{javadoc "Jooby"}} application looks like, we are going to create a simple greeting **JSON API**:

First `Greeting.java`:

```java
{{source "Greeting.java"}}
```

## create a route

Go to `App.java` and add this line:

```java
{
  get("/greeting", () -> new Greeting("Hello World!"));
}
```

Try it:

```
http://localhost:8080/greeting
```

You'll see `Hello World!` in your browser, not bad huh?

Not bad at all! But if you look closely we send a `text/html` response not an `application/json` response.

Before building **JSON** response let's see how to read an HTTP parameter.

## adding a name parameter

We are going to improve our service by allowing a name parameter:

```java
...
{
  ...
  get("/greeting", req -> {
    String name = "Hello " + req.param("name").value() + "!";

    return new Greeting(name);
  });
}
```

HTTP parameters are accessible via the {{javadoc "Request" "param" "java.lang.String"}} method, that is why we change our route a bit to access the {{javadoc "Request"}} object.

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

    return new Greeting(name);
  });
}
```

Same as before, we ask for the HTTP parameter but this time we set a default value: ```World```.

The {{javadoc "Mutant" "value" "java.lang.String"}} is syntactical sugar for:

```
String name = req.param("name").toOptional().orElse("World");
```

Optional parameters are represented by `java.util.Optional`.

Try it with a parameter:

    http://localhost:8080/greeting?name=Jooby

Try it without a parameter:

    http://localhost:8080/greeting

## path parameter

If you want or prefer a ```path``` parameter, you can replace the path pattern with: ```/greeting/:name``` or allow both of them:

```java
...
{
  ...
  get("/greeting", "/greeting/:name", req -> {
    String name = "Hello " + req.param("name").value("World") + "!";

    return new Greeting(name);
  });
}
```

Try it with a path parameter:

```
http://localhost:8080/greeting/Jooby
```

Try it with a query parameter:

```
http://localhost:8080/greeting?name=Jooby
```

Nice huh?

# json

{{jooby}} is a micro-web framework, so in order to write a **JSON** response you will need one of the available {{link "/doc/parser-and-renderer" "json modules"}}.

In this example we'll use {{modlink "jackson"}} but the process is exactly the same if you choose any of the other modules.

## dependency

Let's add the {{modlink "jackson"}} dependency to the project:

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jackson</artifactId>
  <version>{{version}}</version>
</dependency>
```

If `jooby:run` is running, please restart it. We need to force a restart due to having added a new dependency.

## use

Let's {{javadoc "Jooby" "use" "org.jooby.Jooby.Module" label="use"}} the module in our `App.java`:

```java

import org.jooby.json.Jackson;
...

{
  use(new Jackson());

  ...

  get("/greeting", "/greeting/:name", req -> {
    String name = "Hello " + req.param("name").value("World") + "!";

    return new Greeting(name);
  });
}
```

Our service method didn't change at all! we just {{javadoc "Jooby" "use" "org.jooby.Jooby.Module" label="use"}} the {{modlink "jackson"}} module!

Try accessing

    http://localhost:8080/greeting

You will get a nice **JSON** response:

```json
{
  "id": 1,
  "name": "Hello World!"
}
```

# mvc route

As a learning **exercise** we will build the same service using the **MVC** programming model.

## create a route

Create a new `Greetings.java` class like:

```java
package greeting;

import org.jooby.mvc.Path;
import org.jooby.mvc.GET;

@Path("/mvc")
public class Greetings {

  @Path("/greeting")
  @GET
  public String greeting() {
    return "Hello World!";
  }
}
```

The **MVC** programming model is similar to {{spring}} and/or {{jersey}}, except an `MVC` route must be registered at application startup time.

## registering a mvc route

Go to `App.java` and add this line:

```java
{
  ...

  use(Greetings.class);
}
```

Jooby tries to keep `reflection`, `classpath scanning` and `annotations` to a minimum. That is one of the reasons why routes need to be explicitly registered.

The other reason is the **route order**, since routes are executed in the **order** they are defined.

Having said that, we do offer a service {{modlink "scanner"}} module that automatically registers `MVC` routes.

Try it:

    http://localhost:8080/mvc/greeting

## adding a name parameter

As with the script route, we are going to add a **required** `name` parameter:

```java
@Path("/greeting")
@GET
public String greeting(String name) {
  return "Hello " + name + "!";
}
```

Try it:

    http://localhost:8080/mvc/greeting?name=Jooby

To call the service without a name we need to make the `name` parameter an optional parameter:

```java
import java.util.Optional;
...

@Path("/greeting")
@GET
public String greeting(Optional<String> name) {
  return "Hello " + name.orElse("World") + "!";
}
```

Try it with a parameter:

    http://localhost:8080/mvc/greeting?name=Jooby

Try it without:

    http://localhost:8080/mvc/greeting

## path parameter

If you want or prefer a path parameter, you can replace the path pattern with: `/greeting/:name` or allow both of them:

```java
@Path({"/greeting", "/greeting/:name"})
@GET
public String greeting(Optional<String> name) {
  return "Hello " + name.orElse("World") + "!";
}
```

Try it with a path parameter:

    http://localhost:8080/mvc/greeting/Jooby

Try it with a query parameter:

    http://localhost:8080/mvc/greeting?name=Jooby

# conclusion

Your application might look something like this now:

```java
{{source mainclass}}
```

This is an unrealistic **JSON API**, but it helps to demonstrate how simple and easy is to build such an application in {{jooby}}. **Simplicity** is one of the {{jooby}} goals.

We also demonstrate the **script** and **mvc** programming models, you can pick one or mix both in a single application.

The **script** programming model is perfect for getting things done quickly and/or for small applications. It is also possible to use the **script** routes on larger applications, where you usually split routes in one or more applications and then you compose all those small application into one.

The **mvc** programming model is a bit more verbose but probably better for large scale applications.

A common pattern for **medium** scale applications is to write the **UI** routes (those which generate HTML) using the **script** programming model while writing the **Business/API** routes using the **MVC** programming model. 

In short, **script** or **mvc** is matter of taste and/or depends on your background.


That's all for now, if you like what you see here please follow us at [@joobyproject](https://twitter.com/joobyproject) and [Github](https://github.com/jooby-project/jooby)

{{> guides/guide.footer}}
