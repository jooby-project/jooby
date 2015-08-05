# jooby-sass

{{sass}} CSS pre-processor via {{sassjava}}.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-sass</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
import org.jooby.sass.Sass;

{
  assets("/css/**", new Sass());
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
