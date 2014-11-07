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
