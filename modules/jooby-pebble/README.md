[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-pebble/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-pebble/1.6.3)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-pebble.svg)](https://javadoc.io/doc/org.jooby/jooby-pebble/1.6.3)
[![jooby-pebble website](https://img.shields.io/badge/jooby-pebble-brightgreen.svg)](http://jooby.org/doc/pebble)
# pebble

<a href="http://www.mitchellbosecke.com/pebble">Pebble</a> a lightweight but rock solid Java templating engine.

## exports

* ```PebbleEngine```
* [ViewEngine](/apidocs/org/jooby/View.Engine.html)

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-pebble</artifactId>
 <version>1.6.3</version>
</dependency>
```

## usage

```java
{
  use(new Pebble());

  get("/", req -> Results.html("index").put("model", new MyModel());

  // or Pebble API
  get("/pebble-api", req -> {

    PebbleEngine pebble = require(PebbleEngine.class);
    PebbleTemplate template = pebble.getTemplate("template");
    template.evaluate(...);
  });
}
```

Templates are loaded from root of classpath: ```/``` and must end with: ```.html``` file extension.

## template loader

Templates are loaded from the root of classpath and must end with ```.html```. You can change the default template location and extensions too:

```java
{
  use(new Pebble("templates", ".pebble"));
}
```

## request locals

A template engine has access to ```request locals``` (a.k.a attributes). Here is an example:

```java
{
  use(new Pebble());

  get("*", req -> {
    req.set("foo", bar);
  });
}
```

Then from template:

```
{{foo}}
```

## template cache

Cache is OFF when ```env=dev``` (useful for template reloading), otherwise is ON.

Cache is backed by Guava and the default cache will expire after ```200``` entries.

If ```200``` entries is not enough or you need a more advanced cache setting, just set the ```pebble.cache``` option:

```
pebble.cache = "expireAfterWrite=1h;maximumSize=200"
```

See ```com.google.common.cache.CacheBuilderSpec```.

## tag cache

It works like template cache, except the cache is controlled by the property: ```pebble.tagCache```

## advanced configuration

Advanced configuration if provided by callback:

```java
{
  use(new Pebble().doWith(pebble -> {
    pebble.extension(...);
    pebble.loader(...);
  }));
}
```
