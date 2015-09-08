# jooby-sass

[Sass](http://sass-lang.com) CSS pre-processor via [Vaadin Sass Compiler](https://github.com/vaadin/sass-compiler).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-sass</artifactId>
  <version>0.10.0</version>
</dependency>
```

## usage

```java
import org.jooby.sass.Sass;

{
  use(new Sass("/css/**"));
}
```

css/style.scss:

```css
$font-stack:    Helvetica, sans-serif;
$primary-color: #333;

body {
  font: 100% $font-stack;
  color: $primary-color;
}
```

A request like:

```
GET /css/style.css
```

or

```
GET /css/style.scss
```

Produces:

```css
body {
  font: 100% Helvetica, sans-serif;
  color: #333;
}
```

Cool, isn't?
