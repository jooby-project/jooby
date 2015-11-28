# less4j

<a href="https://github.com/SomMeri/less4j">Less4j</a> is a port of <a href="http://lesscss.org/">Less</a> written in Java. <a href="http://lesscss.org/">Less</a> is a CSS pre-processor, meaning that it extends the CSS language, adding features that allow variables, mixins, functions and many other techniques that allow you to make CSS that is more maintainable, themable and extendable.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-less4j</artifactId>
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
   dev: [less]
   dist: [less]
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
   dev: [less]
   dist: [less]
 }
 less {
   dev {
     sourceMap.linkSourceMap : true
   }
 }
}
```

# see also

{{available-asset-procesors.md}}
