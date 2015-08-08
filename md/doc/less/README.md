# jooby-less

Transform {{less}} files to ```css``` via {{less4j}}.

## exposes

* A Less handler
* A Thread-Safe instance of ```LessCompiler```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-less</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
import org.jooby.less.Less;

{
  use(new Less("/css/**));
}
```

styles.css:

```css
@font-stack: Helvetica, sans-serif;
@primary-color: #333;

body {
  font: @font-stack;
  color: @primary-color;
}
```

A request like:

```
GET /css/style.css
```

or

```
GET /css/style.less
```

Produces:

```css
body {
  font: Helvetica, sans-serif;
  color: #333;
}
```

## configuration
A ```Configuration``` object can be configured via ```.conf``` file and/or programmatically via ```doWith(Consumer)```.

application.conf:

```properties
less.compressing = true
```

or

```java
{
  use(new Less("/css/**").doWith(conf -> {
    conf.setCompressing(true);
  }));
}
```

Happy coding!!

{{appendix}}
