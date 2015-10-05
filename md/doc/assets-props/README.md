# clean-css

Replace ```${expressions}``` with a value from ```application.conf```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-props</artifactId>
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
   dev: [props]
   dist: [props]
 }
}
```

## options

```
assets {
 ...
 props {
   delims: [<%, %>]
 }
}
```
