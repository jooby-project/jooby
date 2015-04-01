## mvc routes

Mvc routes are like **controllers** in [Spring](http://spring.io) and/or **resources** in [Jersey](https://jersey.java.net/) with some minor enhancements and/or simplifications.

```java
@Path("/routes")
public class MyRoutes {

  @GET
  public View home() {
    return View.of("home", "model", model);
  }
}
```

Annotations are identical to [Jersey/JAX-RS](https://jersey.java.net/) and they can be found under the package **org.jooby.mvc**.

Keep in mind, Jooby doesn't implement the **JAX-RS** spec that is why it has his own version of the annotations.

A mvc route can be injected by Guice:

```java
@Path("/routes")
public class MyRoutes {

  @Inject
  public MyRoutes(DepA a, DepB) {
   ...
  }

  @GET
  public View home() {
    return View.of("home", "model", model);
  }
}
```

A method annotated with [GET]({{defdocs}}/mvc/GET.html), [POST]({{defdocs}}/mvc/POST.html),... (or any of the rest of the verbs) is considered a route handler (web method).

### registering a mvc route

Mvc routes must be registered, there is no auto-discover feature (and it won't be), classpath scanning, ..., etc.

We learnt that the order in which you define your routes has a huge importance and it defines how your app will work. This is one of the reason why mvc routes need to be explicitly registered. The other reason is bootstrap time, declaring the route explicitly helps to reduce bootstrap time.

So, how do I register a mvc route?

In the same way everything else is registered in Jooby!! from your app class:

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


### binding req params

A mvc handler can be bound to current request parameters:

```java
   @GET
   public List<Object> search(String q) {
    return searcher.doSearch(q);
   }
```

Here **q** can be any of the available param types and it will resolved as described in the [param types and precedence](#param-types-and-precedence) section.

Optional params work in the same way, all you have to do is to declare the param as *java.util.Optional*:

```java
   @GET
   public List<Object> search(Optional<String> q) {
    return searcher.doSearch(q.orElse("*:*"));
   }
```

Multi-value params work in the same way, all you have to do is to declare the param as *java.util.List*, *java.util.Set* or *java.util.SortedSet*:

```java
   @GET
   public List<Object> search(List<String> q) {
    return searcher.doSearch(q);
   }
```

Just remember the injected collection is immutable.

File uploads (again) work in the same way, just use *org.jooby.Upload*

```java
   @POST
   public View search(Upload file) {
    ... do something with the uploaded file
   }
```

As you might already noticed, Jooby uses the method param name and binded it to the request param. If you want explicit mapping and/or the req param isn't a valid Java identifier:

```java
   @GET
   public List<Object> search(@Named("req-param") String reqParam) {
    ...
   }
```

### binding req body

Injecting a req body work in the same way:

```java
  @POST
  public View search(MyObject object) {
  ... do something with the uploaded file
  }
```

### binding req headers

Works just like [req params](#binding-req-params) but you must annotated the param with *org.jooby.mvc.Header*:

```java
   @GET
   public List<Object> search(@Header String myHeader) {
    ...
   }
```

Or, if the header name isn't a valid Java identifier

```java
   @GET
   public List<Object> search(@Header("Last-Modified-Since") long lastModifedSince) {
    ...
   }
```

### mvc response

A web method might or might not send a response to the client. Some examples:

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

If you need/want to render a view, just return a *org.jooby.View* instance:

```java
@GET
public View home() {
  return View.of("home", "model", model);
}
```

#### customizing the response

If you need to deal with HTTP metadata like: status code, headers, etc... use a [org.jooby.Result]({{defdocs}}/Result.html)

```java
@GET
public Result handler() {
  // 201 = created
  return Results.with(model, 201);
}
```
