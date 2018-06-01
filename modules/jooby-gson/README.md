[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-gson/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-gson)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-gson.svg)](https://javadoc.io/doc/org.jooby/jooby-gson/1.4.0)
[![jooby-gson website](https://img.shields.io/badge/jooby-gson-brightgreen.svg)](http://jooby.org/doc/gson)
# gson

JSON support via [Gson](https://github.com/google/gson) library.

## exports

* [Gson](https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/Gson.html)
* [Parser](/apidocs/org/jooby/Parser.html)
* [Renderer](/apidocs/org/jooby/Renderer.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-gson</artifactId>
  <version>1.4.0</version>
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
