/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class MapModelAndView extends ModelAndView<Map<String, Object>> {
  /**
   * Creates a new model and view.
   *
   * @param view View name must include file extension.
   * @param model View model.
   */
  public MapModelAndView(@NonNull String view, @NonNull Map<String, Object> model) {
    super(view, model);
  }

  /**
   * Creates a new model and view.
   *
   * @param view View name must include file extension.
   */
  public MapModelAndView(@NonNull String view) {
    super(view, new LinkedHashMap<>());
  }

  /**
   * Put a model attribute.
   *
   * @param name Name.
   * @param value Value.
   * @return This model and view.
   */
  public MapModelAndView put(@NonNull String name, Object value) {
    model.put(name, value);
    return this;
  }

  /**
   * Copy all the attributes into the model.
   *
   * @param attributes Attributes.
   * @return This model and view.
   */
  public MapModelAndView put(@NonNull Map<String, Object> attributes) {
    this.model.putAll(attributes);
    return this;
  }

  @Override
  public MapModelAndView setLocale(@Nullable Locale locale) {
    super.setLocale(locale);
    return this;
  }
}
