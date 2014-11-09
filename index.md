---
layout: index
title: home
---

# jooby

A micro-web framework for Java 8.

```java

import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", (req, rsp) ->
      rsp.send("Hey Jooby!")
    );
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}

```

