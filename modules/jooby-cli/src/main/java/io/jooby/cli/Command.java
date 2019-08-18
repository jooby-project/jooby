/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import io.jooby.SneakyThrows;

public abstract class Command implements SneakyThrows.Runnable {
  private CommandContext context;

  @Override public void tryRun() throws Exception {
    run(context);
  }

  public abstract void run(CommandContext context) throws Exception;

  public void setContext(CommandContext context) {
    this.context = context;
  }
}
