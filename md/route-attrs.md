## properties

Routes have a few properties that let you extend basic functionality in one way or another including:

* attributes
* with, map and excludes operators
* consumes/produces types

### attributes

Attributes let you annotate a route at application bootstrap time. It functions like static metadata available at runtime:

```java
{
  get("/path", ..)
    .attr("foo", "bar");
}
```

An attribute consist of a `name` and `value`. Allowed values are ```primitives```, ```String```, ```enum```, ```class``` or an ```array``` of these types.

Attributes can be accessed at runtime in a request/response cycle. For example, a security module might check for a ```role``` attribute, a sitemap generator might check for a ```priority``` attribute, etc.

```java
{
  use((req, rsp, chain) -> {
    User user = ...;
    String role = req.route().attr("role");
    if (user.hasRole(role)) {
      chain.next(req, rsp);
    }
    throw new Err(403);
  });
}
```

In MVC routes you can set attributes for all the web methods:

```java
{
   use(Controller.class)
     .attr("foo", "bar");
}
```


Or via ```annotations```:

```java
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public static @interface Role {
  String value();
}

@Path("/path")
public class AdminResource {

  @Role("admin")
  public Object doSomething() {
    ...
  }

}

{
  use("*", (req, rsp) -> {
    System.out.println(req.route().attributes())
  });
}

```

The previous example will print: ```{role = admin}```.

Any runtime annotation is automatically added as route attributes following these rules:

* If the annotation has a ```value``` method, then we use the annotation's name as the attribute name.
* Otherwise, we use the method name as the attribute name.

> **request attributes vs route attributes:**
>
> Route attributes are created at bootstrap. They are global, and once set, they won't change.
>
> On the other hand, request attributes are created in a request/response cycle.

### with operator

The {{route_with}} operator sets attributes, consumes/produces types, exclusions, etc. to one or more routes:

```java
{
  with(() -> {

    get("/admin/1", ...);

    get("/admin/2", ...);

  }).attr("role", "admin");
}
```

### map operator

The {{route_map}} operator converts a route output to something else:

```java
{
  // we got bar.. not foo
  get("/foo", () -> "foo")
    .map(value -> "bar");

  // we got foo.. not bar
  get("/bar", () -> "bar")
    .map(value -> "foo");
}
```

If you want to apply a single {{route_map}} to several routes:

```java
{
  with(() -> {
    get("/foo", () -> "foo");

    get("/bar", () -> "bar");

  }).map(v -> "foo or bar");
}
```

You can apply a [Mapper]({{defdocs}}/Route.Mapper.html) to specific types:

```java
{
  with(() -> {
    get("/str", () -> "str");

    get("/int", () -> 1);

  }).map(String v -> "{" + v + "}");
}
```

A call to ```/str``` produces ```{str}```, while ```/int``` just ```1```.

> **NOTE**: You can apply the map operator to routes that produces an output (a.k.a function routes).

For example, the {{route_map}} operator will be silently ignored here:

```java
{
  get("/", (req, rsp) -> {
    rsp.send(...);
  });
}
```

### excludes

The {{route_excludes}} operator ignores what would otherwise have been a route path match:

```java
{
   use("*", (req, rsp) -> {
     // all except /login
   }).excludes("/login");
}
```

### consumes

The {{route_consumes}} operator indicates the type of input the route can handle.

```java
{
  post("/", req -> {
    MyObject json = req.body().to(MyObject.class);

  }).consumes("json");
}
```

### produces

The {{route_produces}} operator indicates the type of output the route can produce.

```java
{
  get("/", req -> {
    return new MyObject();
  }).produces("json");
}
```
