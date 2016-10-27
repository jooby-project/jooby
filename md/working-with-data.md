# parser and renderer

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

Suppose we want to write a custom parser to convert a value into an ```integer```. In practice we don't need such parser bc it is provided, this is an example.

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

1) Check if current type is what we can parse to

2) We add a param callback

3) We can't deal with current type, so we ask next parser to resolve it

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

Parsers are executed in the order they are defined.

If a param parser isn't able to resolve a param a ```BAD REQUEST(400)``` error will be generated.

If a body parser isn't able to resolve a param an ```UNSUPPORTED_MEDIA_TYPE(415)``` error will be generated.

## renderer

A [Renderer]({{defdocs}}/Renderer.html) converts a Java Object to a series of bytes or text and write them into the HTTP response.

There are a few built-in renderers:

* stream: copy an inputstream to the HTTP response and set a default type of: ```application/octet-stream```
* bytes: copy bytes to the HTTP response and set a default type of: ```application/octet-stream```
* byteBuffer: copy bytes to the HTTP response and set a default type of: ```application/octet-stream```
* readable: copy a readable object to the HTTP response and a default type of: ```text/html```
* text: copy the toString() result to the HTTP response and set a default type of: ```text/html```

### custom renderer

Suppose we want to apply a custom rendering for ```MyObject```. Renderer is as simple as:

```java

render((value, ctx) -> {
  if (value instanceOf MyObject) {
     ctx.text(value.toString());
  }
});

get("/", req -> {
   return new MyObject();
});
```

A generic JSON renderer will looks like:

```java

render((value, ctx) -> {
  if (ctx.accepts("json")) {
     ctx
       .type("json")
       .text(toJson(value));
  }
});

get("/", req -> {
   return new MyObject();
});
```

Renderer API is simple and powerful. Renderers are executed in the order they were defined. The renderer who write the response first wins!

## view engine

A [view engine]({{defdocs}}/View.Engine.html) is a specialized [renderer]({{defdocs}}/Renderer.html) that ONLY accept instances of a [view]({{defdocs}}/View.html).

A [view]({{defdocs}}/View.html) carry the template name + model data:

```java
{
  use(new MyTemplateEngine());

  get("/", req -> Results.html("viewname").put("model", model);

}
```

In order to support multiples view engine, a view engine is allowed to throw a ```java.io.FileNotFoundException``` when a template can't be resolved it. This gives the chance to the next view resolver to load the template.

There is no much to say about views and template engines, any other detail or documentation should be provided in the specific [module](/doc/parser-and-renderer)
