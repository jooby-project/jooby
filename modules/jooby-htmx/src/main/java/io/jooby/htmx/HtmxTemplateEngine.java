/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import org.jspecify.annotations.Nullable;

import io.jooby.*;
import io.jooby.output.Output;

/**
 * Intercepts {@link HtmxModelAndView} returns and streams multiple templates sequentially to the
 * HTMX client.
 */
public class HtmxTemplateEngine implements TemplateEngine {

  @Override
  public Output render(Context ctx, ModelAndView<?> modelAndView) throws Exception {
    if (modelAndView instanceof HtmxModelAndView<?> htmxView) {
      var engineEncoder = resolveTemplateEngine(ctx, htmxView);
      if (engineEncoder == null) {
        throw new IllegalStateException(
            "No template engine registered to handle: " + htmxView.getView());
      }
      var composite = ctx.getOutputFactory().newComposite();
      for (ModelAndView<?> mv : htmxView) {
        composite.write(engineEncoder.encode(ctx, mv).asByteBuffer());
      }
      return composite;
    }
    return null;
  }

  /**
   * Resolves a {@link TemplateEngine} instance capable of rendering the specified {@link
   * ModelAndView}. Iterates through the available template engines in the context, returning the
   * first one that supports the provided model and view.
   *
   * @param ctx The web context containing the registered resources and state information.
   * @param mv The {@link ModelAndView} to be rendered. The method determines its compatibility with
   *     the available template engines.
   * @return The {@link TemplateEngine} capable of rendering the provided {@link ModelAndView}, or
   *     {@code null} if no suitable engine is found.
   */
  private @Nullable TemplateEngine resolveTemplateEngine(Context ctx, ModelAndView mv) {
    // Find the encoder that handles standard ModelAndView
    for (var templateEngine : ctx.getRouter().getTemplateEngines()) {
      if (templateEngine != this && templateEngine.supports(mv)) {
        return templateEngine;
      }
    }
    return null;
  }

  @Override
  public boolean supports(ModelAndView modelAndView) {
    return modelAndView instanceof HtmxModelAndView;
  }
}
