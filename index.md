---
layout: index
title: do more, more easily
version: 0.6.2
---

# &infin; do more, more easily

Introducing [Jooby](http://jooby.org) a minimalist web framework for Java 8 or higher.

* **Solid**. Build on top of mature technologies.

* **Scalable**. Stateless application development.

* **Fast, modular and extensible**. So extensible that even the web server is plugable.

* **Simple, effective and easy to learn**. Ideal for small but also large scale applications.

* **Ready for modern web**. That requires a lot of JavaScript/HTML/CSS

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

## killer features

* **Scripting programming model**. Like [express.js](http://expressjs.com), [Sinatra](http://www.sinatrarb.com), etc.. but also
* **MVC programming model**. Like [Spring](http://spring.io) controllers or [Jersey](https://jersey.java.net) resources
* **Multi-server**. Including [Netty](http://netty.io), [Jetty](http://www.eclipse.org/jetty/) and [Undertow](http://undertow.io)
* **Web-Socket**
* **Dependency Injection**
* **Hot reload** for development


## Want to learn more?

Check out the [quickstart](/quickstart) guide.
