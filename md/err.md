# err

Error handler is represented by [Err.Handler]({{defdocs}}/Err.Handler.html) class and allows you to log and/or display custom error pages.

## default err handler

The [default error handler]({{defdocs}}/Err.Default.html) does content negotiation and attempt to
display friendly err pages using naming convention.

```java
{
  use(new TemplateEngine()); // Hbs, Ftl, etc...
  use(new Json()); // A json body formatter

  get("/", () -> {
    ...
    throw new IllegalArgumentException();
    ...
  });
}
```

### html

If a request to ```/``` has an ```Accept: text/html``` header. Then, the default err handler will
ask to a [View.Engine]({{defdocs}}/View.Engine.html) to render the ```err``` view.

The default model has these attributes:

* message: exception string
* stacktrace: exception stack-trace as an array of string
* status: status code, like ```400```
* reason: status code reason, like ```BAD REQUEST```
* referer: referer header (if present)

Here is a simply ```public/err.html``` error page:

```html
<html>
<body>
  {{ "{{status" }}}}:{{ "{{reason" }}}}
</body>
</html>
```

HTTP status code will be set too.

### no html

If a request to ```/``` has an ```Accept: application/json``` header. Then, the default err handler will
ask to a [Formatter]({{defdocs}}/BodyFormatter.html) to render the ```err``` model.

```json
{
  "message": "...",
  "stacktrace": [],
  "status": 500,
  "reason": "..."
}
```

HTTP status code will be set too.

## custom err handler

If the default view resolution and/or err model isn't enough, you can create your own err handler.

```java
{
  err((req, rsp, cause) -> {
    log.err("err found: ", cause);
    // do what ever you want here
  });
}
```

A good practice is to always log the err and then build a custom page or any other response you want.

## status code

Default status code is ```500```, except for:

| Exception                                | Status Code |
| ---------------------------------------- | ----------- |
| ```java.lang.IllegalArgumentException``` |  ```400```  |
| ```java.util.NoSuchElementException```   |  ```400```  |
| ```java.io.FileNotFoundException```      |  ```404```  |

### custom status code

Just throw an [Err]({{defdocs}}/Err.html):

```java
throw new Err(403);
```

or add a new entry in the ```application.conf``` file:

```properties
err.com.security.Forbidden = 403
```

```java
throw new Forbidden();
```

## fallback err handler

If the err handler failed by any reason, the fallback err handler will be executed. This err handler
just display a generic HTML err page.
