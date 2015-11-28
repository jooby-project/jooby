# rjs

<a href="http://requirejs.org/docs/optimization.html">require.js optimizer</a> resolve and optimize require.js files.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-requirejs</artifactId>
  <version>{{version}}</version>
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

# see also

{{available-asset-procesors.md}}
