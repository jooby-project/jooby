package io.jooby;

import javax.annotation.Nonnull;

public interface Renderer extends Route.After {

  @Nonnull @Override default Object apply(@Nonnull Context ctx, @Nonnull Object value)
      throws Exception {
    if (!ctx.isResponseStarted()) {
      render(ctx, value);
    }
    return value;
  }

  void render(@Nonnull Context ctx, @Nonnull Object value) throws Exception;

  Renderer TO_STRING = (ctx, value) -> ctx.send(value.toString());
}
