---
layout: home
title: home
---


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

{{quickstart.md}}
