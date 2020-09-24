/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.freemarker;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class FreemarkerTemplateEngine implements TemplateEngine {

  private final Configuration freemarker;
  private final List<String> extensions;

  FreemarkerTemplateEngine(Configuration freemarker, List<String> extensions) {
    this.freemarker = freemarker;
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @Nonnull @Override public List<String> extensions() {
    return extensions;
  }

  @Override public String render(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = freemarker.getTemplate(modelAndView.getView());
    StringWriter writer = new StringWriter();
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.getModel());
    Locale locale = modelAndView.getLocale();
    if (locale == null) {
      locale = ctx.locale();
    }
    Environment env = template.createProcessingEnvironment(model, writer);
    env.setLocale(locale);
    env.process();
    return writer.toString();
  }
}
