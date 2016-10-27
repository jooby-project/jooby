# svg-sprites

An [AssetAggregator](/apidocs/org/jooby/assets/AssetAggregator.html) that creates SVG sprites with PNG fallbacks at needed sizes via <a href="https://github.com/drdk/dr-svg-sprites">dr-svg-sprites</a>.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-svg-sprites</artifactId>
 <version>1.0.0.CR8</version>
</dependency>
```

## usage

```
assets {
  fileset {

    sprite: svg-sprites
    home: home.scss
  }

  svg-sprites {

    spriteElementPath: "images/svg-source",
    spritePath: "css"
  }

}
```

The ```spriteElementPath``` contains all the ```*.svg``` files you want to process. The ```spritePath``` indicates where to save the sprite, here you will find the following generated files: ```css/sprite.css```, ```css/sprite.svg``` and ```css/sprite.png```.

## options

```
assets {
  fileset {

    sprite: svg-sprites
    home: home.scss
  }

  svg-sprites {

    spriteElementPath: "images/svg-source",
    spritePath: "css",
    layout: "vertical",
    sizes: {
      large: 24,
      small: 16
    },
    refSize: "large"
  }

}
```

Please refer to <a href="https://github.com/drdk/dr-svg-sprites">dr-svg-sprites</a> for more details.
