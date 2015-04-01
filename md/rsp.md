# response

The response object contains methods for reading and setting headers, status code and body (between others). In the next section we will mention the most important method of a response object, if you need more information please refer to the [javadoc]({{apidocs}}/org/jooby/Response.html).

## sending data

The [rsp.send]({{defdocs}}/Response.html#send-org.jooby.Body-) method is responsible for sending and writing data into the HTTP Response.

A [body formatter]({{defdocs}}/BodyFormatter) is responsible for converting a Java Object into something else (json, html, etc..).

Let's see a simple example:

```java
get("/", (req, rsp) -> rsp.send("hey jooby"));

get("/", req -> "hey jooby"); // or just return a value and Jooby will call send for you.
```

The **send** method select the best [body formatter]({{defdocs}}/BodyFormatter) to use based on the ```Accept``` header and if the current data type is supported.

The resulting ```Content-Type``` when is not set is the first returned by the  [formatter.types()]({{defdocs}}/BodyFormatter#types) method.

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

## response headers

Retrieval of response headers is done via [rsp.header("name")]({{defdocs}}/Response.html#header-java.lang.String-). The method always returns a [Mutant]({{defdocs}}/Mutant.html) and from there you can convert to any of the supported types.

Setting a header is pretty straightforward too:

```java
rsp.header("Header-Name", value).header("Header2", value);
```
