# props

Replace strings in files while bundling them.

{{assets-require.md}}

## usage

```
assets {
  fileset {
    home: ...
  }
  
  pipeline {
    dist: [replace]
  }
  
  replace {
    process.env.NODE_ENV: "\"production\""
    "\"development\"": "\"production\""
  }
}
```

# see also

{{available-asset-procesors.md}}
