# web sockets

The use of web sockets is pretty easy too:

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

A [handler]({{defdocs}}/WebSocket.Handler.html) is executed on new connections, from there we can listen for message, errors and/or send data to the client.

Keep in mind that **web socket** are not like routes. There is no stack/pipe or chain.

You can mount a socket to a path used by a route, but you can't have two or more web sockets under the same path.

## guice access

You can ask [Guice](https://github.com/google/guice) to wired an object from the [ws.require(type)]({{defdocs}}/WebSocket.html#require-com.google.inject.Key-)

```java
ws("/", (ws) -> {
  A a = ws.require(A.class);
});
```

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

This is just an utility method for parsing socket message to Java Object. Consumes in web sockets has nothing to do with content negotiation. Content negotiation is route concept, it doesn't apply for web sockets.

## produces

Web socket can define a type to produce: 

```java
{
  ws("/", ws -> {
   MyObject object = ..;
     ws.send(object);
  })
  .produces("json");
}
```

This is just an utility method for formatting Java Objects as text message. Produces in web sockets has nothing to do with content negotiation. Content negotiation is route concept, it doesn't apply for web sockets.
