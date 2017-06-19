# jetty

[Jetty](https://www.eclipse.org/jetty) provides a Web server and javax.servlet container, plus support for HTTP/2, WebSocket and many other integrations.

## exports

* Jetty Server

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jetty</artifactId>
  <version>1.1.3</version>
</dependency>
```

## usage

In order to use a web server all you have to do is add the dependency to your build system.
[Jooby](http://jooby.org) will find the server and start it.

## http/2

`HTTP/2` is fully supported:

```
| H2        | H2C           | PUSH  |
| --------- | ------------- | ----- |
| Yes       | Yes           | Yes   |
```

You need to add `alpn-boot` to the `JVM bootstrap` path. See [Jetty HTTP2](https://www.eclipse.org/jetty/documentation/9.3.x/http2.html) documentation.
