## dynamic / advanced routing

Suppose you need to serve different content based on hostname. For example ```admin.foo.com``` serves on ```/``` something different than ```www.foo.com```. Even there is different logic how to use it.

```java
{
   use("*", (req, rsp, chain) -> {
     if (...) {
       chain.next("/admin", req, rsp);
     } else {
       chain.next("/normal", req, rsp);
     }
   });

   get("/", () -> "Hello admin").name("admin");

   get("/", () -> "Hello user").name("normal");
}
```

We start by adding a filter:

```java
   use("*", (req, rsp, chain) -> {
     if (...) {
       chain.next("/admin", req, rsp);
     } else {
       chain.next("/normal", req, rsp);
     }
   });
```

Here we make some decision and ask chain to move next by applying a filter: ```/admin``` or ```/normal```. The chain will filter and keep all the routes where name starts with the given prefix.

It is possible to group routes by features and set the name globally:

```java
{
  use("/")
    .get(() -> "Hello admin")
    ...
    // apply name to all routes
    .name("admin");

  use("/")
    .get(() -> "Hello user")
    ...
    // apply name to all routes
    .name("normal");
}
```

Or group routes by features in their own app and then merge them into the main app:

```java
public class Admin extends Jooby {

  public Admin(String prefix) {
    super(prefix);
  }

  {
    get("/", () -> "Hello admin");
    ...
  }
}

public class Normal extends Jooby {

  public Normal(String prefix) {
    super(prefix);
  }

  {
    get("/", () -> "Hello user");
    ...
  }
}

/** merge everything .*/
public class App extends Jooby {
  {
     use("*", (req, rsp) -> {
       if (...) {
         chain.next("/admin", req, rsp);
       } else {
         chain.next("/normal", req, rsp);
       }
     });

     use(new Admin("admin"));
     use(new Normal("normal"));
  }
}
```

A call to ```Jooby(String)``` will prepend the given prefix to all the routes defined by the application.

As you can see routes and routing in {{jooby}} are very powerful!
