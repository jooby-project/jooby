[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.jooby/jooby/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.jooby/jooby)
[![javadoc](https://javadoc.io/badge/io.jooby/jooby.svg)](https://javadoc.io/doc/io.jooby/jooby/latest)
[![Become a Patreon](https://img.shields.io/badge/patreon-donate-orange.svg)](https://patreon.com/edgarespina)
[![Travis](https://travis-ci.org/jooby-project/jooby.svg?branch=master)](https://travis-ci.org/jooby-project/jooby)
[![Github](https://github.com/jooby-project/jooby/workflows/Build/badge.svg)](https://github.com/jooby-project/jooby/actions)
[![Join the chat at https://gitter.im/jooby-project/jooby](https://badges.gitter.im/jooby-project/jooby.svg)](https://gitter.im/jooby-project/jooby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# &infin; do more, more easily

[Jooby](https://jooby.io) is a modern, performant and easy to use web framework for Java and Kotlin built on top of your
favorite web server.

Java:

```java
import static org.jooby.Jooby.runApp;

public class App {

  public static void main(final String[] args) {
    runApp(args, app -> {
      app.get("/", ctx -> "Welcome to Jooby!");
    });
  }
}

```

Kotlin:

```kotlin
import org.jooby.runApp

fun main(args: Array<String>) {
  runApp(args) {
    get ("/") {
      "Welcome to Jooby!"
    }
  }
}

```

documentation
=====

Documentation is available at [https://jooby.io](https://jooby.io)

help
=====

[Gitter](https://gitter.im/jooby-project/jooby)

support
=====

[support@jooby.io](mailto:support@jooby.io?Subject=Jooby%20Support)


1.x version
=====

Documentation for 1.x is available at [https://jooby.io/v1](https://jooby.io/v1)

Source code for 1.x is available at the [1.x branch](https://github.com/jooby-project/jooby/tree/1.x)

author
=====

 [Edgar Espina](https://twitter.com/edgarespina)

license
=====

[Apache License 2](http://www.apache.org/licenses/LICENSE-2.0.html)
