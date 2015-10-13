---
layout: index
title: assets-sass
version: 0.11.2
---

# sass

<a href="http://sass-lang.com/">sass-lang</a> implementation from <a href="https://github.com/sass/sass">Sass (ruby)</a> Sass is the most mature, stable, and powerful professional grade CSS extension language in the world.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-sass</artifactId>
  <version>0.11.2</version>
  <scope>test</scope>
</dependency>
```

## usage

```
assets {
 fileset {
   home: home.scss
 }
 pipeline {
   dev: [sass]
   dist: [sass]
 }
}
```

## options

```
assets {
 ...
 sass {
   syntax: scss
   dev {
     sourceMap: inline
   }
 }
}
```
