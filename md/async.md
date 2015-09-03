## async routes

Async processing is achieved via: [Deferred API]({{apidocs}}/org/jooby/Deferred.html). Application can produces a result from a thread of its choice.

```java
{
  Executor executor = ...;
  get("/async", () -> {
    Deferred deferred = new Deferred();
    executor.execute(() -> {
      try {
        deferred.resolve(...); // success
      } catch (Exception ex) {
        deferred.reject(ex); // errr
      }
    });
    return deferred;
  });
}
```

Too verbose? Let's rewrite with a bit and use ```Jooby.promise```:

```java
{
  Executor executor = ...;
  get("/async", promise(deferred -> {
    executor.execute(() -> {
      try {
        deferred.resolve(...); // success
      } catch (Exception ex) {
        deferred.reject(ex); // errr
      }
    });
  });
}
```

A bit better, let's try it again with automatic error handler:

```java
{
  Executor executor = ...;
  get("/async", promise(deferred -> {
    executor.execute(() -> {
        deferred.resolve(() -> {
           return ...;
        });
    });
  });
}
```

Better, hugh? but we can make it just a bit better:

```java
{
  Executor executor = ...;
  get("/async", promise(deferred -> {
    executor.execute(deferred.run(() -> {
      return ...;
    });
  });
}
```

All the examples work in the same way, you can choose which programming model makes more sense to you.

The last two examples, will ```resolve``` the deferred. But also, catch any error and call ```reject``` for you.

[Deferred API]({{apidocs}}/org/jooby/Deferred.html) is pretty simple and the main goal of is to detach the request, or put it in async mode. Application can produces a result from a thread of its choice.

Here is an example of deferred with {{rx}}:

```java
get("/rx", promise(deferred -> {
  Observable.<List<String>> create(s -> {
    s.onNext(...);
    s.onCompleted();
  }).subscribeOn(Schedulers.computation())
    .subscribe(deferred::resolve, deferred::reject);
  }));
```

### threads

{{Jooby}} is an async web framework. IO is performed in async & non blocking fashion across servers: {{netty_server}}, {{jetty_server}} and {{undertow_server}}.

Application code will **never run in an IO thread**. Application code **always run in a worker thread** and a worker thread can block.

Default pool size of the worker thread pool is 20/200. You can change this setup via ```.conf``` file:

```properties
server.threads.Min=20
server.threads.Max=20
```

You can set them to the ```available processors```:

```properties
server.threads.Min = ${runtime.processors}
server.threads.Max = ${runtime.processors}
```
