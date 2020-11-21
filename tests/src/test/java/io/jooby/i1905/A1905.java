package io.jooby.i1905;

import io.jooby.Jooby;

public class A1905 extends Jooby {

  int counter;

  {
    getServices().put(AService1905.class, new AService1905());

    onStarting(() -> counter += 1);

    onStarted(() -> counter += 1);

    get("/a", ctx -> require(AService1905.class).getClass().getSimpleName() + ";" + counter);
  }
}
