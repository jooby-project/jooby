# svg-symbol

SVG ```symbol``` for icons: merge svg files from a folder and generates a ```sprite.svg``` and ```sprite.css``` files.

{{assets-require.md}}

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-assets-svg-symbol</artifactId>
 <version>{{version}}</version>
 <scope>provided</scope>
</dependency>
```

## usage

```
assets {
  fileset {
    sprite: svg-symbol
  }

  svg-symbol {
    input: "images/svg"
  }
}
```

Previous example looks for ```*.svg``` files inside the ```images/svg``` folder and generate a ```sprite.svg``` and ```sprite.css``` files.

You can display the svg icons using id reference:

```xml
<svg>
 <use xlink:href="#approved" />
</svg>
```

This technique is described here: <a href="https://css-tricks.com/svg-symbol-good-choice-icons">SVG `symbol` a Good Choice for Icons</a>

## options

### output

Defines where to write the ```svg``` and ```css``` files. Default value is: ```sprite```.

```
svg-symbol {
  output: "folder/symbols"
}
```

There are two more specific output options: ```svg.output``` and ```css.output``` if any of these options are present the ```output``` option is ignored:

```
svg-symbol {
  css {
    output: "css/sprite.css"
  },

  svg {
    output: "img/sprite.svg"
  }

}
```

### id prefix and suffix

ID is generated from ```svg file names```. These options prepend or append something to the generated id.

```
svg-symbol {
  output: "sprite"

  id {
    prefix: "icon-"
  }

}
```

Generates IDs like: ```icon-approved```, while:

```
svg-symbol {
  output: "sprite"

  id {
    suffix: "-icon"
  }

}
```

Generates IDs like: ```approved-icon```

### css prefix

Prepend a string to a generated css class. Here is the css class for ```approved.svg```:

```java
.approved {
  width: 18px;
  height: 18px;
}
```

If we set a ```svg``` css prefix:

```
{
  svg-symbol: {
    css {
      prefix: "svg"
    }
  }

}
```

The generated css class will be:

```
svg.approved {
  width: 18px;
  height: 18px;
}
```

This option is useful for generating more specific css class selectors.

# see also

{{available-asset-procesors.md}}
