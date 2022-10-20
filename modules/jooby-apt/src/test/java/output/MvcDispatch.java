/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package output;

import io.jooby.Jooby;

public class MvcDispatch implements Runnable {
  private Jooby application;

  public MvcDispatch(Jooby application) {
    this.application = application;
  }

  @Override
  public void run() {
    application.get("/", ctx -> "xx");
  }
}
