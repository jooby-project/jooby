[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-netty/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-netty/1.6.1)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-netty.svg)](https://javadoc.io/doc/org.jooby/jooby-netty/1.6.1)
[![jooby-netty website](https://img.shields.io/badge/jooby-netty-brightgreen.svg)](http://jooby.org/doc/netty)
# netty

[Netty](http://netty.io) is an asynchronous event-driven network application framework for rapid development of maintainable high performance protocol servers & clients.

## exports

* `Server`

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-netty</artifactId>
  <version>1.6.1</version>
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

No extra configuration is necessary.

## server.conf
These are the default properties for netty:

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
