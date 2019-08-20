/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pebble;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PebbleTemplateEngine implements TemplateEngine {

  private final List<String> extensions;
  private final PebbleEngine engine;

  PebbleTemplateEngine(PebbleEngine.Builder builder, List<String> extensions) {
    this.engine = builder.build();
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @Nonnull
  @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public String render(Context ctx, ModelAndView modelAndView) throws Exception {
    PebbleTemplate template = engine.getTemplate(modelAndView.getView());
    Writer writer = new StringWriter();
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.getModel());
    template.evaluate(writer, model);
    return writer.toString();
  }
}
