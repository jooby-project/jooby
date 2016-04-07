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
    String role = req.route.attr("role");
    if (user.hasRole(role)) {
      chain.next(req, rsp);
    }
    throw new Err(403);
  });

}
```

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
