package io.jooby;

import javax.annotation.Nonnull;

public interface TemplateEngine extends Renderer {

  String apply(Context ctx, ModelAndView modelAndView) throws Exception;

  @Override default boolean render(@Nonnull Context ctx, @Nonnull Object result) throws Exception {
    String output = apply(ctx, (ModelAndView) result);
    ctx.sendText(output);
    return true;
  }
}
