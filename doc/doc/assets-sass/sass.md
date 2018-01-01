# sass

<a href="http://sass-lang.com/">sass-lang</a> implementation from <a href="https://github.com/bit3/jsass">Java sass compiler</a>. Sass is the most mature, stable, and powerful professional grade CSS extension language in the world.

<a href="https://github.com/bit3/jsass">Java sass compiler</a> Feature complete java sass compiler using libsass.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-assets-sass</artifactId>
 <version>{{version}}</version>
 <scope>provided</scope>
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

```java
assets {
  ...

  sass {

    syntax: scss
    dev {
      sourcemap: inline
    }
    dist {
      style: compressed
    }
  }

}
```

# see also

{{available-asset-procesors.md}}
