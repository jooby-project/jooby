/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.jooby.*;
import io.jooby.output.Output;

/**
 * Intercepts {@link HtmxModelAndView} returns and streams multiple templates sequentially to the
 * HTMX client.
 *
 * <p><b>Note:</b> This class is not a standalone template engine (such as Handlebars or
 * Freemarker). Instead, it acts as a composite delegator. When an {@link HtmxModelAndView} is
 * detected, this engine resolves the actual, registered {@link TemplateEngine} capable of handling
 * the views. It then uses that underlying engine to render both the primary view and all attached
 * Out-Of-Band (OOB) views, concatenating their output into a single HTTP response payload.
 *
 * @author edgar
 * @since 4.5.0
 */
public class HtmxTemplateEngine implements TemplateEngine.OnTop {

  private List<TemplateEngine> engines;

  void init(Jooby app) {
    engines = new ArrayList<>(app.getRouter().getTemplateEngines());
    engines.remove(this);
    if (engines.isEmpty()) {
      throw new IllegalStateException("No template engines registered");
    }
  }

  @Override
  public Output render(Context ctx, ModelAndView<?> modelAndView) throws Exception {
    if (modelAndView instanceof HtmxModelAndView<?> htmxView) {
      var engineEncoder = resolveTemplateEngine(htmxView);
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
   * @param mv The {@link ModelAndView} to be rendered. The method determines its compatibility with
   *     the available template engines.
   * @return The {@link TemplateEngine} capable of rendering the provided {@link ModelAndView}, or
   *     {@code null} if no suitable engine is found.
   */
  private @Nullable TemplateEngine resolveTemplateEngine(ModelAndView mv) {
    // Find the encoder that handles standard ModelAndView
    for (var engine : engines) {
      if (engine.supports(mv)) {
        return engine;
      }
    }
    return null;
  }

  @Override
  public boolean supports(ModelAndView modelAndView) {
    return modelAndView instanceof HtmxModelAndView;
  }
}
