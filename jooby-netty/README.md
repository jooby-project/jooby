# netty

NIO web server via [Netty](http://netty.io).

## exports

* Netty Server

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-netty</artifactId>
  <version>1.0.0.CR8</version>
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

## server.conf

```properties
# netty defaults

server.module = org.jooby.netty.Netty

server.http2.cleartext = true

netty {

  http {

    MaxInitialLineLength = 4k

    MaxHeaderSize = ${server.http.HeaderSize}

    MaxChunkSize = 16k

    MaxContentLength = ${server.http.MaxRequestSize}

    IdleTimeout = ${server.http.IdleTimeout}

  }

  threads {

    Min = ${server.threads.Min}

    Max = ${server.threads.Max}

    Name = netty task

    Boss = 1

    Worker = ${runtime.processors-x2}

  }

  options {

    SO_REUSEADDR = true

  }

  worker {

    options {

      SO_REUSEADDR = true

    }

  }

}
```
