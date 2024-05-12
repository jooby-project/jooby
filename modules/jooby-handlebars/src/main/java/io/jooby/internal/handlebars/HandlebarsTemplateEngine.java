/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handlebars;

import static com.github.jknack.handlebars.Context.newContext;

import java.util.Collections;
import java.util.List;

import com.github.jknack.handlebars.Handlebars;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;
import io.jooby.buffer.DataBuffer;

public class HandlebarsTemplateEngine implements TemplateEngine {

  private final List<String> extensions;
  private final Handlebars handlebars;

  public HandlebarsTemplateEngine(Handlebars handlebars, List<String> extensions) {
    this.handlebars = handlebars;
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @NonNull @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public DataBuffer render(Context ctx, ModelAndView modelAndView) throws Exception {
    var template = handlebars.compile(modelAndView.getView());
    var engineModel = newContext(modelAndView.getModel()).data(ctx.getAttributes());
    var buffer = ctx.getBufferFactory().allocateBuffer();
    template.apply(engineModel, buffer.asWriter());
    return buffer;
  }
}
