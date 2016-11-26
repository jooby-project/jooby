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

### delims

The `delims` options allow you to change the default delimiters `${ }`:

```
assets {
 ...
 props {
   delims: [<%, %>]
 }
}
```

### ignoreMissing

Unresolved properties results in `NoSuchElement` exception. Setting `ignoreMissing` skip missing properties:

```
assets {
 ...
 props {
   ignoreMissing: true
 }
}
```

# see also

{{available-asset-procesors.md}}
