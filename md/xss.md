# xss

[Cross-site scripting (XSS)](https://en.wikipedia.org/wiki/Cross-site_scripting) is a type of computer security vulnerability typically found in web applications. XSS enables attackers to inject client-side scripts into web pages viewed by other users.

{{Jooby}} provides a few XSS escapers and a simple and flexible way to provide custom and/or more featured XSS escapers.

Default XSS escapers are `urlFragment`, `formParam`, `pathSegment` and `html` all them from [Guava](https://github.com/google/guava).

More advanced/featured escapers like `js`, `css`, `sql` are provided via [modules](/doc/security).

## usage

There are a few way of using XSS escape functions:

### Applying a XSS escaper to `param` or `header`:

```java
{
  post("/", req -> {
    String safeParam = req.param("input", "html").value();

    String safeHeader = req.header("input", "html").value();
  });
}
```

Here `input` is the `param/header` that you want to escape with the `html` escaper.

### Applying multiple XSS escapers:

```java
{
  post("/", req -> {
    String safeInput = req.param("input", "urlFragment", "html");
  });
}
```

### Applying a XSS escaper to form/bean:

```java
{
  post("/", req -> {
    MyForm form = req.params("input", "html");
  });
}
```

### Applying a XSS escaper from template engines

Template engines usually provide a way to escape `HTML` (mainly) ... still {{jooby}} integrates XSS escapers with the template engine of your choice:

[handlebars](/doc/hbs):

    {{xss input "js" "html"}}

[pebble](/doc/pebble):

    {{xss (input, "js", "html")}}

[freemarker](/doc/ftl):

    ${xss (input, "js", "html")}

[jade](/doc/jade):

    p= xss.apply(input, "js", "html")

## xss modules

* [csl](/doc/csl): XSS escapers via [coverity-security-library](https://github.com/coverity/coverity-security-library)
