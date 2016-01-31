# jade

[Jade](http://jade-lang.com/) templates for [Jooby](/). Exposes a [renderer](/apidocs/Renderer.html).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-jade</artifactId>
  <version>0.14.0</version>
</dependency>
```

## usage
It is pretty straightforward:

```java
{
  use(new Jade());

  get("/", req -> Results.html("index").put("model", new MyModel());
}
```

public/index.html:

```java
${model}
```

Templates are loaded from root of classpath: ```/``` and must end with: ```.jade``` file extension.

## req locals

A template engine has access to ```request locals``` (a.k.a attributes). Here is an example:

```java
{
  use(new Jade());

  get("*", req -> {
    req.set("req", req);
    req.set("session", req.session());
  });
}
```

By default, there is no access to ```req``` or ```session``` from your template. This example shows how to do it.


## configuration

### application.conf
Just add a ```jade.*``` option to your ```application.conf``` file:

```
jade.prettyprint: true
jade.suffix: .html
```

## template loader
Templates are loaded from the root of classpath and must end with ```.jade```. You can
change the default template location and extensions too:

```java
{
  use(new Jade("/", ".html"));
}
```

## cache

Cache is OFF when ```env=dev``` (useful for template reloading), otherwise is ON and templates do not expire.

