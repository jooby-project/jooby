/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pebble;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.MapModelAndView;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;
import io.jooby.buffer.DataBuffer;
import io.pebbletemplates.pebble.PebbleEngine;

class PebbleTemplateEngine implements TemplateEngine {

  private final List<String> extensions;
  private final PebbleEngine engine;

  PebbleTemplateEngine(PebbleEngine.Builder builder, List<String> extensions) {
    this.engine = builder.build();
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @NonNull @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public boolean supports(@NonNull ModelAndView modelAndView) {
    return TemplateEngine.super.supports(modelAndView) && modelAndView instanceof MapModelAndView;
  }

  @Override
  public DataBuffer render(Context ctx, ModelAndView modelAndView) throws Exception {
    if (modelAndView instanceof MapModelAndView mapModelAndView) {
      var buffer = ctx.getBufferFactory().allocateBuffer();
      var template = engine.getTemplate(modelAndView.getView());
      Map<String, Object> model = new HashMap<>(ctx.getAttributes());
      model.putAll(mapModelAndView.getModel());
      var locale = modelAndView.getLocale();
      if (locale == null) {
        locale = ctx.locale();
      }
      template.evaluate(buffer.asWriter(), model, locale);
      return buffer;
    } else {
      throw new IllegalArgumentException(
          "Only " + MapModelAndView.class.getName() + " are supported");
    }
  }
}
