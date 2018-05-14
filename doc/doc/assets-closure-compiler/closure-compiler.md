# closure-compiler

<a href="https://developers.google.com/closure/compiler">Closure Compiler</a> is a tool for making JavaScript download and run faster.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-closure-compiler</artifactId>
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
   ...
   dist: [closure-compiler]
 }
}
```

## options

```
assets {
 ...
 closure-compiler {
   level: advanced
 }
}
```

# see also

{{available-asset-procesors.md}}
