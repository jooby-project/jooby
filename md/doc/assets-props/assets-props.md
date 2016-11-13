# props

Replace ```${}``` expressions with application properties.

{{assets-require.md}}

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

## example

application.conf:

```
foo = bar
```

app.js:

```js
(function (foo) {
  console.log(foo);
})("${foo}")
```

prints:

    bar

## options

It replaces `${}` expressions, the `delims` options allow you to change this:

```
assets {
 ...
 props {
   delims: [<%, %>]
 }
}
```

# see also

{{available-asset-procesors.md}}
