# babel

<a href="http://babeljs.io/">Babel</a> is a JavaScript compiler. By default, Babel ships with a set of ES2015 syntax transformers. These allow you to use new syntax, right now without waiting for browser support.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-babel</artifactId>
  <version>{{version}}</version>
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
  <scope>test</scope>
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
  blacklist: [react]
 }
}
```