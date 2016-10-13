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

// matches path element with prefix "uid"
get("/user/uid{id}", req -> "hey " + req.param("id").value());

// regex
get("/user/{id:\\d+}", req -> "hey " + req.param("id").intValue());
```

Request params are covered later, for now all you need to know is that you can access to a path parameter using the [Request.param(String)]({{apidocs}}/org/jooby/Request.html#param(java.lang.String)).

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

Let's add an asset handler:

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

Here is another example that uses [webjars](http://www.webjars.org):

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

### using a CDN

The asset handler goes one step forward and add support for serving files from a ```CDN``` out of the box.
All you have to do is to define a ```assets.cdn``` property:


```properties
# application.prod.properties

assets.cdn = "http://d7471vfo50fqt.cloudfront.net"
```

```java
{
  assets("/assets/**");
}
```

A ```GET``` to ```/assets/js/index.js``` will be redirected to: ```http://d7471vfo50fqt.cloudfront.net/assets/js/index.js```

Of course, you usually set a ```cdn``` in your ```application.prod.conf``` file only.

### assets module

There is also a super awesome and powerful [assets](/doc/assets/) module. The [assets](/doc/assets/)
is library to validate, concatenate, minify or compress JavaScript and CSS assets.

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

It produces a response of ```second```.

> As you can see **ORDER IS SUPER IMPORTANT**.

Now, why is it allowed to have two routes for the same path?

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

Again the order of route definition is super important. Forgetting this will cause your app behave unpredictably. We will learn more about this behavior in the examples of the next section.

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

