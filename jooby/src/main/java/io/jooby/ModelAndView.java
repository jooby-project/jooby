/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Used by template engines to renderer views.
 *
 * @since 2.0.0
 * @author edgar
 */
public class ModelAndView {

  /** View name. */
  public final String view;

  /** View data. */
  public final Map<String, Object> model;

  /**
   * Creates a new model and view.
   *
   * @param view View name.
   * @param model View model.
   */
  public ModelAndView(@Nonnull String view, @Nonnull Map<String, Object> model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Creates a new model and view.
   *
   * @param view View name.
   */
  public ModelAndView(@Nonnull String view) {
    this(view, new HashMap<>());
  }

  /**
   * Put a model attribute.
   *
   * @param name Name.
   * @param value Value.
   * @return This model and view.
   */
  public ModelAndView put(@Nonnull String name, Object value) {
    model.put(name, value);
    return this;
  }

  /**
   * Copy all the attributes into the model.
   *
   * @param attributes Attributes.
   * @return This model and view.
   */
  public ModelAndView put(@Nonnull Map<String, Object> attributes) {
    model.putAll(attributes);
    return this;
  }

  @Override public String toString() {
    return view;
  }
}
