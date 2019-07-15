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
  private final String view;

  /** View data. */
  private final Map<String, Object> model;

  /**
   * Creates a new model and view.
   *
   * @param view View name must include file extension.
   * @param model View model.
   */
  public ModelAndView(@Nonnull String view, @Nonnull Map<String, Object> model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Creates a new model and view.
   *
   * @param view View name  must include file extension.
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

  /**
   * View data (a.k.a as model).
   *
   * @return View data (a.k.a as model).
   */
  public Map<String, Object> getModel() {
    return model;
  }

  /**
   * View name with file extension, like: <code>index.html</code>.
   *
   * @return View name with file extension, like: <code>index.html</code>.
   */
  public String getView() {
    return view;
  }

  @Override public String toString() {
    return view;
  }
}
