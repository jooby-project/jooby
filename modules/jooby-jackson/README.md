[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-jackson/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-jackson/1.6.6)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jackson.svg)](https://javadoc.io/doc/org.jooby/jooby-jackson/1.6.6)
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
  <version>1.6.6</version>
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

## views

Dynamic views are supported via `org.jooby.json.JacksonView` object:

```java
{
  use(new Jackson());
  
  get("/public", req -> {
    Item item = ...
    return new JacksonView<>(Views.Public.class, item);
  });
  
  get("/internal", req -> {
    Item item = ...
    return new JacksonView<>(Views.Internal.class, item);
  });
}
```

## advanced configuration

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
