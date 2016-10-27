# caffeine

Provides cache solution and session storage via: <a href="https://github.com/ben-manes/caffeine">Caffeine</a>.

## exports

* ```Cache```

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-caffeine-cache</artifactId>
 <version>1.0.0.CR8</version>
</dependency>
```

## usage

App.java:

```java
import org.jooby.caffeine.CaffeineCache;

{
  use(CaffeineCache.newCache());

  get("/", req -> {

    Cache cache = req.require(Cache.class);
    // do with cache...
  });

}
```

## options

### cache configuration

A default cache will be created, if you need to control the number of entries, expire policy, etc... set the ```caffeine.cache``` property in ```application.conf```, like:

```
caffeine.cache {
  maximumSize = 10
}
```

or via ```com.github.benmanes.caffeine.cache.CaffeineSpec``` syntax:

```
caffeine.cache = "maximumSize=10"
```

### multiple caches

Just add entries to: ```caffeine.```, like:

```
# default cache (don't need a name on require calls)
caffeine.cache = "maximumSize=10"
caffeine.cache1 = "maximumSize=1"
caffeine.cacheX = "maximumSize=100"
```

```java
{
  get("/", req -> {
    Cache defcache = req.require(Cache.class);
    Cache cache1 = req.require("cache1", Cache.class);
    Cache cacheX = req.require("cacheX", Cache.class);
  });
}
```

### type-safe caches

Type safe caches are provided by calling and creating a new ```CaffeineCache``` subclass:

```java
{
  // please notes the {} at the line of the next line
  use(new CaffeineCache<Integer, String>() {});
}
```

Later, you can inject this cache in a type-safe manner:

```java
@Inject
public MyService(Cache<Integer, String> cache) {
 ...
}
```

# caffeine session store

## usage

This module comes with a ```Session.Store``` implementation. In order to use it you need to define a cache named ```session``` in your ```application.conf``` file:

```
caffeine.session = "maximumSize=10"
```

And set the ```CaffeineSessionStore```:

```java
import org.jooby.caffeine.CaffeineCache;
import org.jooby.caffeine.CaffeineSessionStore;

{
  use(CaffeineCache.newCache());

  session(CaffeineSessionStore.class);
}
```

You can access to the ```session``` via name:

```java
{
  get("/", req -> {
    Cache cache = req.require("session", Cache.class);
  });
}
```
