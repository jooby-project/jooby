## dynamic / advanced routing

Dynamic routing allows you to filter a pipeline execution chain and produces custom or completely different response.

For example, suppose you need to serve different content based on hostname:

```java
{
   use("*", (req, rsp, chain) -> {
     if (req.hostname().equals("foo.com")) {
       chain.next("/foo-branding", req, rsp);
     } else {
       chain.next("/bar-branding", req, rsp);
     }
   });

   get("/", () -> Results.html("foo")).name("foo-branding");

   get("/", () -> Results.html("bar")).name("bar-branding");
}

```

This application has two routes under root path: `/`. Each of these route has a `name`: `foo-branding` and `bar-branding`.

If you load the application from `http://foo.com` the `foo-branding` route will be executed, otherwise the `bar-branding` route.

Dynamic routing is done over [route.name()]({{defdocs}}/Route.html#name--). From {{req_filter}} you provide a `name filter` via [chain.next(name, req, rsp)](/apidocs/org/jooby/Route.Chain.html#next-java.lang.String-org.jooby.Request-org.jooby.Response-) method. 

You can group routes via [use(path)]({{defdocs}}/Jooby.html#use-java.lang.String-) method and globally set a name to all the child routes:

```java
{
  use("/")
    .get(() -> Results.html("foo"))
    .get("/api", () -> ...)
    // apply name to all routes
    .name("foo-branding");

  use("/")
    Results.html("bar")
    .get(() -> Results.html("bar"))
    .get("/api", () -> ...)
    // apply name to all routes
    .name("bar-branding");
}
```

Or group routes via [with(Runnable)]({{defdocs}}/Jooby.html#with-java.lang.Runnable-):

```java
{
  with(() -> 
    get("/", () -> Results.html("foo"));

    get("/api", () -> ...);

  ).name("foo-branding");

  with(() -> 
    get("/", () -> Results.html("bar"));

    get("/api", () -> ...);

  ).name("bar-branding");
}
```

Or group routes in their own application and then merge them into the main application:

```java
public class FooBranding extends Jooby {

  public FooBranding() {
    super("foo-branding");
  }

  {
    get("/", () -> Results.html("foo"));
    ...
  }
}

public class BarBranding extends Jooby {

  public BarBranding() {
    super("bar-branding");
  }

  {
    get("/", () -> Results.html("bar"));
    ...
  }
}

/** Merge everything .*/
public class App extends Jooby {
  {
     use("*", (req, rsp) -> {
       if (req.hostname().equals("foo.com")) {
         chain.next("/foo-branding", req, rsp);
       } else {
         chain.next("/bar-branding", req, rsp);
       }
     });

     use(new FooBranding());
     use(new BarBranding());
  }
}
```

Routes and routing in {{jooby}} are so powerful!
