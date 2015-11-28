# clean-css

A fast, efficient, and well tested CSS minifier, via: <a href="https://github.com/jakubpawlowicz/clean-css">clean-css</a>

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-clean-css</artifactId>
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
   dist: [clean-css]
 }
}
```

## options

```
assets {
 ...
 clean-css {
   advanced: true
   aggressiveMerging: true
 }
}
```

# see also

{{available-asset-procesors.md}}
