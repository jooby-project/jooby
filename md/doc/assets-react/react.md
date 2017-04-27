# react

Write <a href="https://facebook.github.io/react">React</a> applications easily in the JVM.

{{assets-require.md}}

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-react</artifactId>
 <version>{{version}}</version>
 <scope>provided</scope>
</dependency>
```

## usage

Download <a href="https://unpkg.com/react@15/dist/react.js">react.js</a> and <a href="https://unpkg.com/react-dom@15/dist/react-dom.js">react-dom.js</a> into ```public/js/lib``` folder.

Then add the react processor to ```conf/assets.conf```:

```
assets {
  fileset {

    index: index.js
  }

  pipeline {

    dev: [react]
    dist: [react]
  }

}
```

Write some react code ```public/js/index.js```:

```java
  import React from 'react';
  import ReactDOM from 'react-dom';

  const Hello = () => (
    <p>Hello React</p>
  )

  ReactDOM.render(<Hello />, document.getElementById('root'));
```

Choose one of the available <a href="http://jooby.org/doc/parser-and-renderer/#template-engines">template engines</a> add the ```index.js``` to the page:

```java
<!doctype html>
<html lang="en">
<body>
  <div id="root"></div>
  {{ index_scripts | raw}}
</body>
</html>
```

The ```{{ index_scripts | raw}}``` here is <a href="jooby.org/doc/pebble">pebble expression</a>. Open an browser and try it.

## how it works?

This module give you a ready to use react environment with: ```ES6``` and ```JSX``` support via <a href="http://babeljs.io">babel.js</a> and <a href="https://github.com/rollup/rollup">rollup.js</a>.

You don't need to install ```node.js```, ```npm```, ... nothing, <a href="http://babeljs.io">babel.js</a> and <a href="https://github.com/rollup/rollup">rollup.js</a> run on top of <a href="https://github.com/eclipsesource/J2V8">j2v8</a> as part of the JVM process.

## options

### react-router

Just drop the <a href="https://unpkg.com/react-router-dom/umd/react-router-dom.js">react-router-dom.js</a> into the ```public/js/lib``` folder and use it.

### rollup

It supports all the option of <a href="http://jooby.org/doc/assets-rollup/">rollup.js</a> processor.

# see also

{{available-asset-procesors.md}}
