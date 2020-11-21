package io.jooby.i1905;

import io.jooby.Jooby;

public class B1905 extends Jooby {

  int counter;

  {
    getServices().put(BService1905.class, new BService1905());

    onStarting(() -> counter += 1);

    onStarted(() -> counter += 1);

    get("/b", ctx -> require(BService1905.class).getClass().getSimpleName() + ";" + counter);
  }
}
