# web sockets

## usage

```java
{
  ws("/", ws -> {
    ws.onMessage(message -> System.out.println(message.value()));

    ws.send("connected");
  });
}
```

A [WebSocket]({{defdocs}}/WebSocket.html) consists of a **path pattern** and a [handler]({{defdocs}}/WebSocket.Handler.html).

A **path pattern** can be as simple or complex as you need. All the path patterns supported by routes are supported.

The [onOpen]({{defdocs}}/WebSocket.OnOpen.html) listener is executed on new connections, from there it's possible to listen for messages and errors or to send data to the client.

Keep in mind that a **WebSocket** is different from a route. There is no stack/pipe or chain.

You can mount a socket to a path used by a route, but you can't have two or more WebSockets under the same path.

## send and broadcast

As seen earlier, a [web socket]({{defdocs}}/WebSocket.html) can send data to the client via the [ws.send(...)]({{defdocs}}/WebSocket.html#send-java.lang.Object-) method.

The [ws.broadcast(...)]({{defdocs}}/WebSocket.html#broadcast-java.lang.Object-) method does the same thing, but will send to all connected clients.

## require

Access to existing services is provided through the [ws.require(type)]({{defdocs}}/WebSocket.html#require-com.google.inject.Key-) method:

```java
ws("/", ws -> {
  A a = ws.require(A.class);
});
```

## listener

As with **routes** you can choose between **two flavors** for `WebSocket` listeners:

script:

```java
{
  ws("/ws", ws -> {
    MyDatabase db = ws.require(MyDatabase.class);
    ws.onMessage(msg -> {
      String value = msg.value();
      database.save(value);
      ws.send("Got: " + value);
    });
  });
}
```

class:

```java
{
  ws(MyHandler.class);
}

@Path("/ws")
class MyHandler implements WebSocket.OnMessage<String> {

  WebSocket ws;
  
  MyDatabase db;

  @Inject
  public MyHandle(WebSocket ws, MyDatabase db) {
    this.ws = ws;
    this.db = db;
  }

  @Override
  public void onMessage(String message) {
    database.save(value);
    ws.send("Got: " + message);
  }
}
```

Optionally, your listener could implement [onOpen]({{defdocs}}/WebSocket.OnOpen.html), [onClose]({{defdocs}}/WebSocket.OnClose.html) or [onError]({{defdocs}}/WebSocket.OnError.html). If you need all of them, use the [handler]({{defdocs}}/WebSocket.Handler.html) interface.

## consumes

A WebSocket can define a type to consume:

```java
{
  ws("/", ws -> {
    ws.onMessage(message -> {
      MyObject object = message.to(MyObject.class);
    });

    ws.send("connected");
  })
  .consumes("json");
}
```

Or via annotation for class listeners:

```java
@Path("/ws")
@Consumes("json")
class MyHandler implements WebSocket.OnMessage<MyObject> {

  public void onMessage(MyObject object) {
   ...
  }
}
```

This is just a utility method for parsing a socket message into a Java Object. Consumes in WebSockets has nothing to do with content negotiation. Content negotiation is a route concept, it doesn't apply for WebSockets.

## produces

A WebSocket can define a type to produce: 

```java
{
  ws("/", ws -> {
    MyResponseObject object = ..;
    ws.send(object);
  })
  .produces("json");
}
```

Or via annotation for class listeners:

```java
@Path("/ws")
@Consumes("json")
@Produces("json")
class MyHandler implements WebSocket.OnMessage<MyObject> {

  public void onMessage(MyObject object) {
   ws.send(new MyResponseObject());
  }
}
```

This is just a utility method for formatting Java Objects as text messages. Produces in WebSockets has nothing to do with content negotiation. Content negotiation is a route concept, it doesn't apply for WebSockets.
