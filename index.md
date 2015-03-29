---
layout: index
title: 
version: 0.4.2.1
---

A minimalist web framework for Java 8, inspired by [express.js](http://expressjs.com) and others ;)

## key features

* powerful and easy to use
* fast and modular
* highly flexible and configurable
* high performance NIO web server

```java
import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", () ->
      "Hey Jooby!"
    );
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}

```

Check out the [quickstart guide](/quickstart) and learn how to get starting.
