/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jte;

import java.util.HashMap;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.jooby.Context;
import io.jooby.MapModelAndView;
import io.jooby.ModelAndView;

class JteTemplateEngine implements io.jooby.TemplateEngine {
  private final TemplateEngine jte;
  private final List<String> extensions;

  public JteTemplateEngine(TemplateEngine jte) {
    this.jte = jte;
    this.extensions = List.of(".jte", ".kte");
  }

  @NonNull @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public String render(Context ctx, ModelAndView modelAndView) {
    var output = new StringOutput();
    var attributes = ctx.getAttributes();
    if (modelAndView instanceof MapModelAndView mapModelAndView) {
      var mapModel = new HashMap<>();
      mapModel.putAll(attributes);
      mapModel.putAll(mapModelAndView.getModel());
      jte.render(modelAndView.getView(), mapModel, output);
    } else {
      jte.render(modelAndView.getView(), modelAndView.getModel(), output);
    }

    return output.toString();
  }
}
