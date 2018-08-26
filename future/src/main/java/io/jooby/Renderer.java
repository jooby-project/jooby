package io.jooby;

import javax.annotation.Nonnull;

public interface Renderer {

  Renderer TO_STRING = (ctx, value) -> ctx.send(value.toString());

  void render(@Nonnull Context ctx, @Nonnull Object result) throws Exception;

  @Nonnull default Renderer then(@Nonnull Renderer next) {
    return (ctx, result) -> {
      render(ctx, result);
      if (!ctx.isResponseStarted()) {
        next.render(ctx, result);
      }
    };
  }

}
