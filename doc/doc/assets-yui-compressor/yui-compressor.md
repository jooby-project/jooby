# yui-css

<a href="http://yui.github.io/yuicompressor">Yui compressor</a>.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-yui-compressor</artifactId>
  <version>{{version}}</version>
  <scope>provided</scope>
</dependency>
```

## usage

```
assets {
 fileset {
   home: [css/home.css]
 }
 pipeline {
   ...
   dist: [yui-css]
 }
}
```

# yui-js

<a href="http://yui.github.io/yuicompressor">Yui js compressor</a>.

## usage

```
assets {
 fileset {
   home: [js/home.js]
 }
 pipeline {
   ...
   dist: [yui-js]
 }
}
```

## options

```js
assets {
 ...
 yui-js {
   munge: true
   preserve-semi: true
 }
}
```

# see also

{{available-asset-procesors.md}}
