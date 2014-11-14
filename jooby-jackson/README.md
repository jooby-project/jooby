# jooby-jackson

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jackson</artifactId>
  <version>0.1.0</version>
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

The [jackson](http://jackson.codehaus.org/) module provides a JSON body [parser]({{apidocs}}/Body.Parser.html) and [formatter]({{apidocs}}/Body.Formatter.html).

The underlying *com.fasterxml.jackson.databind.ObjectMapper* is bound too:

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
  get("/", (req, rsp) -> ObjectMapper mapper = req.getInstance(ObjectMapper.class));
}
```

### advanced configuration

If you need a special setting or configuration for your *ObjectMapper* you have two alternatives:

```java
{
  use(new Json().configure(mapper -> {
    // setup your custom object mapper
  });
}
```

or providing an *ObjectMapper* instance:

```java
{
   ObjectMapper mapper = ....;
   use(new Json(mapper));
}
```

It is possible to wire your *com.fasterxml.jackson.databind.Module* modules with Guice:

```java
{

  use(new Json());

  use((mode, config, binder) -> {
    Multibinder.newSetBinder(binder, Module.class).addBinding().to(MyJacksonModuleWiredByGuice.class);
  });
}
```

This is useful when your *MyJacksonModuleWiredByGuice* module require some dependencies.

Cool, isn't?
