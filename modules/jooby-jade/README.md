[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jade/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-jade)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-jade.svg)](https://javadoc.io/doc/org.jooby/jooby-jade/1.3.0)
[![jooby-jade website](https://img.shields.io/badge/jooby-jade-brightgreen.svg)](http://jooby.org/doc/jade)
# jade

<a href="https://github.com/neuland/jade4j">jade4j's</a> intention is to be able to process jade templates in Java without the need of a JavaScript environment, while being fully compatible with the original jade syntax.

## exports

* ```JadeConfiguration```
* [ViewEngine](/apidocs/org/jooby/View.Engine.html)

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-jade</artifactId>
 <version>1.3.0</version>
</dependency>
```

## usage

```java
{
  use(new Jade());

  get("/", req -> Results.html("index").put("model", new MyModel());

  // or via API
  get("/jade-api", req -> {
    JadeConfiguration jade = require(JadeConfiguration.class);
    JadeTemplate template = jade.getTemplate("index");
    template.renderTemplate(...);
  });

}
```

Templates are loaded from root of classpath: ```/``` and must ends with: ```.html``` file extension.

## request locals

A template engine has access to ```request locals``` (a.k.a attributes). Here is an example:

```java
{
  use(new Jade());

  get("*", req -> {
    req.set("foo", bar);
  });
}
```


Then from template:

```
#{foo}
```

## template loader

Templates are loaded from the root of classpath and must ends with ```.html```. Using a custom file extension:

```java
{
  use(new Jade(".jade"));
}
```

Default file extension is: `.html`.

## template cache

Cache is OFF when ```application.env = dev``` (useful for template reloading), otherwise is ON and does not expire, unless you explicitly set ```jade.caching```.

## pretty print

Pretty print is on when ```application.env = dev ```, otherwise is off, unless unless you explicitly set ```jade.prettyprint```.
