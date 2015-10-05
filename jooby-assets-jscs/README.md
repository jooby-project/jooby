# jscs

<a href="http://jscs.info/">JavaScript Code Style checker</a>.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-jscs</artifactId>
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
