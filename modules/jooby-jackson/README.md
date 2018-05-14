[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jackson/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jackson)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jackson.svg)](https://javadoc.io/doc/org.jooby/jooby-jackson/1.3.0)
[![jooby-jackson website](https://img.shields.io/badge/jooby-jackson-brightgreen.svg)](http://jooby.org/doc/jackson)
# jackson

JSON support from the excellent [Jackson](https://github.com/FasterXML/jackson) library.

## exports

* ```ObjectMapper```
* [Parser](/apidocs/org/jooby/Parser.html)
* [Renderer](/apidocs/org/jooby/Renderer.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jackson</artifactId>
  <version>1.3.0</version>
</dependency>
```

## usage

```java
import org.jooby.json.Jackson;

{
  use(new Jackson());
 
  // sending
  get("/my-api", req -> new MyObject()); 

  // receiving a json body
  post("/my-api", req -> {
    MyObject obj = req.body(MyObject.class);
    return obj;
  });
}
```

### advanced configuration

If you need a special setting or configuration for your [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/ObjectMapper.html):

```java
{
  use(new Jackson().doWith(mapper -> {
    // setup your custom object mapper
  });
}
```

or provide an [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/ObjectMapper.html) instance:

```java
{
   ObjectMapper mapper = ....;
   use(new Jackson(mapper));
}
```

It is possible to wire Jackson modules too:

```java
{
  use(new Jackson()
    .module(MyJacksonModuleWiredByGuice.class)
  );
}
```

This is useful when your `MyJacksonModuleWiredByGuice` module require some dependencies.
