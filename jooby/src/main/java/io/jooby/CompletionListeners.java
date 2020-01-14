package io.jooby;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CompletionListeners {
  private List<Route.Complete> listeners;

  public void addListener(@Nonnull Route.Complete listener) {
    if (listeners == null) {
      listeners = new ArrayList<>();
    }
    listeners.add(listener);
  }

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
