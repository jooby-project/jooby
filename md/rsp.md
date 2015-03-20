## response

The response object contains methods for reading and setting headers, status code and body (between others). In the next section we will mention the most important method of a request object, if you need more information please refer to the [javadoc]({{apidocs}}/org/jooby/Response.html).

### sending data

The [rsp.send](http://jooby.org/apidocs/org/jooby/Response.html#send-org.jooby.Body-) method is responsible for sending and writing a body into the HTTP Response.

A [body formatter](http://jooby.org/apidocs/org/jooby/BodyFormatter) is responsible for converting a Java Object into something else (json, html, etc..).

Let's see a simple example:

```java
get("/", req -> rsp.send(data));
```

The **send** method select the best [body formatter](http://jooby.org/apidocs/org/jooby/BodyFormatter) to use base on the ```Accept``` header and if the current data type is supported.

The resulting ```Content-Type``` when is not set is the first returned by the  [formatter.types()](http://jooby.org/apidocs/org/jooby/BodyFormatter#types) method.

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

### response headers

Retrieval of response headers is done via [rsp.header("name")](http://jooby.org/apidocs/org/jooby/Response.html#header-java.lang.String-). The method always returns a [Mutant](http://jooby.org/apidocs/org/jooby/Mutant.html) and from there you can convert to any of the supported types.

Setting a header is pretty straightforward too:

   rsp.header("Header-Name", value).header("Header2", value);
