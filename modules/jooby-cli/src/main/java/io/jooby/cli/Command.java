/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

public abstract class Command implements Runnable {
  private CommandContext context;

  @Override public void run() {
    try {
      run(context);
    } catch (Throwable x) {
      sneakyThrow0(x);
    }
  }

  public abstract void run(CommandContext context) throws Exception;

  public void setContext(CommandContext context) {
    this.context = context;
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow0(final Throwable x) throws E {
    throw (E) x;
  }
}
