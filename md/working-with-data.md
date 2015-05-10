# parsers, formatter and view engine

## parser

A [Parser]({{defdocs}}/Parser.html) is responsible for parsing the HTTP params and/or body to something else.

Automatic type conversion is provided when a type:

* Is a primitive, primitive wrapper or String
* Is an enum
* Is an [Upload]({{apidocs}}/org/jooby/Upload.html)
* Has a public **constructor** that accepts a single **String** argument
* Has a static method **valueOf** that accepts a single **String** argument
* Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```
* Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```
* It is an Optional<T>, List<T>, Set<T> or SortedSet<T> where T satisfies one of previous rules


### custom parser

Suppose we want to write a custom parser to convert a value into an ```integer``. In practice we don't need such parser bc it is provided, but of course you can override the default parser and provide your own.

Let's see how to create our custom HTTP param parser:

```java

parser((type, ctx) -> {
  // 1
  if (type.getRawType() == int.class) {
    // 2
    return ctx.param(values -> Integer.parseInt(values.get(0));
  }
  // 3
  return ctx.next();
});

get("/", req -> {
   int intValue = req.param("v").intValue();
   ...
});

```

Let's have a closer look:

1. Check if current type is what we can parse to
2. We add a param callback
3. We can't deal with current type, so we ask next parser to resolve it

Now, if we ask for HTTP body

```java
get("/", req -> {
   int intValue = req.body().intValue();
   ...
});

```

Our custom parser won't be able to parse the HTTP body, because it works on HTTP parameter. In order to extend our custom parser and use it for HTTP Body we must do:

```java

parser((type, ctx) -> {
  // 1
  if (type.getRawType() == int.class) {
    // 2
    return ctx.param(values -> Integer.parseInt(values.get(0))
       .body(body -> Integer.parseInt(body.text()));
  }
  // 3
  return ctx.next();
});

```

And now we can ask for a HTTP param and/or body.

```java
get("/", req -> {
   int intValue = req.param("v").intValue();
   ...
});

post("/", req -> {
   int intValue = req.body().intValue();
   ...
});
```

[Parser]({{defdocs}}/Parser.html) API is very powerful. It let you apply a parser to a HTTP param, set of param (like a form post), file uploads and/or body. But not just that, you are free to choose if your parser applies for a Java Type and/or a Media Type, like the ```Content-Type``` header.

For example a generic JSON parser looks like:

```java

parser((type, ctx) -> {
  if (ctx.type().name().equals("application/json")) {
    return ctx.body(body -> fromJSON(body.text()));
  }
  return ctx.next();
});
```

Parsers are executed in the order they are defined. Application provided parser has precedence over built-in parsers, so it it possible to override a built-in parser too!

If a param parser isn't able to resolve a param an exception will be thrown with a ```400``` status code.

If a body parser isn't able to resolve a param an exception will be thrown with a ```415``` status code.

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
