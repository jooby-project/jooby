# response

The response object contains methods for reading and setting headers, status code and body (amongst other things). In the next section you will encounter the most important methods of the response object. If you need more information please refer to the [javadoc]({{apidocs}}/org/jooby/Response.html).

## send

The [rsp.send]({{defdocs}}/Response.html#send-org.jooby.Result-) method is responsible for sending and writing data into the HTTP response.

A [renderer]({{defdocs}}/Renderer.html) is responsible for converting a Java Object into something else (json, html, etc..).

Let's consider a simple example:

```java
get("/", (req, rsp) -> rsp.send("hey jooby"));

get("/", req -> "hey jooby"); // or just return a value and Jooby will call send for you.
```

The **send** method will ask the [Renderer API]({{defdocs}}/Renderer.html) to format an object and write a response.

The resulting ```Content-Type``` when not set is ```text/html```.

The resulting ```Status Code``` when not set is ```200```.

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

## headers

Retrieval of response headers is done via [rsp.header("name")]({{defdocs}}/Response.html#header-java.lang.String-). This method returns a [Mutant]({{defdocs}}/Mutant.html) which can be converted to any of the supported types.

Setting a header is pretty straightforward as well:

```java
rsp.header("Header-Name", value).header("Header2", value);
```

## send file

The send file API is available through the [rsp.download*]({{defdocs}}/Response.html#download-java.lang.String-) methods:

```java
{
  get("/download", (req, rsp) -> {
    rsp.download("myfile", new File(...));
  });
}
```

The ```download``` method sets the following headers:

* ```Content-Disposition```

* ```Content-Length```

* ```Content-Type```

In the next example we explicitly set some of these:

```java
{
  get("/download", (req, rsp) -> {
    rsp
      .type("text/plain")
      .header("Content-Disposition", "attachment; filename=myfile.txt;")
      .download(new File(...));
  });
}
```
