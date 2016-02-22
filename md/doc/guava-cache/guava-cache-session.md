# guava session store

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
    Cache cache = req.require("session", Cache.class);
  });
}
```
