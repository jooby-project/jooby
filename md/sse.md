# server-sent events

[Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events) (SSE) is a mechanism that allows the server to push data to the client once the client-server connection is established. After the connection has been established by the client, the server can send to the client whenever a new **chunk** of data is available. In contrast with websockets, SSE can only be used to send from the server to the client and not the other way round.

## usage

```java
{
  sse("/path", sse -> {

    // 1. connected
    sse.send("data"); // 2. send/push data
  });

}
```

Simple, effective and easy to use. The callback will be executed when a new client is connected. Inside the callback you can send data, listen for connection close events, etc.

The factory method [sse.event(Object)]({{apidocs}}/org/jooby/Sse.html#event-java.lang.Object-) will let you set event attributes:

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

Other than raw/string data you can also send structured data, like ```json```, ```xml```, etc..

The next example will send two messages, one in ```json``` format and one in ```text/plain``` format:

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
  }).produces("json"); // json by default
}
```

## request parameters

Request access is provided by a **two argument** callback:

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

The [sse.onClose(Runnable)]({{apidocs}}/org/jooby/Sse.html#onClose-javaslang.control.Try.CheckedRunnable-) callback allows you to clean and release resources on connection close. A connection is closed when you call the [sse.close()]({{apidocs}}/org/jooby/Sse.html#close--) method or when the remote client closes the connection.

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

The previous example will send a ```':'``` message (empty comment) every 15 seconds to keep the connection alive. If the client drops the connection, then the [sse.onClose(Runnable)]({{apidocs}}/org/jooby/Sse.html#onClose-javaslang.control.Try.CheckedRunnable-) event will be fired.

This feature is useful when you want to detect close events without waiting for the next time you send an event. If on the other hand your application already generates events every 15 seconds, the use of keep alive is unnecessary.

## require

The [sse.require(Type)]({{apidocs}}/org/jooby/Sse.html#require-java.lang.Class-) methods gives you access to application services:

```java
{
  sse("/events/:id", sse -> {

    MyService service = sse.require(MyService.class);
  });

}
```

## example

The next example will generate a new event every 60 seconds. It recovers from a server shutdown by using the [sse.lastEventId()]({{apidocs}}/org/jooby/Sse.html#lastEventId--) and cleans resources on connection close.

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
