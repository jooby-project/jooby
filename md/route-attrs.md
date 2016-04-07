## attributes

It is possible to add metadata to routes via ```.attr(String, String)```:

```java
{
  get("/path", ..)
    .attr("foo", "bar");
}
```

You can add as many attributes as you need. They can be accessed later and use it in one way or another. For example, a security module might looks for ```role``` attribute, a sitemap generator might like for ```priority``` attribute, etc...

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

In MVC routes we use ```annotations``` to define route attributes:

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

Any runtime annotations are automatically added as route attributes. Following these rules:

* If the annotation has a ```value``` method, then we use the annotation's name as attribute name.
* Otherwise, we use the method name as attribute name.

> **request attributes vs route attributes:**
>
> Route attributes are created at bootstrap time they are global and once you set they won't change.
>
> While, request attributes are created in a request/response cycle.

### excludes

The excludes attribute skip/ignore a route path match:

```java
{
   use("*", (req, rsp) -> {
     // all except /login
   }).excludes("/login");
}
```

### with

The with operator help to set common attributes, consumes and produces types, exclusions, etc.. to one or more routes:

```java
{
  with(() -> {

    get("/admin/1", ...);

    get("/admin/2", ...);

  }).attr("role", "admin");
}
```
