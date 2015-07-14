---
layout: index
title: netty
version: 0.8.1
---

# jooby-netty

[Netty](http://netty.io) web server for Jooby.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-netty</artifactId>
  <version>0.8.1</version>
</dependency>
```

## usage

In order to use a web server all you have to do is add the dependency to your build system.
[Jooby](/) will find the server and start it.


# appendix: server.conf

```properties
# netty defaults
server.module = org.jooby.netty.Netty

netty {

  http {
    MaxInitialLineLength = 4k

    MaxHeaderSize = ${server.http.HeaderSize}

    MaxChunkSize = 8k

    MaxContentLength = ${server.http.MaxRequestSize}

    IdleTimeout = ${server.http.IdleTimeout}
  }

  threads {
    Min = ${server.threads.Max}

    Max = ${server.threads.Max}

    IdleTimeout = ${server.threads.IdleTimeout}

    Name = netty

    Parent = ${runtime.processors-x2}
  }

  options {
    SO_BACKLOG = 1024
    SO_REUSEADDR = true
  }

  child {
    options {
      SO_REUSEADDR = true
    }
  }
}

```
