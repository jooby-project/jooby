/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

class HbsTemplateEngine implements TemplateEngine {

  private Handlebars handlebars;

  public HbsTemplateEngine(Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  @Override public String apply(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = handlebars.compile(modelAndView.view);
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.model);
    return template.apply(model);
  }
}
