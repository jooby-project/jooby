/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

class FreemarkerTemplateEngine implements TemplateEngine {

  private final Configuration freemarker;

  public FreemarkerTemplateEngine(Configuration freemarker) {
    this.freemarker = freemarker;
  }

  @Override public String render(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = freemarker.getTemplate(modelAndView.view);
    StringWriter writer = new StringWriter();
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.model);
    template.process(model, writer);
    return writer.toString();
  }
}
