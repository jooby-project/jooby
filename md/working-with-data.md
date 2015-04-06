# body parser, formatter and view engine

## body parser

A [BodyParser]({{defdocs}}/BodyParser.html) is responsible for parsing the HTTP body to something else.

A [BodyParser]({{defdocs}}/BodyParser.html) has three(3) methods:

* **types**: list of [media types]({{defdocs}}/MediaType.html) supported by the body parser.
* **canParse**(*type)*: test if the Java type is supported by the body parser.
* **parse***(type, reader)*: do the actual parsing using the type and reader.

In the next example we will try to read the HTTP Body as **MyObject**.

```java
post("/", (req, rsp) -> {
   MyObject obj = req.body(MyObject.class);
   ...
});
```

A call like:

```bash
curl -X POST -H 'Content-Type: application/json' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/
```

Results in ```415 - Unsupported Media Type```. That is because Jooby has no idea how to parse ```application/json```. For that, we need a **json** parser.

Let's said we need to implement a JSON body parser (in real life you wont ever implement a json parser, this is just to demonstrate how it works):

```java
public class Json implements BodyParser {

  public List<MediaType> types() {
    return ImmutableList.of(MediaType.json);
  }

  public boolean canParse(TypeLiteral<?> type) {
    return true; 
  }

  public <T> T parse(TypeLiteral<?> type, Context ctx) throws Exception {
    ... parse it!
  }
}
```

Using it:

```java
{
  use(new Json()); // now Jooby has a json parser

  post("/", req -> {
    MyObject obj = req.body(MyObject.class);
    return obj;
  });
}
```

**How it works**?

A route by default consumes ```*/*``` (any media type). Jooby will find/choose the **parser** which best matches the ```Content-Type``` header.

The ```Content-Type``` header is compared against the [parser.types()]({{defdocs}}/BodyParser.html#types--) method.

Once an acceptable media type is found it call the **canParse** method of the [parser]({{defdocs}}/BodyParser.html).

### consumes

The **consumes** method control what a route can consume or parse explicitly.

```java
{
  post("/", req -> {
    MyObject obj = req.body(MyObject.class);
  })
   .consumes("application/json");
}
```

**200** response:

```bash
curl -X POST -H 'Content-Type: application/json' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/
```

**415** response bc **application/xml** isn't supported:

```bash
curl -X POST -H 'Content-Type: application/xml' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/
```

In general, you hardly will use **consumes** in your routes. It has been created to give you more control on your routes and (more or less) explicitly document what is acceptable for your route. In real life, you won't use it too much but it will depend on your app requirements. For example if you need more than **json** for your routes (xml, yaml, etc..).

Another small advantage of using **consumes** is that the ```415``` response can be detected early (at the time a route is resolved) and not later or lazy (at the time you ask for type conversion).

Keep in mind, you still need a **parser** for your media types. For example:

```java
{
  post("/", req -> {
    MyObject obj = req.body(MyObject.class);
  })
   .consumes("application/json", "application/xml");
}
```

Require two parsers one for **json** and one for **xml**.

## body formatter

A [BodyFormatter]({{defdocs}}/BodyFormatter.html) is responsible for format a Java Object to a series of bytes in order to send them as HTTP response.

A [BodyFormatter]({{defdocs}}/BodyFormatter.html) has three(3) methods:

* **types**: list of [media types]({{defdocs}}/MediaType.html) supported by the body formatter.
* **canFormat**(*type)*: test if the Java type is supported by the body formatter.
* **formatter***(data, writer)*: do the actual formatting using the data and writer.

In the next example we will try to send **MyObject** as HTTP response.

```java
get("/", req -> {
   MyObject obj = ...
   return obj;
});
```

A call like:

```bash
curl http://localhost:8080/
```

Give us a ```text/html``` and body content is ```obj.toString()```

```bash
curl -H 'Accept: application/json' http://localhost:8080/
```

Results in ```406 - Not Acceptable```. That is because Jooby has no idea how to format ```application/json```. For that, we need a **json** formatter.

Let's said we need to implement a JSON body formatter (in real life you wont ever implement a json formatter, this is just to demonstrate how they work):

```java
public class Json implements BodyFormatter {

  public List<MediaType> types() {
    return ImmutableList.of(MediaType.json);
  }

  public boolean canFormat(TypeLiteral<?> type) {
    return true; 
  }

  public void format(Object data, Context ctx) throws Exception {
    ... format and write it!
  }
}
```

Using it:

```java
{
  use(new Json()); // now Jooby has a json formatter

  post("/", (req, rsp) -> {
     MyObject obj = ...
     rsp.send(obj);
  });
}
```

**How it works**?

A route by default produces ```*/*``` (any media type). Jooby will find/choose the **formatter** who best matches the ```Accept``` header.

The ```Accept``` header is compared against the [formatter.types()]({{defdocs}}/BodyFormatter.html#types--) method.

Once an acceptable media type is found it call the **canFormat** method of the [formatter]({{defdocs}}/BodyFormatter.html).

### produces

The **produces** method control what a route can accept or format explicitly.

```java
{
  post("/", req -> {
    MyObject obj = ...
    return obj;
  })
   .produces("application/json");
}
```

**200** response:

```bash
curl -H 'Accept: application/json' http://localhost:8080/
```

**406** response bc **application/xml** isn't supported:

```bash
curl 'Accept: application/xml' http://localhost:8080/
```

In general, you hardly will use **produces** in your routes. It has been created to give you more control on your routes and (more or less) explicitly document what is acceptable for your route. In real life, you won't use it too much but it will depend on your app requirements.

Another small advantage of using **produces** is that the ```406``` response can be detected early (at the time a route is resolved) and not lazily (at the time you ask for type conversion).

Keep in mind, you still need a **formatter** for your media types. For example:

```java
{
  post("/", req -> {
    MyObject obj = ...
    return obj;
  })
   .produces("application/json", "application/xml");
}
```

Require two formatters one for **json** and one for **xml**.

## view engine

A [view engine]({{defdocs}}/View.Engine.html) is a specialized [body formatter]({{defdocs}}/BodyFormatter.html) that ONLY accept instances of a [view]({{defdocs}}/View.html).

```java
{
  use(new MyTemplateEngine());

  get("/", req -> Results.html("viewname").put("model", model);

}
```

There is no much to say about views & engines, any other detail or documentation should be provided in the specific module (mustache, handlebars, freemarker, etc.).
