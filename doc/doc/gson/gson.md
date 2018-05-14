# gson

JSON support via [Gson](https://github.com/google/gson) library.

## exports

* [Gson](https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/Gson.html)
* [Parser]({{defdocs}}/Parser.html)
* [Renderer]({{defdocs}}/Renderer.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-gson</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
import org.jooby.json.Gzon;

{
  use(new Gzon());
 
  // sending
  get("/my-api", req -> new MyObject()); 

  // receiving a json body
  post("/my-api", req -> {
    MyObject obj = req.body(MyObject.class);
    return obj;
  });

  // direct access to Gson
  get("/access", req -> {
    Gson gson = require(Gson.class);
    // ...
  });
}
```

### configuration

If you need a special setting or configuration for your [Gson](https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/Gson.html):

```java
{
  use(new Gzon().doWith(builder -> {
    builder.setPrettyPrint();
    // ...
  });
}
```
