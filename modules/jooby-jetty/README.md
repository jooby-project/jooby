[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jetty/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jetty)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jetty.svg)](https://javadoc.io/doc/org.jooby/jooby-jetty/1.3.0)
[![jooby-jetty website](https://img.shields.io/badge/jooby-jetty-brightgreen.svg)](http://jooby.org/doc/jetty)
# jetty

[Jetty](https://www.eclipse.org/jetty) provides a Web server and javax.servlet container, plus support for HTTP/2, WebSocket and many other integrations.

## exports

* Jetty Server

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jetty</artifactId>
  <version>1.3.0</version>
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

## server.conf
These are the default properties for jetty:

```properties
# jetty defaults

server.module = org.jooby.jetty.Jetty

server.http2.cleartext = true

jetty {

  threads {

    MinThreads = ${server.threads.Min}

    MaxThreads = ${server.threads.Max}

    IdleTimeout = ${server.threads.IdleTimeout}

    Name = jetty task

  }

  FileSizeThreshold = 16k

  http {

    HeaderCacheSize = ${server.http.HeaderSize}

    RequestHeaderSize = ${server.http.HeaderSize}

    ResponseHeaderSize = ${server.http.HeaderSize}

    OutputBufferSize = ${server.http.ResponseBufferSize}

    SendServerVersion = false

    SendXPoweredBy = false

    SendDateHeader = false

    connector {

      # The accept queue size (also known as accept backlog)

      AcceptQueueSize = 0

      # Use -1 to disable

      SoLingerTime = -1

      StopTimeout = 30000

      # Sets the maximum Idle time for a connection, which roughly translates to the Socket#setSoTimeout(int)

      # call, although with NIO implementations other mechanisms may be used to implement the timeout.

      # The max idle time is applied:

      #   When waiting for a new message to be received on a connection

      #   When waiting for a new message to be sent on a connection

      # This value is interpreted as the maximum time between some progress being made on the connection.

      # So if a single byte is read or written, then the timeout is reset.

      IdleTimeout = ${server.http.IdleTimeout}

    }

  }

  ws {

    # The maximum size of a text message during parsing/generating.

    # Text messages over this maximum will result in a close code 1009 {@link StatusCode#MESSAGE_TOO_LARGE}

    MaxTextMessageSize = ${server.ws.MaxTextMessageSize}

    # The maximum size of a text message buffer.

    # Used ONLY for stream based message writing.

    MaxTextMessageBufferSize = 32k

    # The maximum size of a binary message during parsing/generating.

    # Binary messages over this maximum will result in a close code 1009 {@link StatusCode#MESSAGE_TOO_LARGE}

    MaxBinaryMessageSize = ${server.ws.MaxBinaryMessageSize}

    # The maximum size of a binary message buffer

    # Used ONLY for for stream based message writing

    MaxBinaryMessageBufferSize = 32k

    # The timeout in ms (milliseconds) for async write operations.

    # Negative values indicate a disabled timeout.

    AsyncWriteTimeout = 60000

    # The time in ms (milliseconds) that a websocket may be idle before closing.

    IdleTimeout = ${server.ws.IdleTimeout}

    # The size of the input (read from network layer) buffer size.

    InputBufferSize = 4k

  }

  url.charset = ${application.charset}

}
```
