# working with data

## body.parser

A [Body.Parser](http://jooby.org/apidocs/org/jooby/Body.Parser.html) is responsible for parsing the HTTP body to something else.

A [Body.Parser](http://jooby.org/apidocs/org/jooby/Body.Parser.html) has three(3) methods:

* **types**: list of [media types](http://jooby.org/apidocs/org/jooby/MediaType.html) supported by the body parser.
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

    curl -X POST -H 'Content-Type: application/json' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/

Results in ```415 - Unsupported Media Type```. That is because Jooby has no idea how to parse ```application/json```. For that, we need a **json** parser.

Let's said we need to implement a JSON body parser (in real life you wont ever implement a json parser, this is just to demonstrate how it works):

```java
public class Json implements Body.Parser {

  public List<MediaType> types() {
    return ImmutableList.of(MediaType.json);
  }

  public boolean canParse(TypeLiteral<?> type) {
    return true; 
  }

  public <T> T parse(TypeLiteral<?> type, Body.Reader reader) throws Exception {
    ... parse it!
  }
}
```

Using it:

```java
{
  use(new Json()); // now Jooby has a json parser

  post("/", (req, rsp) -> {
    MyObject obj = req.body(MyObject.class);
    rsp.send(obj.getFirstName());
  });
}
```

**How it works**?

A route by default consumes ```*/*``` (any media type). Jooby will find/choose the **parser** which best matches the ```Content-Type``` header.

The ```Content-Type``` header is compared against the [parser.types()](http://jooby.org/apidocs/org/jooby/Body.Parser.html#types--) method.

Once an acceptable media type is found it call the **canParse** method of the [parser](http://jooby.org/apidocs/org/jooby/Body.Parser.html).

### consumes

The **consumes** method control what a route can consume or parse explicitly.

```java
{
  post("/", (req, rsp) -> {
    MyObject obj = req.body(MyObject.class);
  })
   .consumes("application/json");
}
```

**200** response:

    curl -X POST -H 'Content-Type: application/json' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/

**415** response bc **application/xml** isn't supported:

    curl -X POST -H 'Content-Type: application/xml' -d '{"firstName":"Pato", "lastName":"Sol"}' http://localhost:8080/


In general, you hardly will use **consumes** in your routes. It has been created to give you more control on your routes and (more or less) explicitly document what is acceptable for your route. In real life, you won't use it too much but it will depend on your app requirements. For example if you need more than **json** for your routes (xml, yaml, etc..).

Another small advantage of using **consumes** is that the ```415``` response can be detected early (at the time a route is resolved) and not later or lazy (at the time you ask for type conversion).

Keep in mind, you still need a **parser** for your media types. For example:

```java
{
  post("/", (req, rsp) -> {
    MyObject obj = req.body(MyObject.class);
  })
   .consumes("application/json", "application/xml");
}
```

Require two parsers one for **json** and one for **xml**.

## body.formatter

A [Body.Formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter.html) is responsible for format a Java Object to a series of bytes in order to send them as HTTP response.

A [Body.Formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter.html) has three(3) methods:

* **types**: list of [media types](http://jooby.org/apidocs/org/jooby/MediaType.html) supported by the body formatter.
* **canFormat**(*type)*: test if the Java type is supported by the body formatter.
* **formatter***(data, writer)*: do the actual formatting using the data and writer.

In the next example we will try to send **MyObject** as HTTP response.

```java
get("/", (req, rsp) -> {
   MyObject obj = ...
   rsp.send(obj);
});
```

A call like:

    curl http://localhost:8080/

Give us a ```text/html``` and body content is ```obj.toString()```

    curl -H 'Accept: application/json' http://localhost:8080/

Results in ```406 - Not Acceptable```. That is because Jooby has no idea how to format ```application/json```. For that, we need a **json** formatter.

Let's said we need to implement a JSON body formatter (in real life you wont ever implement a json formatter, this is just to demonstrate how they work):

```java
public class Json implements Body.Formatter {

  public List<MediaType> types() {
    return ImmutableList.of(MediaType.json);
  }

  public boolean canFormat(TypeLiteral<?> type) {
    return true; 
  }

  public void format(Object data, Body.Writer writer) throws Exception {
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

The ```Accept``` header is compared against the [formatter.types()](http://jooby.org/apidocs/org/jooby/Body.Formatter.html#types--) method.

Once an acceptable media type is found it call the **canFormat** method of the [formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter.html).

### produces

The **produces** method control what a route can accept or format explicitly.

```java
{
  post("/", (req, rsp) -> {
    MyObject obj = ...
    rsp.send(obj);
  })
   .produces("application/json");
}
```

**200** response:

    curl -H 'Accept: application/json' http://localhost:8080/

**406** response bc **application/xml** isn't supported:

    curl 'Accept: application/xml' http://localhost:8080/

In general, you hardly will use **produces** in your routes. It has been created to give you more control on your routes and (more or less) explicitly document what is acceptable for your route. In real life, you won't use it too much but it will depend on your app requirements.

Another small advantage of using **produces** is that the ```406``` response can be detected early (at the time a route is resolved) and not lazily (at the time you ask for type conversion).

Keep in mind, you still need a **formatter** for your media types. For example:

```java
{
  post("/", (req, rsp) -> {
    MyObject obj = ...
    rsp.send(obj);
  })
   .produces("application/json", "application/xml");
}
```

Require two formatters one for **json** and one for **xml**.

## view engine

A [view engine](http://jooby.org/apidocs/org/jooby/View.Engine.html) is a specialized [body formatter](http://jooby.org/apidocs/org/jooby/Body.Formatter.html) that ONLY accept instances of a [view](http://jooby.org/apidocs/org/jooby/View.html).

```java
{
  use(new MyTemplateEngine());

  get("/", (req, rsp) -> rsp.send(View.of("viewname", model));

}
```

There is no much to say about views & engines, any other detail or documentation should be provided in the specific module (mustache, handlebars, freemarker, etc.).

## response.format

As you learnt before, content negotiation is done and executed every time a request is processed. Sometimes this isn't enough and that's why [rsp.format](http://jooby.org/apidocs/org/jooby/Response.html#format--) exists:

```java
get("/", (req, rsp)  ->
  rsp.format()
    .when("text/html", ()  -> View.of("viewname", model))
    .when("application/json", ()  -> model)
    .when("*", ()  -> Status.NOT_ACCEPTABLE)
    .send()
);
```

Performs content-negotiation on the Accept HTTP header of the request object. It select a handler for the request, based on the acceptable types ordered by their quality values. If the header is not specified, the first callback is invoked. When no match is found, the server responds with ```406 Not Acceptable```, or invokes the default callback: ```**/*```.

