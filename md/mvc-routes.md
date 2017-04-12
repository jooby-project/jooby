## mvc routes

Mvc routes are similar to **controllers** in [Spring](http://spring.io) and **resources** in [Jersey](https://jersey.java.net/) with some minor enhancements and simplifications.

```java
@Path("/")
public class MyRoutes {

  @GET
  public Result home() {
    return Results.html("home").put("model", model);
  }
}
```

Annotations are identical to [Jersey/JAX-RS](https://jersey.java.net/) and they can be found under the package `org.jooby.mvc`.

> **NOTE**: Jooby doesn't implement the **JAX-RS** specification. That is why it has its own version of the annotations.

An mvc route can be injected by {{guice}}:

```java
@Path("/")
public class MyRoutes {

  @Inject
  public MyRoutes(DepA a, DepB) {
   ...
  }

  @GET
  public Result home() {
    return Results.html("home").put("model", model);
  }

  @GET
  @Path("/search")
  public List<SearchResult> search() {
    List<SearchResult> result = ...;
    return result;
  }

  @POST
  @Path("/form")
  public MyObject submit(MyObject form) {
    ...
    return Results.html("success");
  }
}
```

> **NOTE**: MVC routes **are NOT singleton**, unless you explicitly annotated the route as a Singleton:

```java

import javax.inject.Singleton;

@Singleton
@Path("/")
public class MyRoutes {

  @Inject
  public MyRoutes(DepA a, DepB) {
   ...
  }

  @GET
  public Result home() {
    return Results.html("home").put("model", model);
  }

  @GET
  @Path("/search")
  public List<SearchResult> search() {
    List<SearchResult> result = ...;
    return result;
  }

  @POST
  @Path("/form")
  public MyObject submit(MyObject form) {
    ...
    return Results.html("success");
  }
}
```

### registering an mvc route

Mvc routes must be registered, there is **no auto-discover** feature, no classpath scanning, ..., etc.

The order in which you define your routes is very important and it defines how your app will work.

This is one of the reason why mvc routes need to be explicitly registered.

The other reason is that declaring the route explicitly helps to reduce bootstrap time.


So, how do I register an mvc route?

In the same way everything else is registered in {{jooby}}, from your application class:

```java
public class App extends Jooby {
  {
     use(MyRoutes.class);
  }
}
```

Again, handlers are registered in the order they are declared, so:

```java
@Path("/routes")
public class MyRoutes {
  
  @GET
  public void first() {
     log.info("first");
  }

  @GET
  public void second() {
     log.info("second");
  }

  @GET
  public String third() {
     return "third";
  }
}
```

A call to ```/routes``` will print: **first**, **second** and produces a response of **third**.

If you find the **explicit registration** odd or have too many `MVC routes`, checkout the [classpath scanner](/doc/scanner) module which automatically find and register `MVC routes`.

### request parameters

A method parameter represents a HTTP parameter:

```java
   @GET
   public List<Object> search(String q) {
    return searcher.doSearch(q);
   }
```

Here **q** can be any of the available parameter types and it will resolved as described in the [request parameters](#request-parameters) section.

Optional parameters work in the same way, all you have to do is to declare them as ```java.util.Optional```:

```java
   @GET
   public List<Object> search(Optional<String> q) {
    return searcher.doSearch(q.orElse("*:*"));
   }
```

Same for `multi-value` parameters, just declare them as ```java.util.List```, ```java.util.Set``` or ```java.util.SortedSet```:

```java
   @GET
   public List<Object> search(List<String> q) {
    return searcher.doSearch(q);
   }
```

> NOTE: The injected collection is immutable.

Same for {{file_upload}}

```java
   @POST
   public Object formPost(Upload file) {
    ...
   }
```

Jooby uses the method parameter name and binds that name to a request parameter. If you want an explicit mapping or if the request parameter isn't a valid Java identifier:

```java
   @GET
   public List<Object> search(@Named("req-param") String reqParam) {
    ...
   }
```

### form submit

A form submitted as {{formurlencoded}} or {{formmultipart}} doesn't require anything:

```java
  @POST
  public Result create(MyObject form) {
    ...
  }
```

### request body

Annotate the method parameter with the [@Body](/apidocs/org/jooby/mvc/Body.html) annotation:

```java
  @POST
  public MyObject create(@Body MyObject object) {
    ... do something with my object
  }
```


### request headers

Annotate the method parameter with the [@Header]({{defdocs}}/mvc/Header.html) annotation:

```java
   @GET
   public List<Object> search(@Header String myHeader) {
    ...
   }
```

Or, if the header name isn't a valid Java identifier:

```java
   @GET
   public List<Object> search(@Header("Last-Modified-Since") long lastModifedSince) {
    ...
   }
```

### response

A methods return type is sent to the client. Some examples:

```java

@GET
public String sayHi(String name) {
  // OK(200)
  return "Hi " + name;
}

@GET
public Result dontSayGoodbye(String name) {
  // NO_CONTENT(204)
  return Results.noContent();
}

```

If you want to render a view, just return a [view]({{defdocs}}/View.html) instance:

```java
@GET
public Result home() {
  return Results.html("home").put("model", model);
}
```

If you need to deal with HTTP metadata like: status code, headers, etc... use a [result] as the return type:

```java
@GET
public Result handler() {
  // 201 = created
  return Results.with(model, 201);
}
```
