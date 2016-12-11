# routes

A route describes the interface for making requests to your application. It combines a HTTP **method** and a **path pattern**.

A route has an associated **handler**, which does some job and produces some kind of output (HTTP response).

Jooby offers two programming model for writing routes:

* Script routes via **lambdas**, like *Sinatra* and/or *expressjs*
* MVC routes via **annotations**, like *Jersey* and/or *Spring* (covered later)

## creating routes

A `script` route definition looks like:

```java
{
  get("/", () -> "hey jooby");
}
```

while a `MVC` route definition looks like:

```java

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

@Path("/")
public class Controller {

  @GET
  public String salute() {
    return "hey jooby";
  }
}
```

MVC routes are [covered later](/doc/#routes-mvc-routes) in details. For simplicity and easy to understand all the example uses the `script` routes, but keep in mind you can do the same in `MVC` routes.

We created a route to handle a GET request at the root of our application. Any other HTTP method can be created in the same way.

If you need a `POST`:

```java
post("/", () -> "hey jooby");
```

or need to listen to any `HTTP method`:

```java
use("*", "/", () -> "hey jooby");
```

It is possible to name a route explicitly:

```java
get("/", () -> "hey jooby")
   .name("salute");
```

Default route name is **anonymous**. Naming a route is useful for debugging purpose (if you have two or more routes mounted on the same path) and for [dynamic and advanced routing](#routes-dynamic-advanced-routing).

## route handler

Jooby offers several flavors for routes:

### functional handler: ()

```java
get("/", () -> "hey jooby");
```

This handler usually produces a constant value. Returned value will be send to the client.

### functional handler: req

```java
get("/", req -> "hey " + req.param("name").value());
```

This handler depends on some `external` attribute which is available via [Request]({{defdocs}}/Request.html) object. Returned value will be send to the client.

### handler: (req, rsp)

```java
get("/", (req, rsp) -> rsp.send("hey " + req.param("name").value());
```

This handler depends on some `external` attribute which is available via [Request]({{defdocs}}/Request.html) object and we explicitly send a response via [response.send]({{defdocs}}/Response.html#send(java.lang.Object)) method.

### filter: (req, rsp, chain)


```java
get("/", (req, rsp, chain) -> {
  // do something
  chain.next(req, rsp);
});
```

This is the most advanced handler, you have access to the [Request]({{defdocs}}/Request.html), [Response]({{defdocs}}/Response.html) and [Route.Chain]({{defdocs}}/Route.Chain.html) objects.

From here you can end a response by calling [response.send]({{defdocs}}/Response.html#send(java.lang.Object)) method, abort the request by throwing an [Err]({{defdocs}}/Err.html) or allow to proceed to the next handler in the pipeline by calling [chain.next(req, rsp)]({{defdocs}}/Route.Chain.html#next-org.jooby.Request-org.jooby.Response-).

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

// matches path element with prefix "uid"
get("/user/uid{id}", req -> "hey " + req.param("id").value());

// regex
get("/user/{id:\\d+}", req -> "hey " + req.param("id").intValue());
```

Request parameters are covered later, for now all you need to know is that you can access to a path parameter using the [Request.param(String)]({{apidocs}}/org/jooby/Request.html#param(java.lang.String)).

### ant style patterns

  ```com/t?st.html``` - matches ```com/test.html``` but also ```com/tast.html``` and ```com/txst.html```

  ```com/*.html``` - matches all ```.html``` files in the ```com``` directory

  ```com/**/test.html``` - matches all ```test.html``` files underneath the ```com``` path

  ```**``` - matches any path at any level

  ```*``` - matches any path at any level, shortcut for ```**```
  
  ```**:name``` or ```{name:**}``` - matches any path at any level and binds the match to the request parameter ```name```

## static files

Static files are located inside the ```public``` directory.

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

The [assets]({{defdocs}}/Jooby.html#assets-java.lang.String-) method let you expose all the content from a `folder`:

```java
{
  assets("/assets/**");
}
```

The asset route handler resolves requests like:

```
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

Here is another example with [webjars](http://www.webjars.org):

```java
{
  assets("/assets/**", "/META-INF/resources/webjars/{0}");
}
```

and responds to the following requests:

```
GET /assets/jquery/2.1.3/jquery.js
GET /assets/bootstrap/3.3.4/css/bootstrap.css
```

### file system location

By default the asset handler is able to read files from the `public` folder, which is a classpath folder.

It is possible to specify an `external` file system location too:

```
{
  assets("/static/**", Paths.get("/www"));
}
```

A request to `/static/images/logo.png` is translated to the `/www/images/logo.png` file.

### ETag, Last-Modified and Cache-Control

The `assets.etag` and `assets.lastModified` are two boolean properties that control the `ETag` and `Last-Modified` headers. Both are enabled by default.

The `assets.cache.maxAge` controls the `Cache-Control` header. Allowed value includes: `60`, `1h`, `365d`, etc. This property is off by default: `-1`.

### using a CDN

The asset handler goes one step forward and add support for serving files from a ```CDN``` out of the box.

All you have to do is to define a ```assets.cdn``` property:

```properties
assets.cdn = "http://d7471vfo50fqt.cloudfront.net"
```

```java
{
  assets("/assets/**");
}
```

A ```GET``` to ```/assets/js/index.js``` will be redirected to: ```http://d7471vfo50fqt.cloudfront.net/assets/js/index.js```


### assets module

There is also an awesome and powerful [assets](/doc/assets) module. The [assets](/doc/assets)
is library to validate, concatenate, minify or compress JavaScript and CSS assets.

## precedence and order

Routes are executed in the **order they are defined**. So the ordering of routes is crucial to the behavior of an application. Let's review this fact via some examples:

```java
get("/abc", req -> "first");

get("/abc", req -> "second");
```

A call to ```/abc``` produces a response of ```first```. If we revert the order:

```java
get("/abc", req -> "second");

get("/abc", req -> "first");
```

It produces a response of ```second```.

> As you can see **ORDER IS VERY IMPORTANT**.

Now, why is it allowed to have two routes on the same path?

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

Again the order of route definition is important. Forgetting this will cause your application behave unpredictably. We will learn more about this behavior in the examples of the next section.

## request handling

When a request is made to the server, which matches a route definition, the associated callback functions kick in to process the request and send back a response. We call this route pipe or stack.

Routes are like a plumbing pipe, requests start at the first route you define and work their way "down" the route stack processing for each path they match.

Each route handler has the capability to send a response or pass on the request to the next route handler in the current stack.

Route handlers, also have access to the chain object, which happens to be the next callback function in the pipe. To make the chain object available to the callback function, pass it along with the req and the rsp objects to it:

```java
get("/", (req, rsp, chain) -> {
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

Will the server print all of them? "first"? "third"?

It prints "first". The act of doing a {{rsp_send}} terminates the flow of the request then and there; the request is not passed on to any other route handler.

So, how do we specify multiple handlers for a route, and use them all at the same time? Call the {{chain_next}} function from the callback, without calling {{rsp_send}} because it terminates the request flow. Here is an example:

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

Alternative, if you **always** call {{chain_next}} just use the `(req, rsp` handler:

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

The 3rd arg is required if you need to decide if the next route need to be executed or not. If you always call {{chain_next}} the 3rd arg isn't required and does exactly what the 2arg handler does: **always** call {{chain_next}}.

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
