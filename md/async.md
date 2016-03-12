# async

In {{jooby}} all the IO operations are performed in async & non blocking fashion across servers: {{netty_server}}, {{jetty_server}} and {{undertow_server}}.

All IO operations run in an IO thread while application logic **always run in a worker thread**. Worker threads can block at any time.

Having said that let see what tools are available for async/reactive programming.

## threads

Default worker thread pool is 20/100. You can change this setup via ```.conf``` file:

```properties
server.threads.Min=20
server.threads.Max=20
```

You can set them to the ```available processors```:

```properties
server.threads.Min = ${runtime.processors}
server.threads.Max = ${runtime.processors}
```

Or pick one variant: ```runtime.processors-plus1```, ```runtime.processors-plus2``` or ```runtime.processors-x2```.

This is a typical setup if you want to build your application using **reactive** programming.

## deferred

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

Too verbose? Let's rewrite it a bit with: ```Jooby.promise```

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

All the examples work in the same way, you can choose which programming model makes **more sense to you**.

The last two examples, will ```resolve``` the deferred. But also, catch any error and call ```reject``` for you.

[Deferred API]({{apidocs}}/org/jooby/Deferred.html) is pretty simple and the main goal of is to detach the request, or put it in async mode then you can produces a result from a thread of its choice.

## RxJava example

Here is an example of deferred with {{rx}}:

```java
{
  get("/rx", promise(deferred -> {
    Observable.<List<String>> create(s -> {
      s.onNext(...);
      s.onCompleted();
    }).subscribeOn(Schedulers.computation())
      .subscribe(deferred::resolve, deferred::reject);
    }));
}
```

And of course the threads setup:

```properties
server.threads.Min = ${runtime.processors}
server.threads.Max = ${runtime.processors}
```
