# whoops

Pretty error page that helps you debug your web application.

<img alt="whoops!" width="75%" src="http://jooby.org/resources/images/whoops.png">

**NOTE**: This module is base on <a href="https://github.com/filp/whoops">whoops</a> and uses the same front end resources.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-whoops</artifactId>
 <version>{{version}}</version>
</dependency>
```

## exports

* A [pretty error page]({{defdocs}}/Err.Handler.html) 

## usage

```java
{
  use(new Whoops());

  get("/", req -> {

    throw new IllegalStateException("Something broken!");

  });

}
```

The pretty error page handler is available in development mode: ```application.env = dev```

## custom err pages

The pretty error page is implemented via [err(req, rsp, err)]({{defdocs}}/Routes.html#err-org.jooby.Err.Handler-). You might run into troubles if your application require custom error pages. On those cases you probably won't use this module or apply one of the following options:

### whoops as last err handler

This option is useful if you have custom error pages on some specific exceptions:

```java
{
  err(NotFoundException.class, (req, rsp, err) -> {
   // custom not found
  });

  err(AccessDeniedException.class, (req, rsp, err) -> {
   // custom access denied
  });

  // not handled it use whoops
  use(new Whoops());

}
```

Here the custom error page for ```NotFoundException``` or ```AccessDeniedException``` will be render before the ```Whoops``` error handler.

### whoops on dev

This options will active ```Whoops``` in ```dev``` and the custom err pages in ```prod-like``` environments:

```java
{
  on("dev", () -> {

    use(new Whoops());

  })
  .orElse(() -> {

    err((req, rsp, err) -> {
      // custom not found
    });

  });

}
```
