/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import javax.annotation.Nonnull;

/**
 * Base class for application commands.
 *
 * @since 2.0.5
 */
public abstract class Command implements Runnable {
  private CommandContext context;

  @Override public void run() {
    try {
      run(context);
    } catch (Throwable x) {
      sneakyThrow0(x);
    }
  }

  /**
   * Run a command.
   *
   * @param context Command context.
   * @throws Exception If something goes wrong.
   */
  public abstract void run(@Nonnull CommandContext context) throws Exception;

  /**
   * Set command context.
   *
   * @param context Command context.
   */
  public void setContext(@Nonnull CommandContext context) {
    this.context = context;
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow0(final Throwable x) throws E {
    throw (E) x;
  }
}
