# guava-cache

Provides cache solution and session storage via: <a href="https://github.com/google/guava">Guava</a>.

## exports

* ```Cache```

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-guava-cache</artifactId>
 <version>1.0.1</version>
</dependency>
```

## usage

App.java:

```java
import org.jooby.guava.GuavaCache;

{
  use(GuavaCache.newCache());

  get("/", req -> {

    Cache cache = require(Cache.class);
    // do with cache...
  });

}
```

## options 

### cache configuration

A default cache will be created, if you need to control the number of entries, expire policy, etc... set the ```guava.cache``` property in ```application.conf```, like:

```
guava.cache {
  maximumSize = 10

  concurrencyLevel = 2
}
```

or via ```com.google.common.cache.CacheBuilderSpec``` syntax: 

```
guava.cache = "maximumSize=10,concurrencyLevel=2"
```

### multiple caches

Just add entries to: ```guava.```, like:

```
# default cache (don't need a name on require calls)
guava.cache = "maximumSize=10"
guava.cache1 = "maximumSize=1"
guava.cacheX = "maximumSize=100"
```

```java
{
  get("/", req -> {
    Cache defcache = require(Cache.class);
    Cache cache1 = require("cache1", Cache.class);
    Cache cacheX = require("cacheX", Cache.class);
  });
}
```

### type-safe caches

Type safe caches are provided by calling and creating a new ```GuavaCache``` subclass:

```java
{
  // please notes the {} at the line of the next line
  use(new GuavaCache<Integer, String>() {});
}
```

Later, you can inject this cache in a type-safe manner:

```java
@Inject
public MyService(Cache<Integer, String> cache) {
 ...
}
```

# guava session store

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-guava-cache</artifactId>
 <version>1.0.1</version>
</dependency>
```

## usage

This module comes with a ```Session.Store``` implementation. In order to use it you need to define a cache named ```session``` in your ```application.conf``` file:

```
guava.session = "maximumSize=10"
```

And set the ```GuavaSessionStore```:

```java
import org.jooby.guava.GuavaCache;
import org.jooby.guava.GuavaSessionStore;

{
  use(GuavaCache.newCache());

  session(GuavaSessionStore.class);
}
```

You can access to the ```session``` via name:

```java
{
  get("/", req -> {
    Cache cache = require("session", Cache.class);
  });
}
```
