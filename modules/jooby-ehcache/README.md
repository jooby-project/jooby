[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-ehcache/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-ehcache/1.6.6)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-ehcache.svg)](https://javadoc.io/doc/org.jooby/jooby-ehcache/1.6.6)
[![jooby-ehcache website](https://img.shields.io/badge/jooby-ehcache-brightgreen.svg)](http://jooby.org/doc/ehcache)
# ehcache

Provides advanced cache features via [Ehcache](http://ehcache.org)

## exports

* ```CacheManager```
* ```Ehcache```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-ehcache</artifactId>
  <version>1.6.6</version>
</dependency>
```

## usage

```java
{
  use(new Eh());

  get("/", req -> {
    CacheManager cm = require(CacheManager.class);
    // work with cm

    Ehcache ehcache = require(Ehcache.class);
    // work with ehcache
  });
}
```

[Ehcache](http://ehcache.org) can be fully configured from your ```.conf``` file and/or programmatically, but for the
time being there is no support for ```xml```.
 
## caches

Caches are configured via ```.conf``` like almost everything in [Jooby](http://jooby.org).

```properties
ehcache.cache.mycache {
  eternal = true
}
```

Later, we can access to ```mycache``` with:

```java
{
  get("/", req -> {
    Ehcache mycache = require(Ehcache.class);
  });
}
```

## multiple caches

Multiple caches are also possible:

```properties
ehcache.cache.cache1 {
  maxEntriesLocalHeap = 100
  eternal = true
}

ehcache.cache.cache2 {
  maxEntriesLocalHeap = 100
  eternal = true
}
```

Later, we can access to our caches with:

```java
{
  get("/", req -> {
    Ehcache cache1 = require("cache1", Ehcache.class);
    // ..
    Ehcache cache2 = require("cache2", Ehcache.class);
    // ..
  });
}
```

### cache inheritance

Previous examples, show how to configure two or more caches, but it is also possible to inherit
cache configuration using the ```default``` cache:

```properties
ehcache.cache.default {
  maxEntriesLocalHeap = 100
  eternal = true
}

ehcache.cache.cache1 {
  eternal = false
}

ehcache.cache.cache2 {
  maxEntriesLocalHeap = 1000
}
```

Here ```cache1``` and ```cache2``` will inherited their properties from the ```default``` cache.

Please note the ```default``` cache works as a template and isn't a real/usable cache.

# ehcache session store

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-ehcache</artifactId>
  <version>1.6.6</version>
</dependency>
```

## usage

This module provides an [EhSessionStore](/apidocs/org/jooby/ehcache/EhSessionStore.html). In order to use the [EhSessionStore](/apidocs/org/jooby/ehcache/EhSessionStore.html) all
you have to do is define a ```session``` cache:

```properties

ehcache.cache.session {
  # cache will expire after 30 minutes of inactivity
  timeToIdle = 30m
}
```

And then register the [EhSessionStore](/apidocs/org/jooby/ehcache/EhSessionStore.html):

```java
{
  session(EhSessionStore.class);
}
```

## configuration

Configuration is done in one of two ways: 1) via ```.conf```; or 2) ```programmatically:```.

### via .conf file

```properties
ehcache {

  defaultTransactionTimeout = 1m

  dynamicConfig = true

  maxBytesLocalDisk = 1k

  maxBytesLocalHeap = 1k

  maxBytesLocalOffHeap = 1m

  monitor = off

  # just one event listener
  cacheManagerEventListenerFactory {
    class = MyCacheEventListenerFactory
    p1 = "v1"
    p2 = true
  }

  # or multiple event listeners
  cacheManagerEventListenerFactory {
    listener1 {
      class = MyCacheEventListenerFactory1
      p1 = "v1"
      p2 = true
    }
    listener2 {
      class = MyCacheEventListenerFactory2
    }
  }

  diskStore.path = ${application.tmpdir}${file.separator}ehcache

  # etc...
}
```

### programmatically

```java
{
  use(new Eh().doWith(conf -> {
    conf.setDefaultTransactionTimeoutInSeconds(120);
    // etc...
  }));
}
```

## ehcache.conf
These are the default properties for ehcache:

```properties
ehcache {

  # default cache, caches defined in .conf will inherit these properties

  cache.default {

  }

  diskStore.path = ${application.tmpdir}${file.separator}ehcache

}
```
