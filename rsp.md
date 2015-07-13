# response

The response object contains methods for reading and setting headers, status code and body (between others). In the next section we will mention the most important method of a response object, if you need more information please refer to the [javadoc](/apidocs
/org/jooby/Response.html).

## sending data

The [rsp.send](/apidocs/org/jooby
/Response.html#send-org.jooby.Result-) method is responsible for sending and writing data into the HTTP Response.

A [renderer](/apidocs/org/jooby
/Renderer.html) is responsible for converting a Java Object into something else (json, html, etc..).

Let's see a simple example:

```java
get("/", (req, rsp) -> rsp.send("hey jooby"));

get("/", req -> "hey jooby"); // or just return a value and Jooby will call send for you.
```

The **send** method will ask the [Renderer API](/apidocs/org/jooby
/Renderer.html) to format an object and write a response.

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

Retrieval of response headers is done via [rsp.header("name")](/apidocs/org/jooby
/Response.html#header-java.lang.String-). The method always returns a [Mutant](/apidocs/org/jooby
/Mutant.html) and from there you can convert to any of the supported types.

Setting a header is pretty straightforward too:

```java
rsp.header("Header-Name", value).header("Header2", value);
```
