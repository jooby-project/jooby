---
layout: index
title: assets-clean-css
version: 0.11.2
---

# clean-css

A fast, efficient, and well tested CSS minifier, via: <a href="https://github.com/jakubpawlowicz/clean-css">clean-css</a>

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-clean-css</artifactId>
  <version>0.11.2</version>
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
   ...
   dist: [clean-css]
 }
}
```

## options

```
assets {
 ...
 clean-css {
   advanced: true
   aggressiveMerging: true
 }
}
```
