# undertow

[Undertow](http://undertow.io/) web server for Jooby.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-undertow</artifactId>
  <version>0.12.0</version>
</dependency>
```

## usage

In order to use a web server all you have to do is add the dependency to your build system.
[Jooby](http://jooby.org) will find the server and start it.


## server.conf

```properties
# undertow defaults

server.module = org.jooby.undertow.Undertow

undertow {

  bufferSize = ${server.http.ResponseBufferSize}

  workerThreads = ${server.threads.Max}

  server {

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
