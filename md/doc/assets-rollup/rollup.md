# rollup.js

<a href="http://rollupjs.org/">rollup.js</a> the next-generation ES6 module bundler.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-rollup</artifactId>
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
   ...
   dist: [rollup]
 }
}
```

## options

```
assets {
 fileset {
   home: ...
 }
 pipeline {
   ...
   dist: [rollmap]
 }
 rollup {
   output {
     format: amd
   }
 }
}
```

See: <a href="https://github.com/rollup/rollup/wiki/JavaScript-API">rollup.js options.</a>

# see also

{{available-asset-procesors.md}}
