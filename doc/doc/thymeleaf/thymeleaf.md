# thymeleaf

<a href="http://www.thymeleaf.org">Thymeleaf</a> is a modern server-side Java template engine for both web and standalone environments.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-thymeleaf</artifactId>
 <version>{{version}}</version>
</dependency>
```

## exports

* TemplateEngine 

## usage

```java
{
  use(new Thl());
  get("/", () -> {
    return Results.html("index")
        .put("model", new MyModel());
  });

  // Or Thymeleaf API:
  get("/thymeleaf-api", () -> {
    TemplateEngine engine = require(TemplateEngine.class);
    engine.process("template", ...);
  });
}
```

Templates are loaded from root of classpath: ```/``` and must end with: ```.html``` file extension. Example:

public/index.html:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<p>
Hello <span th:text="${model.name}">World</span>!!!
</p>
</body>
</html>
```

## template loader

Templates are loaded from the ```root``` of classpath and must end with ```.html```. You can change the default template location and/or extensions:

```java
{
  use(new Thl("templates", ".thl"));
}
```

## request locals

A template engine has access to request locals (a.k.a attributes). Here is an example:

```java
{
  use(new Thl());

  get("*", req -> {
    req.set("foo", bar);
  });

}
```

Then from template:

```html
<span th:text="${who}">World</span>
```

## template cache

Cache is OFF when ```env=dev``` (useful for template reloading), otherwise is ON.

## advanced configuration

Advanced configuration if provided by callback:

```java
{
  use(new Thl().doWith(engine -> {
    engine.addDialect(...);
  }));

}
```
