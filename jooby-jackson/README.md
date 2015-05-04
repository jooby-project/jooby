# jooby-jackson

JSON support from the excellent [Jackson](https://github.com/FasterXML/jackson) library.

This module provides a JSON body [parser](/apidocs/Body.Parser.html) and [formatter](/apidocs/Body.Formatter.html)
but also an [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.5.2/com/fasterxml/jackson/databind/ObjectMapper.html).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jackson</artifactId>
  <version>0.5.3</version>
</dependency>
```
## usage

```java
import org.jooby.jackson.Json;

{
  use(new Jackson());
 
  // sending
  get("/my-api", req -> new MyObject()); 

  // receiving a json body
  post("/my-api", req -> {
    MyObject obj = req.body(MyObject.class);
    return obj;
  });

  // receiving a json param from a multipart or form url encoded
  post("/my-api", req -> {
    MyObject obj = req.param("my-object").to(MyObject.class);
    return obj;
  });
}
```

### advanced configuration

If you need a special setting or configuration for your [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/ObjectMapper.html):

```java
{
  use(new Jackson().configure(mapper -> {
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

  use(new Jackson());

  use((mode, config, binder) -> {
    Multibinder.newSetBinder(binder, Module.class).addBinding().to(MyJacksonModuleWiredByGuice.class);
  });
}
```

This is useful when your *MyJacksonModuleWiredByGuice* module require some dependencies.

That's all folks! Enjoy it!!!
