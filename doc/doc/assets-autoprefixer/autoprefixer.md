# auto-prefixer

<a href="https://github.com/postcss/postcss">PostCSS</a> plugin to parse CSS and add vendor prefixes to CSS rules using values from <a href="http://caniuse.com">Can I Use</a>. It is recommended by Google and used in Twitter, and Taobao.

{{assets-require.md}}

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-assets-autoprefixer</artifactId>
 <version>{{version}}</version>
 <scope>provided</scope>
</dependency>
```

## usage

```
assets {
  pipeline {
    dev: [auto-prefixer]
    dist: [auto-prefixer]
  }
}
```

Once configured, write your CSS rules without vendor prefixes (in fact, forget about them entirely):

```css
:fullscreen a {
  display: flex

}
```

Output:

```css
:-webkit-full-screen a {
   display: -webkit-box;
   display: flex
}
:-moz-full-screen a {
   display: flex
}
:-ms-fullscreen a {
   display: -ms-flexbox;
   display: flex
}
:fullscreen a {
   display: -webkit-box;
   display: -ms-flexbox;
   display: flex
}
```

## options

```
{
  auto-prefixer {

    browsers: ["> 1%", "IE 7"]
    cascade: true
    add: true
    remove: true
    supports: true
    flexbox: true
    grid: true
    stats: {}
  }

}
```

For complete documentation about available options, please refer to the <a href="https://github.com/postcss/autoprefixer">autoprefixer</a> site.

# see also

{{available-asset-procesors.md}}
