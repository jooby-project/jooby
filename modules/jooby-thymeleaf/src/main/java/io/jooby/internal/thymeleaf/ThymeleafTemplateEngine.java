/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.thymeleaf;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.MapModelAndView;
import io.jooby.ModelAndView;
import io.jooby.output.Output;

public class ThymeleafTemplateEngine implements io.jooby.TemplateEngine {
  private final TemplateEngine templateEngine;
  private final List<String> extensions;

  public ThymeleafTemplateEngine(TemplateEngine templateEngine, List<String> extensions) {
    this.templateEngine = templateEngine;
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @NonNull @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public @NonNull Output render(io.jooby.Context ctx, ModelAndView<?> modelAndView) {
    if (modelAndView instanceof MapModelAndView mapModelAndView) {
      Map<String, Object> model = new HashMap<>(ctx.getAttributes());
      model.putAll(mapModelAndView.getModel());

      // Locale:
      var locale = modelAndView.getLocale();
      if (locale == null) {
        locale = ctx.locale();
      }
      var buffer = ctx.getOutputFactory().newBufferedOutput();
      var context = new Context(locale, model);
      var templateName = modelAndView.getView();
      if (!templateName.startsWith("/")) {
        templateName = "/" + templateName;
      }
      templateEngine.process(templateName, context, buffer.asWriter());
      return buffer;
    } else {
      throw new ModelAndView.UnsupportedModelAndView(MapModelAndView.class);
    }
  }
}
