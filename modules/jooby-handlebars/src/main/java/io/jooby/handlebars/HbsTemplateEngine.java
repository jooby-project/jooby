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

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HbsTemplateEngine implements TemplateEngine {

  private final List<String> extensions;
  private final Handlebars handlebars;

  HbsTemplateEngine(Handlebars handlebars, List<String> extensions) {
    this.handlebars = handlebars;
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @Nonnull @Override public List<String> extensions() {
    return extensions;
  }

  @Override public String render(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = handlebars.compile(modelAndView.view);
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.model);
    return template.apply(model);
  }
}
