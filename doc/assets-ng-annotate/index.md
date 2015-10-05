---
layout: index
title: assets-ng-annotate
version: 0.11.1
---

# ng-annotate

<a href="https://github.com/olov/ng-annotate">ng-annotate</a> add, remove and rebuild AngularJS dependency injection annotations.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-ng-annotate</artifactId>
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
   dev: [ng-annotate]
   dist: [ng-annotate]
 }
}
```

## options

```
assets {
 ...
 ng-annotate {
   add: true
   remove: false
 }
}
```
