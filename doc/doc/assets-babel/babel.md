# babel

<a href="http://babeljs.io/">Babel</a> is a JavaScript compiler with a set of ES2015 syntax transformers that allow you to use new syntax, right now without waiting for browser support.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-babel</artifactId>
  <version>{{version}}</version>
  <scope>provided</scope>
</dependency>
```

## usage

```
assets {
 fileset {
   home: ...
 }
 pipeline {
   dev: [babel]
   dist: [babel]
 }
}
```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-babel</artifactId>
  <version>{{version}}</version>
  <scope>provided</scope>
</dependency>
```

## options

```
assets {
 fileset {
   home: ...
 }
 pipeline {
   dev: [babel]
   dist: [babel]
 }
 babel {
  dev {
    sourceMaps: inline
  }
  presets: [es2015, react]
 }
}
```

# see also

{{available-asset-procesors.md}}
