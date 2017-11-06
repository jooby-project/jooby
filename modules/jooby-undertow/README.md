[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-undertow/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-undertow)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-undertow.svg)](https://javadoc.io/doc/org.jooby/jooby-undertow/1.2.2)
[![jooby-undertow website](https://img.shields.io/badge/jooby-undertow-brightgreen.svg)](http://jooby.org/doc/undertow)
# undertow

[Undertow](http://undertow.io/) is a flexible performant web server written in java, providing both blocking and non-blocking API’s based on NIO.

## exports

* `Server`

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-undertow</artifactId>
  <version>1.2.2</version>
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
