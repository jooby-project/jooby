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

A [web socket]({{defdocs}}/WebSocket.html) consist of a **path pattern** and a [handler]({{defdocs}}/WebSocket.Handler.html).

A **path pattern** can be as simple or complex as you need. All the path patterns supported by routes are supported here.

The [onOpen]({{defdocs}}/WebSocket.OnOpen.html) listener is executed on new connections, from there we can listen for message, errors and/or send data to the client.

Keep in mind that **web socket** are not like routes. There is no stack/pipe or chain.

You can mount a socket to a path used by a route, but you can't have two or more web sockets under the same path.

## send and broadcast

As you saw early a [web socket]({{defdocs}}/WebSocket.html) and send data to client via [ws.send(...)]({{defdocs}}/WebSocket.html#send-java.lang.Object-) method.

The [ws.broadcast(...)]({{defdocs}}/WebSocket.html#broadcast-java.lang.Object-) method does the same thing but for all the connected clients.

## require

Access to existing services is provided via [ws.require(type)]({{defdocs}}/WebSocket.html#require-com.google.inject.Key-) method:

```java
ws("/", ws -> {
  A a = ws.require(A.class);
});
```

## listener

As with **routes** we do provide **two flavors** for `WebSockets`:

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

Optionally, your listener could implements [onOpen]({{defdocs}}/WebSocket.OnOpen.html), [onClose]({{defdocs}}/WebSocket.OnClose.html) or [onError]({{defdocs}}/WebSocket.OnError.html). If you need them all then use [handler]({{defdocs}}/WebSocket.Handler.html) interface.

## consumes

Web socket can define a type to consume:

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

This is just an utility method for parsing socket message to Java Object. Consumes in web sockets has nothing to do with content negotiation. Content negotiation is route concept, it doesn't apply for web sockets.

## produces

Web socket can define a type to produce: 

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

This is just an utility method for formatting Java Objects as text message. Produces in web sockets has nothing to do with content negotiation. Content negotiation is route concept, it doesn't apply for web sockets.
