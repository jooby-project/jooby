# csslint

<a href="http://csslint.net/">CSSLint</a> automated linting of Cascading Stylesheets.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-csslint</artifactId>
  <version>0.11.0</version>
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
   dev: [csslint]
   ...
 }
}
```
