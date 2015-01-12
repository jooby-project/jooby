# web sockets

The use of web sockets is pretty easy too:

```java
{
   ws("/", (ws) -> {
     ws.onMessage(message -> System.out.println(message.stringValue()));
     
     ws.send("connected");
   });
}
```

A [web socket](http://jooby.org/apidocs/org/jooby/WebSocket.html) consist of a **path pattern** and a [handler](http://jooby.org/apidocs/org/jooby/WebSocket.Handler.html).

A **path pattern** can be as simple or complex as you need. All the path patterns supported by routes are supported here.

A [handler](http://jooby.org/apidocs/org/jooby/WebSocket.Handler.html) is executed on new connections, from there we can listen for message, errors and/or send data to the client.

Keep in mind that **web socket** are not like routes. There is no stack/pipe, chain or request modules.

You can mount a socket to a path used by a route, but you can't have two or more web sockets under the same path.

## guice access

You can ask [Guice](https://github.com/google/guice) to wired an object from the [ws.getInstance(type)](http://jooby.org/apidocs/org/jooby/WebSocket.html#getInstance-com.google.inject.Key-)

```java
ws("/", (ws) -> {
  A a = ws.getInstance(A.class);
});
```

But remember, there isn't a child injector and/or request objects.

## consumes

Web socket can define a type to consume:

```
{
   ws("/", (ws) -> {
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

```
{
   ws("/", (ws) -> {
     MyObject object = ..;
     ws.send(object);
   })
   .produces("json");
}
```

This is just an utility method for formatting Java Objects as text message. Produces in web sockets has nothing to do with content negotiation. Content negotiation is route concept, it doesn't apply for web sockets.

