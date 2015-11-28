# jshint

<a href="http://jshint.com/">JSHint</a>, helps to detect errors and potential problems in code.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-jshint</artifactId>
  <version>{{version}}</version>
  <scope>test</scope>
</dependency>
```

## screenshot

Here is a screenshot of the [live compiler](/doc/assets/#assets-live-compiler-and-error-report) for <a href="http://jshint.com/">JSHint</a>:

<img alt="live compiler" width="75%" src="http://jooby.org/resources/images/jshint.png">

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

# see also

{{available-asset-procesors.md}}
