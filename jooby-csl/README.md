# csl

<a href="https://github.com/coverity/coverity-security-library">Coverity Security Library (CSL)</a> is a lightweight set of escaping routines for fixing cross-site scripting (XSS), SQL injection, and other security defects in Java web applications

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-csl</artifactId>
 <version>1.1.0</version>
</dependency>
```

## exports

* **html** escaper: HTML entity escaping for text content and attributes. 
* **htmlText** escaper: Faster HTML entity escaping for tag content or quoted attributes values only. 
* **js** escaper: JavaScript String Unicode escaper. 
* **jsRegex** escaper: JavaScript regex content escaper. 
* **css** escaper: CSS String escaper. 
* **uri** escaper: URI encoder. 

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

If you want to learn more about nested context and why they are important have a look at this <a href="http://security.coverity.com/document/2013/Mar/fixing-xss-a-practical-guide-for-developers.html">nice guide from </a><a href="https://github.com/coverity/coverity-security-library">coverity-security-library</a>.
