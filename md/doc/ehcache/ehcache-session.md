# ehcache session store

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-ehcache</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

This module provides an [EhSessionStore]({{defdocs}}/ehcache/EhSessionStore.html). In order to use the [EhSessionStore]({{defdocs}}/ehcache/EhSessionStore.html) all
you have to do is define a ```session``` cache:

```properties

ehcache.cache.session {
  # cache will expire after 30 minutes of inactivity
  timeToIdle = 30m
}
```

And then register the [EhSessionStore]({{defdocs}}/ehcache/EhSessionStore.html):

```java
{
  session(EhSessionStore.class);
}
```
