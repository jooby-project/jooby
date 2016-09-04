# netty

NIO web server via [Netty](http://netty.io).

## exports

* Netty Server

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-netty</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

In order to use a web server all you have to do is add the dependency to your build system.
{{Jooby}} will find the server and start it.

## http/2

`HTTP/2` is fully supported:

```
| H2        | H2C           | PUSH  |
| --------- | ------------- | ----- |
| Yes       | Yes           | Yes   |
```

{{appendix}}
