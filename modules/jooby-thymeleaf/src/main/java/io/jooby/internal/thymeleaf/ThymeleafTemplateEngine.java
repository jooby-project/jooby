/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.thymeleaf;

import io.jooby.ModelAndView;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ThymeleafTemplateEngine implements io.jooby.TemplateEngine {

  private TemplateEngine templateEngine;
  private List<String> extensions;

  public ThymeleafTemplateEngine(TemplateEngine templateEngine, List<String> extensions) {
    this.templateEngine = templateEngine;
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @Nonnull @Override public List<String> extensions() {
    return extensions;
  }

  @Override public String render(io.jooby.Context ctx, ModelAndView modelAndView) {
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.getModel());

    // Locale:
    Locale locale = (Locale) model.getOrDefault("locale", Locale.getDefault());

    Context context = new Context(locale, model);
    String templateName = modelAndView.getView();
    if (!templateName.startsWith("/")) {
      templateName = "/" + templateName;
    }
    return templateEngine.process(templateName, context);
  }
}
