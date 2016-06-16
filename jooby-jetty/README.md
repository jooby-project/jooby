# jetty

NIO web server via: [Jetty](https://www.eclipse.org/jetty).

## exports

* Jetty Server

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jetty</artifactId>
  <version>1.0.0.CR5</version>
</dependency>
```

## usage

In order to use a web server all you have to do is add the dependency to your build system.
[Jooby](http://jooby.org) will find the server and start it.


## server.conf

```properties
# jetty defaults

server.module = org.jooby.jetty.Jetty

jetty {

  threads {

    MinThreads = ${server.threads.Min}

    MaxThreads = ${server.threads.Max}

    IdleTimeout = ${server.threads.IdleTimeout}

    Name = jetty task

  }

  http {

    HeaderCacheSize = ${server.http.HeaderSize}

    RequestHeaderSize = ${server.http.HeaderSize}

    ResponseHeaderSize = ${server.http.HeaderSize}

    OutputBufferSize = ${server.http.ResponseBufferSize}

    FileSizeThreshold = 16k

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
