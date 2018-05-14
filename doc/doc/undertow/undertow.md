# undertow

[Undertow](http://undertow.io/) is a flexible performant web server written in java, providing both blocking and non-blocking API’s based on NIO.

## exports

* `Server`

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-undertow</artifactId>
  <version>{{version}}</version>
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

{{appendix}}
