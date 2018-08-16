package io.jooby;

import javax.annotation.Nonnull;

public interface Renderer {

  Renderer TO_STRING = (ctx, value) -> ctx.send(value.toString());

  void render(@Nonnull Context ctx, @Nonnull Object value) throws Exception;

  default Route.After toFilter() {
    return (ctx, value) -> {
      if (!ctx.isResponseStarted()) {
        render(ctx, value);
      }
      return value;
    };
  }
}
