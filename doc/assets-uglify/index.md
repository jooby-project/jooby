---
layout: index
title: assets-uglify
version: 0.11.1
---

# uglify

<a href="https://github.com/mishoo/UglifyJS2">UglifyJs2</a> JavaScript parser / mangler / compressor / beautifier toolkit.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-uglify</artifactId>
  <version>0.11.1</version>
  <scope>test</scope>
</dependency>
```

## usage

```
assets {
 fileset {
   home: [js/home.js]
 }
 pipeline {
   ...
   dist: [uglify]
 }
}
```

## options

```
assets {
 ...
 uglify {
   strict: false
   output {
     beautify: true
   }
 }
}
```
