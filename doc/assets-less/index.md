---
layout: index
title: assets-less
version: 0.11.1
---

# less

<a href="http://lesscss.org/">Less</a> is a CSS pre-processor, meaning that it extends the CSS language, adding features that allow variables, mixins, functions and many other techniques that allow you to make CSS that is more maintainable, themable and extendable.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-less</artifactId>
  <version>0.11.1</version>
  <scope>test</scope>
</dependency>
```

## usage

```
assets {
 fileset {
   home: ...
 }
 pipeline {
   dev: [less]
   dist: [less]
 }
}
```

## options

```
assets {
 fileset {
   home: ...
 }
 pipeline {
   dev: [less]
   dist: [less]
 }
 less {
   dev {
     sourceMap {
       sourceMapFileInline: true
     }
   }
 }
}
```
