# props

Replace ```${expressions}``` with a value from ```application.conf```.

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

## options

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
