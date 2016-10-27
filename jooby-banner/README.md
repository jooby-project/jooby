# banner

Prints out an ASCII art banner on startup using <a href="https://github.com/lalyos/jfiglet">jfiglet</a>.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-banner</artifactId>
 <version>1.0.0.CR8</version>
</dependency>
```

## usage

```java
package com.myapp;
{
  use(new Banner());

}
```

Prints out the value of ```application.name``` which here is ```myapp```. Or you can specify the text to prints out:

```java
package com.myapp;
{
  use(new Banner("my awesome app"));

}
```

## font

You can pick and use the font of your choice via {@link #font(String)} option:

```java
package com.myapp;
{
  use(new Banner("my awesome app").font("slant"));

}
```

Fonts are distributed within the library inside the ```/flf``` classpath folder. A full list of fonts is available <a href="http://patorjk.com/software/taag">here</a>.
