---
layout: index
title: assets-requirejs
version: 0.11.1
---

# rjs

<a href="http://requirejs.org/docs/optimization.html">require.js optimizer</a> resolve and optimize require.js files.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-requirejs</artifactId>
  <version>0.11.1</version>
  <scope>test</scope>
</dependency>
```

## usage

```
assets {
 fileset {
   home: js/home.js
 }
 pipeline {
   dev: [rjs]
   dist: [rjs]
 }
}
```

NOTE: The fileset have to define the main module (root/main entry point) and require.js will do all the work.

## options

```
assets {
 ...
 rjs {
   optimize: none
   ...
 }
}
```
