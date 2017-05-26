# unbescape

<a href="https://github.com/unbescape/unbescape">Unbescape</a> is a Java library aimed at performing fully-featured and high-performance escape and unescape operations for: ```HTML```, ```JavaScript``` and lot more.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-unbescape</artifactId>
 <version>1.1.2</version>
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
