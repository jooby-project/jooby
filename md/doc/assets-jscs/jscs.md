# jscs

<a href="http://jscs.info/">JavaScript Code Style checker</a>.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-jscs</artifactId>
  <version>{{version}}</version>
  <scope>provided</scope>
</dependency>
```

## screenshot

Here is a screenshot of the [live compiler](/doc/assets/#assets-live-compiler-and-error-report) for <a href="http://jscs.info/">JavaScript Code Style checker</a>:

<img alt="live compiler" width="75%" src="http://jooby.org/resources/images/jscs.png">

## usage

```
assets {
 fileset {
   home: ...
 }
 pipeline {
   dev: [jscs]
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
   dev: [jscs]
   ...
 }
 jscs {
   preset: jquery
 }
}
```

# see also

{{available-asset-procesors.md}}
