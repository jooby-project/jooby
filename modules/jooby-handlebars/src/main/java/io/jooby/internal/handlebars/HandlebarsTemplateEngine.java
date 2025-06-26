/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handlebars;

import java.util.Collections;
import java.util.List;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.ValueResolver;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;
import io.jooby.output.Output;

public class HandlebarsTemplateEngine implements TemplateEngine {

  private final Handlebars handlebars;
  private final ValueResolver[] resolvers;
  private final List<String> extensions;

  public HandlebarsTemplateEngine(
      Handlebars handlebars, ValueResolver[] resolvers, List<String> extensions) {
    this.handlebars = handlebars;
    this.resolvers = resolvers;
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @NonNull @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public Output render(Context ctx, ModelAndView<?> modelAndView) throws Exception {
    var template = handlebars.compile(modelAndView.getView());
    var engineModel =
        com.github.jknack.handlebars.Context.newBuilder(modelAndView.getModel())
            .resolver(resolvers)
            .build()
            .data(ctx.getAttributes());
    var buffer = ctx.getOutputFactory().newBufferedOutput();
    template.apply(engineModel, buffer.asWriter());
    return buffer;
  }
}
