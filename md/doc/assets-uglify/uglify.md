# uglify

<a href="https://github.com/mishoo/UglifyJS2">UglifyJs2</a> JavaScript parser / mangler / compressor / beautifier toolkit.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-uglify</artifactId>
  <version>{{version}}</version>
  <scope>provided</scope>
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

# see also

{{available-asset-procesors.md}}
