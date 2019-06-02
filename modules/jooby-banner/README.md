[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-banner/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-banner/1.6.1)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-banner.svg)](https://javadoc.io/doc/org.jooby/jooby-banner/1.6.1)
[![jooby-banner website](https://img.shields.io/badge/jooby-banner-brightgreen.svg)](http://jooby.org/doc/banner)
# banner

Prints out an ASCII art banner on startup using <a href="https://github.com/lalyos/jfiglet">jfiglet</a>.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-banner</artifactId>
 <version>1.6.1</version>
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
