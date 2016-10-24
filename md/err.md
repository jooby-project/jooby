# error handling

Error handler is represented by the [Err.Handler]({{defdocs}}/Err.Handler.html) class and allows you to log and/or render exceptions.

## default err handler

The [default error handler]({{defdocs}}/Err.DefHandler.html) does content negotiation and optionally display friendly err pages using naming convention.

```java
{
  use(new TemplateEngine()); // Hbs, Ftl, etc...
  use(new Json()); // A json renderer

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
ask to a [renderer]({{defdocs}}/Renderer.html) to render the ```err``` model.

```json
{
  "message": "...",
  "stacktrace": [],
  "status": 500,
  "reason": "..."
}
```

In both cases, the error model is the result of ```err.toMap()``` which creates a lightweight version of the exception.

HTTP status code will be set too.

## custom err handler

If the default view resolution and/or err model isn't enough, you can create your own err handler:

```java
{
  err((req, rsp, err) -> {
    log.err("err found: ", err);
    // do what ever you want here
    rsp.send(...);
  });
}
```

Err handler are executed in the order they were provided (like routes, parsers and renderers).
The first err handler that send an output wins!

### catch specific exception or status code


```java
{
  err(MyException1.class, (req, rsp, err) -> {
    MyException1 cause = (MyException1) err.getCause();
    // handle MyException1
  });

  err(MyException2.class, (req, rsp, err) -> {
    MyException2 cause = (MyException2) err.getCause();
    // handle MyException2
  });

  err((req, rsp, err) -> {
    // handle any other exception
  });
}
```

Or you can catch exception base on their response status code (see next section):

```java
{
  err(404, (req, rsp, err) -> {
    // handle 404
  });

  err(503, (req, rsp, err) -> {
    // handle 503
  });

  err((req, rsp, err) -> {
    // handle any other exception
  });
}
```

## status code

Default status code is ```500```, except for:

```
| Exception                          | Status Code |
| ---------------------------------- | ----------- |
| java.lang.IllegalArgumentException |     400     |
|                                    |             |
| java.util.NoSuchElementException   |     400     |
|                                    |             |
| java.io.FileNotFoundException      |     404     |
```

### custom status code

Just throw an [Err]({{defdocs}}/Err.html):

```java
throw new Err(403);
```

or add a new entry in the ```application.conf``` file:

```properties
err.com.security.Forbidden = 403
```

So, now if you throw a ```com.security.Forbidden``` exception the status code will be ```403```.
