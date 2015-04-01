# &infin; do more, more easily

Introducing [Jooby](http://jooby.org) a minimalist web framework for Java 8 or higher.

* Simple and effective programming model for building scalable web applications.

* Easy to learn and to get started. Ideal for small but also large scale applications.

* Fast, modular and extensible. It is so extensible that even the web server is plugable.

* Ready for modern web, with a lot of JavaScript/HTML/CSS. It is pretty simple to integrate with [grunt](http://gruntjs.com/), [gulp](http://gulpjs.com/), etc...

## hello world!

```java
import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", () -> "Hey Jooby!");
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}

```

Want to learn more?

Check out the [quickstart](/quickstart) guide.
