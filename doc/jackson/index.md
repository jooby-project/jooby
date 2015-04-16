---
layout: index
title: jackson
version: 0.5.0
---

# jooby-jackson

JSON support from the excelent [Jackson](https://github.com/FasterXML/jackson) library.

This module provides a JSON body [parser](/apidocs/Body.Parser.html) and [formatter](/apidocs/Body.Formatter.html).

Exposes [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.5.2/com/fasterxml/jackson/databind/ObjectMapper.html) services.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jackson</artifactId>
  <version>0.5.0</version>
</dependency>
```
## usage

```java
import org.jooby.jackson.Json;

{
  use(new Json());
 
  // sending
  get("/my-api", (req, rsp) -> rsp.send(new MyObject())); 

  // receiving a json body
  post("/my-api", (req, rsp) -> {
    MyObject obj = req.body(MyObject.class);
    rsp.send(obj);
  });

  // receiving a json param from a multipart or form url encoded
  post("/my-api", (req, rsp) -> {
    MyObject obj = req.param("my-object").to(MyObject.class);
    rsp.send(obj);
  });
}
```

## direct access

```java
// Injecting
public class Service {

   @Inject
   public Service(ObjectMapper mapper) {
     ...
   }
}

// or ask for it
{
  get("/", (req, rsp) -> ObjectMapper mapper = req.require(ObjectMapper.class));
}
```

### advanced configuration

If you need a special setting or configuration for your [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/ObjectMapper.html) you have two alternatives:

```java
{
  use(new Json().configure(mapper -> {
    // setup your custom object mapper
  });
}
```

or providing an [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/ObjectMapper.html) instance:

```java
{
   ObjectMapper mapper = ....;
   use(new Json(mapper));
}
```

It is possible to wire Jackson modules too:

```java
{

  use(new Json());

  use((mode, config, binder) -> {
    Multibinder.newSetBinder(binder, Module.class).addBinding().to(MyJacksonModuleWiredByGuice.class);
  });
}
```

This is useful when your *MyJacksonModuleWiredByGuice* module require some dependencies.

That's all folks! Enjoy it!!!



