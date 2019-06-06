[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-unbescape/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-unbescape/1.6.2)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-unbescape.svg)](https://javadoc.io/doc/org.jooby/jooby-unbescape/1.6.2)
[![jooby-unbescape website](https://img.shields.io/badge/jooby-unbescape-brightgreen.svg)](http://jooby.org/doc/unbescape)
# unbescape

<a href="https://github.com/unbescape/unbescape">Unbescape</a> is a Java library aimed at performing fully-featured and high-performance escape and unescape operations for: ```HTML```, ```JavaScript``` and lot more.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-unbescape</artifactId>
 <version>1.6.2</version>
</dependency>
```

## exports

* **html** escaper. 
* **js** escaper. 
* **json** escaper. 
* **css** escaper. 
* **uri** escaper. 
* **queryParam** escaper. 
* **uriFragmentId** escaper. 

## usage

```java
{
  use(new XSS());

  post("/", req -> {
    String safeHtml = req.param("text", "html").value();
  });

}
```

Nested context are supported by providing multiple encoders:

```java
{
  use(new XSS());

  post("/", req -> {
    String safeHtml = req.param("text", "js", "html", "uri").value();
  });

}
```

Encoders run in the order they are provided.
