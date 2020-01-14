/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class that group one or more completion listeners and execute them in reverse order.
 *
 * @author edgar
 * @since 2.5.2
 */
public class CompletionListeners {
  private List<Route.Complete> listeners;

  /**
   * Add a listener.
   *
   * @param listener Listener.
   */
  public void addListener(@Nonnull Route.Complete listener) {
    if (listeners == null) {
      listeners = new ArrayList<>();
    }
    listeners.add(listener);
  }

  /**
   * Execute all listeners.
   *
   * @param ctx Listeners.
   */
  public void run(@Nonnull Context ctx) {
    if (listeners != null) {
      Throwable cause = null;
      for (int i = listeners.size() - 1; i >= 0; i--) {
        try {
          listeners.get(i).apply(ctx);
        } catch (Throwable x) {
          if (cause == null) {
            cause = x;
          } else {
            cause.addSuppressed(x);
          }
        }
      }
      if (cause != null) {
        ctx.getRouter().getLog()
            .error("Completion listener(s) resulted in exception(s): {} {}", ctx.getMethod(),
                ctx.getRequestPath(), cause);
        Stream.concat(Stream.of(cause), Stream.of(cause.getSuppressed()))
            .filter(SneakyThrows::isFatal)
            .findFirst()
            .ifPresent(SneakyThrows::propagate);
      }
    }
  }
}
