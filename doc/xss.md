## xss

[Cross-site scripting (XSS)](https://en.wikipedia.org/wiki/Cross-site_scripting) is a type of security vulnerability typically found in web applications. XSS enables attackers to inject client-side scripts into web pages viewed by other users.

{{Jooby}} provides a few XSS escapers and a simple and flexible way to provide custom and/or more featured XSS escapers.

Default XSS escapers are `urlFragment`, `formParam`, `pathSegment` and `html`, all provided by [Guava](https://github.com/google/guava).

More advanced and feature rich escapers like `js`, `css`, `sql` are provided via [modules](/doc/security).

### usage

There are a couple of ways to use XSS escape functions:

#### Applying an XSS escaper to `param` or `header`:

```java
{
  post("/", req -> {
    String safeParam = req.param("input", "html").value();

    String safeHeader = req.header("input", "html").value();
  });
}
```

Here `input` is the `param/header` that you want to escape with the `html` escaper.

#### Applying multiple XSS escapers:

```java
{
  post("/", req -> {
    String safeInput = req.param("input", "urlFragment", "html");
  });
}
```

#### Applying an XSS escaper to form/bean:

```java
{
  post("/", req -> {
    MyForm form = req.params("input", "html");
  });
}
```

#### Applying an XSS escaper from template engines

Template engines usually provide built in methods to escape `HTML`. However, {{jooby}} will also integrate its XSS escapers with the template engine of your choice:

[handlebars](/doc/hbs):

    {{xss input "js" "html"}}

[pebble](/doc/pebble):

    {{xss (input, "js", "html")}}

[freemarker](/doc/ftl):

    ${xss (input, "js", "html")}

[jade](/doc/jade):

    p= xss.apply(input, "js", "html")

### modules

* [unbescape](/doc/unbescape): XSS escapers via [unbescape](https://github.com/unbescape/unbescape)
* [csl](/doc/csl): XSS escapers via [coverity-security-library](https://github.com/coverity/coverity-security-library)
