# yasson

JSON support via [yasson](https://github.com/eclipse-ee4j/yasson) library.

## exports

* [json-b](http://json-b.net/users-guide.html)
* [Parser](/apidocs/org/jooby/Parser.html)
* [Renderer](/apidocs/org/jooby/Renderer.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-yasson</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
import org.jooby.json.Yasson;

{
  use(new Yasson());
 
  // sending
  get("/my-api", req -> new MyObject()); 

  // receiving a json body
  post("/my-api", req -> {
    MyObject obj = req.body(MyObject.class);
    return obj;
  });

  // direct access to Jsonb
  get("/access", req -> {
    Jsonb jsonb = require(Jsonb.class);
    // ...
  });
}
```

### configuration

If you need a special setting or configuration for your [json-b](http://json-b.net/users-guide.html):

```java
{
  use(new Yasson().doWith(builder -> {
    builder.withFormatting(true);
    // ...
  });
}
```
