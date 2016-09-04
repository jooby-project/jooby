# undertow

NIO web server via [Undertow](http://undertow.io/).

## exports

* Undertow server

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
{{Jooby}} will find the server and start it.

## http/2

`HTTP/2` is fully supported:

```
| H2        | H2C           | PUSH  |
| --------- | ------------- | ----- |
| Yes       | Yes           | Yes   |
```

{{appendix}}
