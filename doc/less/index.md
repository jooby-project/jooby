---
layout: index
title: less
version: 0.10.0
---

# jooby-less

Transform [Less](http://lesscss.org) files to ```css``` via [Less4j](https://github.com/SomMeri/less4j).

## exposes

* A Less handler
* A Thread-Safe instance of ```LessCompiler```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-less</artifactId>
  <version>0.10.0</version>
</dependency>
```

## usage

```java
import org.jooby.less.Less;

{
  use(new Less("/css/**));
}
```

styles.css:

```css
@font-stack: Helvetica, sans-serif;
@primary-color: #333;

body {
  font: @font-stack;
  color: @primary-color;
}
```

A request like:

```
GET /css/style.css
```

or

```
GET /css/style.less
```

Produces:

```css
body {
  font: Helvetica, sans-serif;
  color: #333;
}
```

## configuration
A ```Configuration``` object can be configured via ```.conf``` file and/or programmatically via ```doWith(Consumer)```.

application.conf:

```properties
less.compressing = true
```

or

```java
{
  use(new Less("/css/**").doWith(conf -> {
    conf.setCompressing(true);
  }));
}
```

Happy coding!!

# appendix: less.conf

```properties
less {
  # turn on/off compression. if no value was set, compression is 'off' for dev.
  # compressing: true/false

  sourceMap {
    encodingCharset: ${application.charset}
    includeSourcesContent: false
    relativizePaths: true
    inline: false
    # If set to false, generated css does not contain link to source map file.
    # when no value was set, this flag is 'on' for dev.
    # linkSourceMap: true on dev, otherwise false
  }
}
```
