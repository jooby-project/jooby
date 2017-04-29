# undertow

[Undertow](http://undertow.io/) is a flexible performant web server written in java, providing both blocking and non-blocking API’s based on NIO.

## exports

* `Server`

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-undertow</artifactId>
  <version>1.1.1</version>
</dependency>
```

## usage

In order to use a web server all you have to do is add the dependency to your build system.

## http/2

`HTTP/2` is fully supported:

```
| H2        | H2C           | PUSH  |
| --------- | ------------- | ----- |
| Yes       | Yes           | Yes   |
```

You need `Java 8 Update 71` or higher (integration tests uses `Java 8 Update 101`).

See [Java 8 releases](https://www.java.com/en/download/faq/release_dates.xml) for more details.

## server.conf

```properties
# undertow defaults

server.module = org.jooby.undertow.Undertow

server.http2.cleartext = true

undertow {

  bufferSize = ${server.http.ResponseBufferSize}

  workerThreads = ${server.threads.Max}

  # Waits a set length of time for the handler to shut down. It provides a way to prevent the server

  # from accepting new requests, and wait for existing requests to complete

  awaitShutdown = 1000

  server {

    ALLOW_EQUALS_IN_COOKIE_VALUE = true

    ALWAYS_SET_KEEP_ALIVE = true

    # The maximum size in bytes of a http request header.

    MAX_HEADER_SIZE = ${server.http.HeaderSize}

    # The default maximum size of the HTTP entity body.

    MAX_ENTITY_SIZE = ${server.http.MaxRequestSize}

    # The maximum number of parameters that will be parsed. This is used to protect against hash

    # vulnerabilities.

    # This applies to both query parameters, and to POST data, but is not cumulative

    # (i.e. you can potentially have max parameters * 2 total parameters).

    MAX_PARAMETERS = 1000

    # The maximum number of headers that will be parsed. This is used to protect against hash

    # vulnerabilities.

    MAX_HEADERS = 200

    # The maximum number of headers that will be parsed. This is used to protect against hash

    # vulnerabilities.

    MAX_COOKIES = 200

    URL_CHARSET = ${application.charset}

    # If this is true then the parser will decode the URL and query parameters using the selected

    # character encoding (URL_CHARSET by default). If this is false they will not be decoded.

    # This will allow a later handler to decode them into whatever charset is desired.

    DECODE_URL = true

    # The idle timeout in milliseconds after which the channel will be closed.

    # If the underlying channel already has a read or write timeout set the smaller of the two values will be used

    # for read/write timeouts.

    IDLE_TIMEOUT = ${server.http.IdleTimeout}

  }

  worker {

    WORKER_NAME = utow

  }

  socket {

    BACKLOG = 1024

  }

  ws {

    # The maximum size of a text message during parsing/generating.

    MaxTextBufferSize = ${server.ws.MaxTextMessageSize}

    # The maximum size of a binary message during parsing/generating.

    MaxBinaryBufferSize = ${server.ws.MaxBinaryMessageSize}

    # The time in ms (milliseconds) that a websocket may be idle before closing.

    IdleTimeout = ${server.ws.IdleTimeout}

  }

}
```
