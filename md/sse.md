# server-sent events

Server-Sent Events (SSE) is a mechanism that allows server to push the data from the server to the client once the client-server connection is established by the client. Once the connection is established by the client, it is the server who provides the data and decides to send it to the client whenever new **chunk** of data is available.

## usage

```java
{
  sse("/path", sse -> {

    // 1. connected
    sse.send("data"); // 2. send/push data
  });

}
```

Simple, effective and easy to use. The callback will be executed once when a new client is connected. Inside the callback we can send data, listen for connection close events, etc.

There is a factory method [sse.event(Object)]({{apidocs}}/org/jooby/Sse.html#event-java.lang.Object-) that let you set event attributes:

```java
{
  sse("/path", sse -> {

    sse.event("data")
        .id("id")
        .name("myevent")
        .retry(5000L)
        .send();
  });

}
```

## structured data

Beside raw/string data you can also send structured data, like ```json```, ```xml```, etc..

The next example will send two message one in ```json``` format and one in ```text/plain``` format:

```java
{
  use(new MyJsonRenderer());

  sse("/path", sse -> {

    MyObject object = ...
    sse.send(object, "json");
    sse.send(object, "plain");
  });
}
```

Or if your need only one format, just:

```java
{
  use(new MyJsonRenderer());

  sse("/path", sse -> {

    MyObject object = ...
    sse.send(object);
  }).produces("json"); // by json by default
}
```

## request params

We provide request access via two arguments callback:

```java
{
  sse("/events/:id", (req, sse) -> {

    String id = req.param("id").value();
    MyObject object = findObject(id);
    sse.send(object);
  });

}
```

## connection lost

The [sse.onClose(Runnable)]({{apidocs}}/org/jooby/Sse.html#onClose-javaslang.control.Try.CheckedRunnable-) callback allow you to clean and release resources on connection close. A connection is closed when you call [sse.close()]({{apidocs}}/org/jooby/Sse.html#close--) method or the client/browser close the connection.

```java
{
  sse("/events/:id", sse -> {

    sse.onClose(() -> {
      // clean up resources
    });
  });

}
```

A connection close event is detected and fired when you attempt to send some data.

## keep alive time

The keep alive time feature can be used to prevent connections from timing out:

```java
{
  sse("/events/:id", sse -> {

    sse.keepAlive(15, TimeUnit.SECONDS);
  });

}
```

The previous example will sent a ```':'``` message (empty comment) every 15 seconds to keep the connection alive. If the client drop the connection, then the [sse.onClose(Runnable)]({{apidocs}}/org/jooby/Sse.html#onClose-javaslang.control.Try.CheckedRunnable-) event will be fired it.

This feature is useful when you want to detect close events without waiting for the next time you send an event. But for example, if your application already generate events every 15s, then the use of keep alive is useless and you can avoid it.

## require

The [sse.require(Type)]({{apidocs}}/org/jooby/Sse.html#require-java.lang.Class-) methods let you access to application services:

```java
{
  sse("/events/:id", sse -> {

    MyService service = sse.require(MyService.class);
  });

}
```

## example

The next example will generate a new event every 60s. It recovers from a server shutdown by using the [sse.lastEventId()]({{apidocs}}/org/jooby/Sse.html#lastEventId--) and clean resources on connection close.

```java
{
  // creates an executor service
  ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

  sse("/events", sse -> {

    // if we go down, recover from last event ID we sent. Otherwise, start from zero.
    int lastId = sse.lastEventId(Integer.class).orElse(0);
    AtomicInteger next = new AtomicInteger(lastId);

    // send events every 60s
    ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
      Integer id = next.incrementAndGet();
      Object data = findDataById(id);
      // send data and id
      sse.event(data).id(id).send();
    }, 0, 60, TimeUnit.SECONDS);

    // on connection lost, cancel 60s task
    sse.onClose(() -> {
      future.cancel(true);
    });
  });

}
```
