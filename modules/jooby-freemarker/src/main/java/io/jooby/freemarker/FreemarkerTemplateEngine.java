/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.freemarker;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import freemarker.core.Environment;
import freemarker.template.*;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.SneakyThrows;
import io.jooby.TemplateEngine;

class FreemarkerTemplateEngine implements TemplateEngine {

  private final Configuration freemarker;
  private final List<String> extensions;

  FreemarkerTemplateEngine(Configuration freemarker, List<String> extensions) {
    this.freemarker = freemarker;
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @NonNull @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public String render(Context ctx, ModelAndView modelAndView) throws Exception {
    var template = freemarker.getTemplate(modelAndView.getView());
    var writer = new StringWriter();
    var wrapper = freemarker.getObjectWrapper();
    var model = modelAndView.getModel();
    var engineModel = wrapper.wrap(model);
    var locale = modelAndView.getLocale();
    if (locale == null) {
      locale = ctx.locale();
    }
    Environment env = template.createProcessingEnvironment(engineModel, writer);
    env.setLocale(locale);
    ctx.getAttributes()
        .forEach(
            SneakyThrows.throwingConsumer(
                (name, value) -> env.setVariable(name, wrapper.wrap(value))));
    env.process();
    return writer.toString();
  }
}
