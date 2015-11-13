# jshint

<a href="http://jshint.com/">JSHint</a>, helps to detect errors and potential problems in code.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-jshint</artifactId>
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
   dev: [jshint]
   ...
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
   dev: [jshint]
   ...
 }
 jshint {
   undef: true
   devel: true
   ...
 }
}
```
