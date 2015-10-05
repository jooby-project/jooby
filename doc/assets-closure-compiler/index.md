---
layout: index
title: assets-closure-compiler
version: 0.11.1
---

# closure-compiler

<a href="https://developers.google.com/closure/compiler">Closure Compiler</a> is a tool for making JavaScript download and run faster.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-closure-compiler</artifactId>
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
   ...
   dist: [closure-compiler]
 }
}
```

## options

```
assets {
 ...
 closure-compiler {
   level: advanced
 }
}
```
