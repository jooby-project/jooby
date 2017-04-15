# rollup.js

<a href="http://rollupjs.org/">rollup.js</a> the next-generation ES6 module bundler.

{{assets-require.md}}

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-rollup</artifactId>
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
    dev: [rollup]
    dist: [rollup]
  }

}
```

## options

### generate

```
rollup {
    genereate {
      format: es
    }
  }

```

See <a href="https://github.com/rollup/rollup/wiki/JavaScript-API#bundlegenerate-options-">generate options</a>.

### plugins

#### babel

```
rollup {
    plugins {
      babel {
        presets: [[es2015, {modules: false}]]
      }
    }
  }

```

See <a href="https://babeljs.io/">https://babeljs.io</a> for more options.

#### legacy

Add a ```export default``` line to legacy modules:

```
rollup {
    plugins {
      legacy {
        "/js/lib/react.js": React
      }
    }
  }

```

#### alias

Set an alias to a common (probably long) path.

```
rollup {
    plugins {
      alias {
        "/js/lib/react.js": "react"
      }
    }
  }

```

Instead of:

```js
import React from 'js/lib/react.js';
```

Now, you can import a module like:

```js
import React from 'react';
```

# see also

{{available-asset-procesors.md}}
