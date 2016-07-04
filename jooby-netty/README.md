# netty

NIO web server via [Netty](http://netty.io).

## exports

* Netty Server

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-netty</artifactId>
  <version>1.0.0.CR6</version>
</dependency>
```

## usage

In order to use a web server all you have to do is add the dependency to your build system.
[Jooby](http://jooby.org) will find the server and start it.


## server.conf

```properties
# netty defaults

server.module = org.jooby.netty.Netty

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

    Parent = ${runtime.processors-x2}

  }

  options {

    SO_REUSEADDR = true

  }

  child {

    options {

      SO_REUSEADDR = true

    }

  }

}
```
